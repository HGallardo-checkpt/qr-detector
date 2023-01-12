package com.checkpoint.qrdetector.events

import android.graphics.Bitmap

class OnFinishDetectionEvent(private val direction: String,
                             private val translate: String?,
                             private val image: Bitmap
) : EventBase() {


    fun getResultDirection(): String? {
        return direction
    }

    fun getResultTranslation(): String? {
        return translate
    }
    fun getImageBase64():Bitmap?{
        return image
    }

}