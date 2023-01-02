package com.checkpoint.qrdetector.api.model

import com.google.gson.annotations.SerializedName

data class DetectionEventResponse(
    @SerializedName("total")val  total: Int
)