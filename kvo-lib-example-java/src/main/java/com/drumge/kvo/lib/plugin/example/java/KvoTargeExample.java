package com.drumge.kvo.lib.plugin.example.java;

import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.KvoEvent;

public class KvoTargeExample  {
    private static final String TAG = "KvoTargeExample";

    @KvoWatch(name = K_KvoSourceExample.index, thread = KvoWatch.Thread.WORK)
    public void onUpdateIndex(KvoEvent<KvoSourceExample, Integer> event) {
//        Log.d(TAG, "onUpdateName oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

}
