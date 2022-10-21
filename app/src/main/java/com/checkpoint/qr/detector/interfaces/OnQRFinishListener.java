package com.checkpoint.qr.detector.interfaces;

import com.checkpoint.qr.detector.pojos.DetectionResult;

public interface OnQRFinishListener {
    void setResult(DetectionResult detectionResult);
}
