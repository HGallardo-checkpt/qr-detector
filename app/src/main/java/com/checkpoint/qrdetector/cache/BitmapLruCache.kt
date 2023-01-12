package com.checkpoint.qrdetector.cache

import android.graphics.Bitmap
import androidx.collection.LruCache


class BitmapLruCache(maxSize: Int) : LruCache<String?, Bitmap?>(maxSize) {
    protected fun sizeOf(key: String?, value: Bitmap): Int {
        return value.byteCount / 1024
    }

    fun getBitmap(key: String?): Bitmap? {
        return this.get(key!!)
    }


    fun setBitmap(key: String?, bitmap: Bitmap?) {
        if (!hasBitmap(key)) {
            this.put(key!!, bitmap!!)
        }
    }

    fun hasBitmap(key: String?): Boolean {
        return getBitmap(key) != null
    }

    companion object {
        // use 1/8th of the available memory for this memory cache.
        val cacheSize: Int
            get() {
                val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
                // use 1/8th of the available memory for this memory cache.
                return maxMemory / 8
            }
    }
}