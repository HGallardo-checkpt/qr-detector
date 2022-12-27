package com.checkpoint.qrdetector.opencv

import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.videoio.VideoCapture


class WIFICamera {

    init {
        var videoCapture =  VideoCapture("")

    }
   fun validate(){
       if (OpenCVLoader.initDebug()) {
           Log.d("myTag", "OpenCV loaded")
       }

   }
}