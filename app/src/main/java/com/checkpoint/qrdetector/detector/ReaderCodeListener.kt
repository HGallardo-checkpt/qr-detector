package com.checkpoint.qrdetector.detector

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.common.Barcode


interface ReaderCodeListener {
    fun result(result: Pair<Bitmap?, List<Barcode>?>)
}