package com.checkpoint.qrdetector.utils

import android.graphics.Bitmap
import android.graphics.Matrix

import android.graphics.RectF




class Resize(private var bitmap: Bitmap) {

    fun scale(width: Int, height: Int): Bitmap? {
        val matrix = Matrix()
        matrix.setRectToRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}