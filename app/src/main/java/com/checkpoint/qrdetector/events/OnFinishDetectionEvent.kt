package com.checkpoint.qrdetector.events

class OnFinishDetectionEvent(private val direction: String,private val translate: String?) : EventBase() {


    fun getResultDirection(): String? {
        return direction
    }

    fun getResultTranslation(): String? {
        return translate
    }
}