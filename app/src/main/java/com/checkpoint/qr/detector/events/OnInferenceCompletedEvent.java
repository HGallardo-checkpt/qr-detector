package com.checkpoint.qr.detector.events;

import com.checkpoint.qr.detector.events.BaseEvent;

public class OnInferenceCompletedEvent extends BaseEvent {
    private final byte[] imageBytes;

    public OnInferenceCompletedEvent(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public byte[] getImageAsBytes() {
        return imageBytes;
    }

}
