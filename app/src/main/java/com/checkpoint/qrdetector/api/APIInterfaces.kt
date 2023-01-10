package com.checkpoint.qrdetector.api
import com.checkpoint.qrdetector.api.model.DetectionEventRequest
import com.checkpoint.qrdetector.api.model.DetectionEventResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
interface APIInterfaces {

    @POST("/")
    fun sendDetectionEvent( @Body event: DetectionEventRequest): Call<String>

}