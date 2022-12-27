package com.checkpoint.qrdetector.model

import com.checkpoint.qrdetector.enum.DirectionDetected

class DetectionResult {
    var direction: DirectionDetected? = null
    var code: String? = null
}