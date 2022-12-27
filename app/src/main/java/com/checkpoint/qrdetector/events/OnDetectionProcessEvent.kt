package com.checkpoint.qrdetector.events

import android.graphics.Bitmap


/* Process event to send bitmap from camera to detection thread */

class OnDetectionProcessEvent(private  var bitmap: Bitmap?): EventBase() {

    fun getResult(): Bitmap? {
        return bitmap
    }
}