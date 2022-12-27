package com.checkpoint.qrdetector.events

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.common.Barcode


class OnPostProccessingCompletedEvent(private  var detectionResult: Pair<Bitmap, MutableList<Barcode>>) : EventBase() {

    fun getResult(): Pair<Bitmap?, List<Barcode>?>? {
        return detectionResult
    }
}