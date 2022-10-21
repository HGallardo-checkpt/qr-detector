package com.checkpoint.qr.detector.events;

import android.graphics.Bitmap;
import com.checkpoint.qr.detector.pojos.DetectionResult;

public class OnDetectionProcessEvent extends BaseEvent {

    public Bitmap bitmap;

    public OnDetectionProcessEvent(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getResult() {

        return bitmap;

    }
}