package com.checkpoint.qr.detector.events;

public class OnFinishDetectionEvent extends BaseEvent {

    public String direction;
    public String translation;

    public OnFinishDetectionEvent(String direction,String translation) {
        this.direction = direction;
        this.translation = translation;
    }

    public String getResultDirection() {

        return direction;

    }

    public String getResultTranslation() {
        return translation;
    }
}
