package com.checkpoint.qrdetector.api

import android.os.Build
import androidx.annotation.RequiresApi
import com.checkpoint.qrdetector.api.model.DetectionEventRequest
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
/*
* Class for manage api connection and post event
*/

class QRDetectionApiRest(url: String) : ApiClient() {
    private var api: APIInterfaces? = null

    init {
        api = this.getClient(url)!!.create(APIInterfaces::class.java)
    }

    /*
    * Post detection and translation events to backend
    * @params
    *   imageB64:    this  param is a string containing the source image where QR code was detected
    *   translation: human readable content of QR code
    *   direction:   enum corresponding to direction detected by the algorithm
    * */
    @RequiresApi(Build.VERSION_CODES.O)
    fun postEvent(detectionEventRequest: DetectionEventRequest):Boolean {
        val response = api?.sendDetectionEvent(detectionEventRequest)?.execute()
        return response!!.code() == 200

    }

}