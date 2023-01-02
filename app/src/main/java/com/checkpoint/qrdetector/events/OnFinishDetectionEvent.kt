package com.checkpoint.qrdetector.events

class OnFinishDetectionEvent(private val direction: String,
                             private val translate: String?,
                             private val imageB64: String) : EventBase() {


    fun getResultDirection(): String? {
        return direction
    }

    fun getResultTranslation(): String? {
        return translate
    }
    fun getImageBase64():String?{
        return imageB64
    }

}