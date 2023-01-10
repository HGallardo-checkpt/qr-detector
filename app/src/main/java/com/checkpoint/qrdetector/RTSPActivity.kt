package com.checkpoint.qrdetector

import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.checkpoint.qrdetector.databinding.ActivityCameraBinding
import com.checkpoint.qrdetector.databinding.ActivityRtspactivityBinding
import com.checkpoint.qrdetector.utils.CacheFile
import com.google.android.renderscript.Toolkit
import com.google.android.renderscript.YuvFormat
import ir.am3n.rtsp.client.Rtsp
import ir.am3n.rtsp.client.data.Frame
import ir.am3n.rtsp.client.data.SdpInfo
import ir.am3n.rtsp.client.interfaces.RtspFrameListener
import ir.am3n.rtsp.client.interfaces.RtspStatusListener


class RTSPActivity : AppCompatActivity(), RtspFrameListener {
    private val url = "rtsp://192.168.100.159:554/onvif1"
    private lateinit  var surfaceView: SurfaceView
    private lateinit var binding: ActivityRtspactivityBinding
    private lateinit var rtsp: Rtsp
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         binding = ActivityRtspactivityBinding.inflate(layoutInflater)
        surfaceView = binding.svVideo
        setContentView(binding.root)
        rtsp = Rtsp()
         rtsp.init(url, "", "")
        rtsp.setStatusListener(object : RtspStatusListener {
            override fun onConnecting() {}
            override fun onConnected(sdpInfo: SdpInfo) {}
            override fun onDisconnected() {}
            override fun onUnauthorized() {}
            override fun onFailed(message: String?) {}
        })

        rtsp.setFrameListener(this)
        rtsp.setSurfaceView(surfaceView)
        rtsp.setRequestBitmap(false)
        rtsp.setRequestYuvBytes(true)


    }

    override fun onStart() {
        super.onStart()
        rtsp.start(autoPlayAudio = false)
    }
    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun onVideoNalUnitReceived(frame: Frame?) {
     }

    override fun onVideoFrameReceived(
        width: Int,
        height: Int,
        mediaImage: Image?,
        yuv420Bytes: ByteArray?,
        bitmap: Bitmap?
    ) {
        val nBitmap = Toolkit.yuvToRgbBitmap(yuv420Bytes!!, width, height, YuvFormat.NV21)
        CacheFile(this).saveImgToCache(nBitmap!!,index.toString())
        index ++
     }

    override fun onAudioSampleReceived(frame: Frame?) {

    }

}