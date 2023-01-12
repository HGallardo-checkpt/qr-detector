package com.checkpoint.qrdetector

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Icon
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.checkpoint.qrdetector.api.APIInterfaces
import com.checkpoint.qrdetector.api.QRDetectionApiRest
import com.checkpoint.qrdetector.api.model.DetectionEventRequest
import com.checkpoint.qrdetector.cache.BitmapLruCache
import com.checkpoint.qrdetector.databinding.ActivityCameraBinding
import com.checkpoint.qrdetector.detector.QRCodeReader
import com.checkpoint.qrdetector.events.OnDetectionProcessEvent
import com.checkpoint.qrdetector.events.OnFinishDetectionEvent
import com.checkpoint.qrdetector.events.OnPostProccessingCompletedEvent
import com.checkpoint.qrdetector.events.OnPostProccessingNotDetectedEvent
import com.checkpoint.qrdetector.handlers.BitmapHandler
import com.checkpoint.qrdetector.handlers.DirectionDetectorHandler
import com.checkpoint.qrdetector.handlers.InferenceHandler
import com.checkpoint.qrdetector.model.DirectionDetection
import com.checkpoint.qrdetector.notification.NotificationBuilder
import com.checkpoint.qrdetector.ui.AdminConsoleFragment
import com.checkpoint.qrdetector.ui.EventDetailFragment
import com.checkpoint.qrdetector.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


@SuppressLint("StaticFieldLeak")
private lateinit var analysisExecutor: Executor

private var nV21toBitmap: NV21toBitmap? = null
private var reader: QRCodeReader? = null
private var mDirectorDetectorHandler: Handler? = null
private var mDirectionDetectorThread: HandlerThread? = null
private var mBitmapHandler: Handler? = null
private var mBitmapHandlerThread: HandlerThread? = null
private var mInferenceHandler: InferenceHandler? = null
private var mInferenceHandlerThread: HandlerThread? = null

private val DIRECTION_DETECTOR_THREAD_NAME = String.format(
    "%s%s",
    MainActivity::class.java.simpleName, "DetectorThread"
)
private val BITMAP_THREAD_NAME = String.format(
    "%s%s",
    BitmapHandler::class.java.simpleName, "BitmapThread"
)
private val INFERENCE_THREAD_NAME = String.format(
    "%s%s",
    MainActivity::class.java.simpleName, "InferenceThread"
)

private val BITMAP_HANDLER_LOCK = Any()
private val DIRECTION_DETECTOR_HANDLER_LOCK = Any()
private val INFERENCE_HANDLER_LOCK = Any()
private val directionDetectionList: ArrayList<DirectionDetection> = arrayListOf()
private var detectionCounter = 0




class CameraActivity : AppCompatActivity() {
    private  var txtTranslation: TextView ?= null
    private  var txtDirection: TextView ?= null
    private lateinit var binding: ActivityCameraBinding
    private var notificationManager: NotificationManager? = null
    private var cache: BitmapLruCache? = null
    private var cacheFile:   CacheFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        txtTranslation = binding.textTranslationCode
        txtDirection = binding.textDirectionDetected

        cacheFile = CacheFile(this)
        EventBus.getDefault().register(this)
        nV21toBitmap = NV21toBitmap(this)
        reader = QRCodeReader()

        notificationManager =
            getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager

        cache = BitmapLruCache(BitmapLruCache.cacheSize)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onStart() {
        super.onStart()
        startBitmapHandlerThread()
        startInferenceThread()
        configureDetector()
        startDirectionDetectorThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        stopBitmapHandlerThread()
        stopInferenceThread()
        stopDirectionDetectorThread()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        var date =intent!!.extras!!.getString("date")
        var translate =intent!!.extras!!.getString("translation")
        var direction =intent!!.extras!!.getString("direction")
        var image =intent!!.extras!!.getString("idImage")
        var fragment = EventDetailFragment.newInstance(date!!,translate!!,direction!!,image!!)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment, "FRAGMENT")
            .disallowAddToBackStack()
            .commit()


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        analysisExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor, QRCodeAnalyzer())
                }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("NewApi")
    private fun notifyEvent(date: String, translation: String, direction: String, image: Bitmap){
        val resultIntent = Intent(this, CameraActivity::class.java)
        val idImage = UUID.randomUUID().toString()
        cacheFile!!.saveImgToCache(image,"${idImage}")

        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        resultIntent.putExtra("date",date)
        resultIntent.putExtra("translation",translation)
        resultIntent.putExtra("direction",direction)
        resultIntent.putExtra("idImage",idImage)

        val icon: Icon = Icon.createWithResource(this, android.R.drawable.ic_dialog_alert)
        Thread.sleep(10000)
        NotificationBuilder(notificationManager!!).getNotification(this,resultIntent,icon)
    }

    ///////////////////////////////////////////////////
    /*
  * Prepare bitmap for detection processes sending it to the inference thread
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

        val directionDetection = DirectionDetection()
        directionDetection.translate = detectionResult!!.second!![0].rawValue
        directionDetection.center = Point(
            detectionResult.first!!.width / 2,
            detectionResult.first!!.height / 2
        )
        directionDetection.position = Point(
            detectionResult.second!![0].boundingBox!!.left,
            detectionResult.second!![0].boundingBox!!.top
        )
        directionDetection.image =detectionResult.first!!
        directionDetectionList.add(directionDetection)
        detectionCounter++
    }

    /*
    * When treads report not detection validate the content on list if is not empty
    * the content is prepare for position calculation, after send data to DirectionDetectorHandler
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
                            directionDetectionList.clone()
                        ).sendToTarget()
                        detectionCounter = 0
                        directionDetectionList.clear()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(onFinishDetectionEvent: OnFinishDetectionEvent) {

        val direction = onFinishDetectionEvent.getResultDirection()
        val translation = onFinishDetectionEvent.getResultTranslation()
        val image = onFinishDetectionEvent.getImageBase64()
        val now: OffsetDateTime = OffsetDateTime.now()
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
        val date = formatter.format(now)

        notifyEvent(date,translation!!,direction!!,image!!)


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



    private class QRCodeAnalyzer() : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {

            val bitmap = RotateBitmap().rotate(
                ImageToBitmap().toBitmap(image.planes,image.width,image.height),
                90f)
            synchronized (BITMAP_HANDLER_LOCK) {
                if (mBitmapHandler != null &&
                    !mBitmapHandler!!.hasMessages(BitmapHandler.Message.SET_BITMAP.ordinal)) {
                    mBitmapHandler!!.obtainMessage(BitmapHandler.Message.SET_BITMAP.ordinal, bitmap)
                        .sendToTarget()
                }
            }

            image.close()
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisos no garantizados.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    companion object {
        internal const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}