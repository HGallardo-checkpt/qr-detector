package com.checkpoint.qrdetector.detector

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage


class QRCodeReader internal constructor() {
    private var scanner: BarcodeScanner? = null
    private var options: BarcodeScannerOptions? = null

    init {
        options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        scanner = BarcodeScanning.getClient(options!!)


    }

    fun readCode(bitmap: Bitmap,listener: ReaderCodeListener) {
         val image = InputImage.fromBitmap(bitmap, 0)
         scanner!!.process(image)
            .addOnSuccessListener { barcodes ->
                listener.result(Pair(bitmap,barcodes))
            }
            .addOnFailureListener {
                listener.result(Pair(null,null))
            }
        }
    fun readCode(bitmap: Bitmap): Task<MutableList<Barcode>> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return scanner!!.process(image)

    }

}



