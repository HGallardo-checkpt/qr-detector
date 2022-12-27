package com.checkpoint.qrdetector

import android.graphics.Point
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.SurfaceHolder
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.checkpoint.qrdetector.detector.QRCodeReader
import com.checkpoint.qrdetector.detector.ReaderCodeListener
import com.checkpoint.qrdetector.events.OnDetectionProcessEvent
import com.checkpoint.qrdetector.events.OnPostProccessingCompletedEvent
import com.checkpoint.qrdetector.events.OnPostProccessingNotDetectedEvent
import com.checkpoint.qrdetector.handlers.BitmapHandler
import com.checkpoint.qrdetector.handlers.DirectionDetectorHandler
import com.checkpoint.qrdetector.handlers.InferenceHandler
import com.checkpoint.qrdetector.model.DirectionDetection
import com.checkpoint.qrdetector.utils.NV21toBitmap
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera
import com.serenegiant.widget.AspectRatioSurfaceView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {
    private val DEFAULT_WIDTH = 640

    private val DEFAULT_HEIGHT = 480

    private var mCameraHelper: ICameraHelper? = null

    private var mCameraViewMain: AspectRatioSurfaceView? = null

    private var nV21toBitmap: NV21toBitmap? = null

    private var reader: QRCodeReader? = null

    private var listener: ReaderCodeListener? = null

    private var mDirectorDetectorHandler: Handler? = null

    private var mDirectionDetectorThread: HandlerThread? = null

    private var mBitmapHandler: Handler? = null

    private var mBitmapHandlerThread: HandlerThread? = null

    private var mInferenceHandler: InferenceHandler? = null
    private var mInferenceHandlerThread: HandlerThread? = null


    private val DIRECTION_DETECTOR_THREAD_NAME = String.format(
        "%s%s",
        MainActivity::class.java.getSimpleName(), "DetectorThread"
    )

    private val BITMAP_THREAD_NAME = String.format(
        "%s%s",
        BitmapHandler::class.java.simpleName, "BitmapThread"
    )

    private val INFERENCE_THREAD_NAME = String.format(
        "%s%s",
        MainActivity::class.java.getSimpleName(), "InferenceThread"
    )

    private val BITMAP_HANDLER_LOCK = Any()

    private val DIRECTION_DETECTOR_HANDLER_LOCK = Any()

    private val INFERENCE_HANDLER_LOCK = Any()


    private val directionDetectionList: ArrayList<DirectionDetection> = arrayListOf()

    private var detectionCounter = 0

    private  var imageView: ImageView? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        nV21toBitmap = NV21toBitmap(this)
        mCameraViewMain = findViewById(R.id.svCameraViewMain)
        imageView= findViewById(R.id.imageView)
        mCameraViewMain!!.setAspectRatio(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        mCameraViewMain!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mCameraHelper?.addSurface(holder.surface, false)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mCameraHelper?.removeSurface(holder.surface)
            }
        })
        if (mCameraHelper == null) {
            mCameraHelper = CameraHelper()
            mCameraHelper!!.setStateCallback(mStateListener)
            reader = QRCodeReader()
        }

    }

    override fun onStart() {
        super.onStart()
        initCameraHelper()
        startBitmapHandlerThread()
        startInferenceThread()
        configureDetector()
        startDirectionDetectorThread()
    }

    override fun onStop() {
        super.onStop()
        clearCameraHelper()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        stopBitmapHandlerThread()
        stopInferenceThread()
        stopDirectionDetectorThread()
    }

    private fun initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = CameraHelper()
            mCameraHelper!!.setStateCallback(mStateListener)
        }
    }

    private fun clearCameraHelper() {
        if (mCameraHelper != null) {
            mCameraHelper!!.release()
            mCameraHelper = null
        }

    }

    private fun selectDevice(device: UsbDevice) {
        mCameraHelper!!.selectDevice(device)
    }

    private val mStateListener: ICameraHelper.StateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            selectDevice(device)
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {

            mCameraHelper!!.openCamera()
        }

        override fun onCameraOpen(device: UsbDevice) {
            mCameraHelper!!.startPreview()
            val size: Size? = mCameraHelper!!.previewSize
            if (size != null) {
                val width: Int = size.width
                val height: Int = size.height
                mCameraViewMain!!.setAspectRatio(width, height)
            }
            mCameraHelper!!.addSurface(mCameraViewMain!!.holder.surface, false)
            mCameraHelper!!.setFrameCallback(
                {

                    synchronized (BITMAP_HANDLER_LOCK) {
                        if (mBitmapHandler != null &&
                            !mBitmapHandler!!.hasMessages(BitmapHandler.Message.SET_BITMAP.ordinal)) {
                            val nv21 = ByteArray(it.remaining())
                            it.get(nv21,0,nv21.size)

                            val bitmap  = nV21toBitmap!!.nv21ToBitmap(nv21,size!!.width,size!!.height)
                            mBitmapHandler!!.obtainMessage(BitmapHandler.Message.SET_BITMAP.ordinal, bitmap)
                                .sendToTarget()
                        }
                    }
                }
                , UVCCamera.PIXEL_FORMAT_NV21)
        }

        override fun onCameraClose(device: UsbDevice) {
            if (mCameraHelper != null) {
                mCameraHelper!!.removeSurface(mCameraViewMain!!.holder.surface)
            }
        }

        override fun onDeviceClose(device: UsbDevice) {
            Log.v("", "onDeviceClose:")
        }

        override fun onDetach(device: UsbDevice) {
            Log.v("","onDetach:")
        }

        override fun onCancel(device: UsbDevice) {
            Log.v("","onCancel:")
        }
    }

    /*
    * Prepare bitmap for detection processs sending it to the inference thread
    * */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onEvent(onDetectionProcessEvent: OnDetectionProcessEvent) {
        val bitmap = onDetectionProcessEvent.getResult()
        if (!mInferenceHandler!!.hasMessages(InferenceHandler.Message.RUN_INFERENCE.ordinal)) {
            mInferenceHandler!!.obtainMessage(InferenceHandler.Message.RUN_INFERENCE.ordinal,bitmap).sendToTarget();
        }

    }


    /*
    * Receiving post processing inference result when code hase detected
    * this convert objects result from detection list to position list
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(onPostProcessingCompletedEvent: OnPostProccessingCompletedEvent) {
        val detectionResult = onPostProcessingCompletedEvent.getResult()

        Log.e("QR: ", "" + detectionResult!!.second!![0].rawValue)
        val directionDetection = DirectionDetection()
        directionDetection.translate = detectionResult.second!![0].rawValue
        directionDetection.center = Point(
            detectionResult.first!!.width / 2,
            detectionResult.first!!.height / 2
        )
        directionDetection.position = Point(
            detectionResult.second!![0].boundingBox!!.left,
            detectionResult.second!![0].boundingBox!!.top
        )
        directionDetectionList!!.add(directionDetection)

        detectionCounter++
    }

    /*
    * When treads report not detection validate the content on list if is not empty
    * the content is prepare for postion calculation, after send data to DirectionDetectorHandler
    * the list need to be clear
    */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(onPostProcessingNotDetectedEvent: OnPostProccessingNotDetectedEvent) {
        onPostProcessingNotDetectedEvent.getResult()
         if (onPostProcessingNotDetectedEvent.getResult()) {
             if (detectionCounter >= 2) {
                 synchronized(DIRECTION_DETECTOR_HANDLER_LOCK) {
                     if (!mDirectorDetectorHandler!!.hasMessages(DirectionDetectorHandler.Message.CALCULATE_DIRECTION.ordinal)) {
                          mDirectorDetectorHandler!!.obtainMessage(
                             DirectionDetectorHandler.Message.CALCULATE_DIRECTION.ordinal,
                              directionDetectionList
                         ).sendToTarget()
                         detectionCounter = 0
                         directionDetectionList.clear()
                     }
                 }
             }
         }
    }

    /*
     * Thread declaration for each process required in detection translation qr code task
    */

    private fun startBitmapHandlerThread() {
        mBitmapHandlerThread = HandlerThread(BITMAP_THREAD_NAME)
        mBitmapHandlerThread!!.start()
        mBitmapHandler = BitmapHandler(mBitmapHandlerThread!!.looper)
    }

    private fun stopBitmapHandlerThread() {
        synchronized(BITMAP_HANDLER_LOCK) {
            mBitmapHandler = null
            if (mBitmapHandlerThread != null) {
                mBitmapHandlerThread!!.quitSafely()
                mBitmapHandlerThread = null
            }
        }
    }

    private fun startInferenceThread() {
        mInferenceHandlerThread = HandlerThread(INFERENCE_THREAD_NAME)
        mInferenceHandlerThread!!.start()
        mInferenceHandler = InferenceHandler(mInferenceHandlerThread!!.looper)
    }

    private fun stopInferenceThread() {
        synchronized(INFERENCE_HANDLER_LOCK) {
            mInferenceHandler = null
            if (mInferenceHandlerThread != null) {
                mInferenceHandlerThread!!.quitSafely()
                mInferenceHandlerThread = null
            }
        }
    }

    private fun configureDetector() {
        if (!mInferenceHandler!!.hasMessages(InferenceHandler.Message.INIT_DETECTOR.ordinal)) {
            mInferenceHandler!!.obtainMessage(InferenceHandler.Message.INIT_DETECTOR.ordinal)
                .sendToTarget()
        }
    }

    private fun startDirectionDetectorThread() {
        mDirectionDetectorThread = HandlerThread(DIRECTION_DETECTOR_THREAD_NAME)
        mDirectionDetectorThread!!.start()
        mDirectorDetectorHandler = DirectionDetectorHandler(mDirectionDetectorThread!!.looper)
    }

    private fun stopDirectionDetectorThread() {
        synchronized(DIRECTION_DETECTOR_HANDLER_LOCK) {
            mDirectorDetectorHandler = null
            if (mDirectionDetectorThread != null) {
                mDirectionDetectorThread!!.quitSafely()
                mDirectionDetectorThread = null
            }
        }
    }
}
