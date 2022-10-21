package com.checkpoint.qr.detector.events;

import org.greenrobot.eventbus.EventBus;

/**
 * Base class for all events.
 */
public abstract class BaseEvent {
    public void broadcastEvent() {
        EventBus.getDefault().post(this);
    }
}
