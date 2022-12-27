package com.checkpoint.qrdetector.handlers

import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.checkpoint.qrdetector.events.OnFinishDetectionEvent

import com.checkpoint.qrdetector.model.DirectionDetection




class DirectionDetectorHandler(looper: Looper) : Handler(looper) {

     override fun handleMessage(msg: android.os.Message) {
         when (Message.fromOrdinal(msg.what)) {
            Message.CALCULATE_DIRECTION -> {
                Log.e("LAUNCH DETECTOR", "---->")
                val directionDetectionList = msg.obj as List<DirectionDetection>
                Log.e("----->", "" + directionDetectionList.size)
                if (directionDetectionList.isNotEmpty()) {
                    val size = directionDetectionList.size - 1
                    val center: Point? = directionDetectionList[0].center
                    val firstLocation: Point? = directionDetectionList[0].position
                    val secondLocation: Point? = directionDetectionList[size].position
                    Log.e("center", "" + center.toString())
                    Log.e("firstLocation", "" + firstLocation.toString())
                    Log.e("secondLocation", "" + secondLocation.toString())
                    if (firstLocation!!.x < secondLocation!!.x) {
                        OnFinishDetectionEvent(
                            "DOWN TO UP",
                            directionDetectionList[0].translate
                        ).broadcastEvent()
                    } else if (firstLocation.x > secondLocation.x) {
                        OnFinishDetectionEvent(
                            "UP to DOWN",
                            directionDetectionList[0].translate
                        ).broadcastEvent()
                    } else {
                        if (secondLocation.x > center!!.x) {
                            OnFinishDetectionEvent(
                                "DOWN TO UP",
                                directionDetectionList[0].translate
                            ).broadcastEvent()
                        } else {
                            OnFinishDetectionEvent(
                                "UP to DOWN",
                                directionDetectionList[0].translate
                            ).broadcastEvent()
                        }
                    }
                }
            }
        }
    }
    enum class Message {
        CALCULATE_DIRECTION;

        companion object {
            private val VALUES = values()
            fun fromOrdinal(ordinal: Int): Message {
                return VALUES[ordinal]
            }
        }
    }

}