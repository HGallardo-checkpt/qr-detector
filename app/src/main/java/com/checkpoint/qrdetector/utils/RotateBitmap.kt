package com.checkpoint.qrdetector.utils

import android.graphics.Bitmap
import android.graphics.Matrix


class RotateBitmap() {

    fun rotate(original: Bitmap, degrees: Float): Bitmap? {
        val x = original.width
        val y = original.height
        val matrix = Matrix()
        matrix.preRotate(degrees)
        return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    }
}