package com.checkpoint.qrdetector.api.model

import com.checkpoint.qrdetector.enum.DirectionDetected
import com.google.gson.annotations.SerializedName
/*
* Object data to send at endpoint
*
* */
data class DetectionEventRequest(
    // Image containing the QR detected in base 64 format
    @SerializedName("image") val image: String,
    // Textual tranlation of QR code
    @SerializedName("translation") val translation: String,
    // Direction detected by the algorithm
    @SerializedName("direction") val direction: String,
    // Date and time of event detection occurs
    @SerializedName("date") val date: String,

    )
