package com.checkpoint.qrdetector.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.*


@Suppress("DEPRECATION")
class NV21toBitmap(context: Context?) {
    private val rs: RenderScript
    private val yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB
    init {
        rs = RenderScript.create(context)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    }
    fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap {
        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(nv21.size)
        val input = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        val out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        input.copyFrom(nv21)
        yuvToRgbIntrinsic.setInput(input)
        yuvToRgbIntrinsic.forEach(out)
        val bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.copyTo(bmpout)
        return bmpout
    }


}