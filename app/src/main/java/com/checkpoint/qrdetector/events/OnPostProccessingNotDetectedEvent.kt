package com.checkpoint.qrdetector.events

class OnPostProccessingNotDetectedEvent(private  var detectedCode: Boolean) : EventBase() {

    fun getResult(): Boolean {
        return detectedCode
    }
}