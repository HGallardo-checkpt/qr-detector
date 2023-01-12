package com.checkpoint.qrdetector.utils

import android.graphics.Bitmap
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import java.io.ByteArrayOutputStream

class Encode(private var bitmap: Bitmap) {


    fun encodeImage(): String? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val b = byteArrayOutputStream.toByteArray()
        return encodeToString(b, DEFAULT)
    }
}