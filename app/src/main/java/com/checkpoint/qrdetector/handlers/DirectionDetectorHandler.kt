package com.checkpoint.qrdetector.handlers

import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.checkpoint.qrdetector.events.OnFinishDetectionEvent
import com.checkpoint.qrdetector.model.DirectionDetection

/*
* Handler process to analyze a list of objects in order to determinate direction move
* of detected QR code, the remote parameter is a List of DirectionDetection object class
*
* */


class DirectionDetectorHandler(looper: Looper) : Handler(looper) {

     override fun handleMessage(msg: android.os.Message) {
         when (Message.fromOrdinal(msg.what)) {
            Message.CALCULATE_DIRECTION -> {
                val directionDetectionList = msg.obj as List<DirectionDetection>
                if (directionDetectionList.isNotEmpty()) {
                    val size = directionDetectionList.size - 1
                    val center: Point? = directionDetectionList[0].center
                    val firstLocation: Point? = directionDetectionList[0].position
                    val secondLocation: Point? = directionDetectionList[size].position
                    if (firstLocation!!.x < secondLocation!!.x) {
                        OnFinishDetectionEvent(
                            "DOWN TO UP",
                            directionDetectionList[0].translate,
                            directionDetectionList[0].image!!
                        ).broadcastEvent()
                    } else if (firstLocation.x > secondLocation.x) {
                        OnFinishDetectionEvent(
                            "UP to DOWN",
                            directionDetectionList[0].translate,
                            directionDetectionList[0].image!!
                        ).broadcastEvent()
                    } else {
                        if (secondLocation.x > center!!.x) {
                            OnFinishDetectionEvent(
                                "DOWN TO UP",
                                directionDetectionList[0].translate,
                                directionDetectionList[0].image!!
                            ).broadcastEvent()
                        } else {
                            OnFinishDetectionEvent(
                                "UP to DOWN",
                                directionDetectionList[0].translate,
                                directionDetectionList[0].image!!
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