package com.checkpoint.qrdetector.events

import org.greenrobot.eventbus.EventBus


abstract class EventBase {
    fun broadcastEvent() {
        EventBus.getDefault().post(this)
    }
}