package com.checkpoint.qrdetector.handlers

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.checkpoint.qrdetector.detector.QRCodeReader
import com.checkpoint.qrdetector.events.OnPostProccessingCompletedEvent
import com.checkpoint.qrdetector.events.OnPostProccessingNotDetectedEvent


class InferenceHandler(looper: Looper) : Handler(looper) {

    private var qrCodeReader: QRCodeReader? = null

    override fun handleMessage(msg: android.os.Message) {
        when (Message.fromOrdinal(msg.what)) {
            Message.INIT_DETECTOR -> handleConfigureDetector()
            Message.RUN_INFERENCE -> {
                val imageBmp = msg.obj as Bitmap
                handleRunningInference(imageBmp)
            }
         }
    }

    private fun handleConfigureDetector() {
        Log.e("ConfigureDetector->","Creating reader intance")
         qrCodeReader = QRCodeReader()
    }

    private fun handleRunningInference(imageBmp: Bitmap) {
        val resultInference = qrCodeReader!!.readCode(imageBmp)
        resultInference
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()){

                     OnPostProccessingNotDetectedEvent(true).broadcastEvent()

                }else{

                     OnPostProccessingCompletedEvent(Pair(imageBmp!!,barcodes!!)).broadcastEvent()
                }

        }


    }

    enum class Message {
        INIT_DETECTOR,
        RUN_INFERENCE;

        companion object {
            private val VALUES = values()

            fun fromOrdinal(ordinal: Int): Message {
                return VALUES[ordinal]
            }
        }
    }

}