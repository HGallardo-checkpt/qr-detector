package com.checkpoint.qrdetector

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.TextureView
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.checkpoint.qrdetector.api.QRDetectionApiRest
import com.checkpoint.qrdetector.api.model.DetectionEventRequest
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
import com.checkpoint.qrdetector.ui.EventDetailFragment
import com.checkpoint.qrdetector.utils.CacheFile
import com.checkpoint.qrdetector.utils.Encode
import com.checkpoint.qrdetector.utils.NV21toBitmap
import com.checkpoint.qrdetector.utils.Resize
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class VLCActivity : AppCompatActivity(), TextureView.SurfaceTextureListener{

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: TextureView? = null
    private val url ="rtsp://192.168.100.159:554/onvif1"// "rtsp://admin1:adminroot@10.203.222.176/stream1"
    private lateinit var cacheFile: CacheFile
    private var nV21toBitmap: NV21toBitmap? = null
    private var reader: QRCodeReader? = null
    private var mDirectorDetectorHandler: Handler? = null
    private var mDirectionDetectorThread: HandlerThread? = null
    private var mBitmapHandler: Handler? = null
    private var mBitmapHandlerThread: HandlerThread? = null
    private var mInferenceHandler: InferenceHandler? = null
    private var mInferenceHandlerThread: HandlerThread? = null
    private var notificationManager: NotificationManager? = null
    private val DIRECTION_DETECTOR_THREAD_NAME = String.format(
        "%s%s",
        VLCActivity::class.java.simpleName, "DetectorThread"
    )
    private val BITMAP_THREAD_NAME = String.format(
        "%s%s",
        BitmapHandler::class.java.simpleName, "BitmapThread"
    )
    private val INFERENCE_THREAD_NAME = String.format(
        "%s%s",
        VLCActivity::class.java.simpleName, "InferenceThread"
    )

    private val BITMAP_HANDLER_LOCK = Any()
    private val DIRECTION_DETECTOR_HANDLER_LOCK = Any()
    private val INFERENCE_HANDLER_LOCK = Any()
    private val directionDetectionList: ArrayList<DirectionDetection> = arrayListOf()
    private var detectionCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vlcactivity)


        val btnBack:        FloatingActionButton = findViewById(R.id.btnBack)
        val btnAnimation:   FloatingActionButton = findViewById(R.id.btnAnimation)
        val rlButton:       RelativeLayout = findViewById(R.id.rlButton)
        val rlAnimation:    RelativeLayout= findViewById(R.id.rlAnimation)
        val rlButtonAnimation: RelativeLayout= findViewById(R.id.rlButtonAnimation)

        btnBack.setOnClickListener {
            rlButton.isVisible=false
            rlAnimation.isVisible=false

            rlButtonAnimation.isVisible=true
        }

        btnAnimation.setOnClickListener {
            rlButton.isVisible=true
            rlAnimation.isVisible=true
            rlButtonAnimation.isVisible=false
        }
         videoLayout = findViewById(R.id.videoLayout)
        videoLayout!!.surfaceTextureListener = this
        cacheFile = CacheFile(this)

        libVlc =  LibVLC(this)

        mediaPlayer =  MediaPlayer(libVlc)
        mediaPlayer!!.scale = 1.9f

        EventBus.getDefault().register(this)
        nV21toBitmap = NV21toBitmap(this)
        reader = QRCodeReader()

        notificationManager =
            getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
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

    fun onPrepared(videoWidth: Int,videoHeight: Int) {

        val displayMetrics = DisplayMetrics()
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val scaleY = 1.0f
        val scaleX = (videoWidth * screenHeight / videoHeight / screenWidth).toFloat()
        videoLayout!!.scaleX = scaleX
        videoLayout!!.scaleY = scaleY
        videoLayout!!.pivotX = ((screenWidth / 2).toFloat())
        videoLayout!!.pivotY = ((screenHeight / 2).toFloat())
    }


    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        Log.e("---->","onSurfaceTextureAvailable")

        val  vlcVout: IVLCVout = mediaPlayer!!.vlcVout
        vlcVout.detachViews()
        vlcVout.setVideoView(videoLayout)
        vlcVout.setWindowSize(videoLayout!!.width,videoLayout!!.height)
        vlcVout.attachViews()
        videoLayout!!.keepScreenOn
        try {
            val media = Media(libVlc, Uri.parse(url))
            media.setHWDecoderEnabled(true,false);
            media.addOption(":network-caching=100")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")
            media.addOption(":fullscreen")
            mediaPlayer!!.media = media
            mediaPlayer!!.play()

            GlobalScope.launch {
                val dispatcher = this.coroutineContext
                CoroutineScope(dispatcher).launch {

                    while(true){
                        var bitmap = videoLayout!!.bitmap!!


                             if (mBitmapHandler != null &&
                                 !mBitmapHandler!!.hasMessages(BitmapHandler.Message.SET_BITMAP.ordinal)) {

                                 mBitmapHandler!!.obtainMessage(BitmapHandler.Message.SET_BITMAP.ordinal, bitmap)
                                    .sendToTarget()

                             }

                        Thread.sleep(50)


                    }
                }
            }
        } catch (e: Exception) {
        }
    }


    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
        Log.e("---->","onSurfaceTextureSizeChanged")
     }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        Log.e("---->","onSurfaceTextureDestroyed")

        return true
     }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
        Log.e("---->","onSurfaceTextureUpdated")

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

    @SuppressLint("NewApi")
    private fun notifyEvent(date: String, translation: String, direction: String, image: Bitmap){
        val resultIntent = Intent(this, VLCActivity::class.java)
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
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
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

        directionDetection.image =  Resize(detectionResult.first!!).scale(200,150)!!
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

        /*
        val request = DetectionEventRequest(
            "data:image/jpeg;base64,"+image!!,
            translation!!,
            direction!!,
            date
        )

         QRDetectionApiRest("http://18.191.22.171:5000").postEvent(request)

        */
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