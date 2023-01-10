package com.checkpoint.qrdetector

import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.checkpoint.qrdetector.utils.CacheFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout


class VLCActivity : AppCompatActivity(), TextureView.SurfaceTextureListener{

    private var libVlc: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: TextureView? = null
    private val url = "rtsp://admin1:adminroot@10.203.220.11/stream1"
    private lateinit var cacheFile: CacheFile
    private var mHeight = 0
    private var mWidth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vlcactivity)
        libVlc =  LibVLC(this)
        mediaPlayer =  MediaPlayer(libVlc)
        videoLayout = findViewById(R.id.videoLayout)
        videoLayout!!.surfaceTextureListener = this
        cacheFile = CacheFile(this)
        mediaPlayer!!.scale = 0f
     }

    fun onPrepared(videoWidth: Int,videoHeight: Int) {

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
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
                    var index = 0
                    while(true){
                        cacheFile.saveImgToCache(videoLayout!!.bitmap!!,index.toString())
                        Log.e("---->",index.toString())
                        index ++

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


}