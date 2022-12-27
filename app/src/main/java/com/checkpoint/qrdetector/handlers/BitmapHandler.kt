package com.checkpoint.qrdetector.handlers

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.checkpoint.qrdetector.events.OnDetectionProcessEvent
import com.checkpoint.qrdetector.utils.CacheFile

/*
* This looper thread receive a number of bitmaps peer second ,
* this number is determinate by camera capabilities
*
* */
class BitmapHandler (looper: Looper) : Handler(looper) {
    private var  counter = 0
     override fun handleMessage(msg: android.os.Message) {
        when (Message.fromOrdinal(msg.what)) {
            //Handler message
            Message.SET_BITMAP -> {
                val currentBitmap = msg.obj as Bitmap
                counter ++
                Log.e("BitmapHandler -->","RECEIVE BITMAP FROM CAMERA")
                //Evento to response bitmap for postproccesing.
                OnDetectionProcessEvent(currentBitmap).broadcastEvent()
            }
         }
    }

    enum class Message {
        //Handler message
        SET_BITMAP;
        companion object {
            private val VALUES = values()
    fun fromOrdinal(ordinal: Int): Message {
                return VALUES[ordinal]
            }
        }
    }
}