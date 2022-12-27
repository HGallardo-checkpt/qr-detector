package com.checkpoint.qrdetector.utils

import android.content.Context
import android.graphics.Bitmap

import android.text.TextUtils
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class CacheFile(private  var context: Context) {
    private val CHILD_DIR = "/images"
    private val TEMP_FILE_NAME = "img"
    private val FILE_EXTENSION = ".png"

    private val COMPRESS_QUALITY = 100
    fun saveImgToCache(bitmap: Bitmap, name: String): File? {
        var cachePath: File? = null
        var fileName: String = TEMP_FILE_NAME
        if (!TextUtils.isEmpty(name)) {
            fileName = name
        }
        try {
            cachePath = File(context.cacheDir.path+CHILD_DIR)
            cachePath!!.mkdirs()
            val stream = FileOutputStream("$cachePath/$fileName$FILE_EXTENSION")
            bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESS_QUALITY, stream)
            stream.close()
        } catch (e: IOException) {
            Log.e("", "saveImgToCache error: $bitmap", e)
        }
        return cachePath
    }
}