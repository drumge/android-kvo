package com.github.drumge.kvo.example.java;

import android.util.Log;

import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.KvoEvent;
import com.github.drumge.kvo.example.kotlin.K_KtSource;
import com.github.drumge.kvo.example.kotlin.KtSource;

import static android.content.ContentValues.TAG;

/**
 * Created by chenrenzhan on 2019/6/11.
 */
public class JavaTargert {

    // @KvoWatch(name = K_KtSource.time, thread = KvoWatch.Thread.MAIN)
    void onTimeChange(KvoEvent<KtSource, Long> event) {
        Log.i(TAG, "onTimeChange oldValue: ${event.oldValue}, newValue: ${event.newValue}, source: ${event.source}");
    }

    // @KvoWatch(name = K_JavaSource.example, thread = KvoWatch.Thread.MAIN)
    void onExampleChange(KvoEvent<KtSource, String> event) {
        Log.i(TAG, "onTimeChange oldValue: ${event.oldValue}, newValue: ${event.newValue}, source: ${event.source}");
    }
}
