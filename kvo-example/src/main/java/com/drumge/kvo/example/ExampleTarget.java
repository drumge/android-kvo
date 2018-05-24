package com.drumge.kvo.example;

import android.util.Log;

import com.drumge.kvo.api.Kvo;
import com.drumge.kvo.api.KvoEvent;
import com.drumge.kvo.annotation.KvoWatch;

/**
 * Created by chenrenzhan on 2018/5/3.
 */

public class ExampleTarget {
    private static String TAG = "ExampleTarget";

    ExampleSource tag1;
    ExampleSource tag2;
    ExampleSource tag3;

    public ExampleTarget() {
        tag1 = new ExampleSource();
        tag2 = new ExampleSource();
        tag3 = new ExampleSource();
    }

    public ExampleSource getTag1() {
        return tag1;
    }

    public ExampleSource getTag2() {
        return tag2;
    }

    public ExampleSource getTag3() {
        return tag3;
    }

    public void bindKvo() {
        Kvo.getInstance().bind(this, tag1, "tag1", false);
        Kvo.getInstance().bind(this, tag2, "tag2");
        Kvo.getInstance().bind(this, tag3);
    }

    public void unbindKvo() {
        Kvo.getInstance().unbind(this, tag1);
        Kvo.getInstance().unbind(this, tag2);
        Kvo.getInstance().unbind(this, tag3);
    }

    @KvoWatch(name = K_ExampleSource.example, tag = "tag1", thread = KvoWatch.Thread.MAIN)
    public void onUpdateExampleTag1(KvoEvent<ExampleSource, String> event) {
        Log.d(TAG, "onUpdateExampleTag1 oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());
    }

    @KvoWatch(name = K_ExampleSource.example, tag = "tag2", thread = KvoWatch.Thread.WORK)
    public void onUpdateExampleTag2(KvoEvent<ExampleSource, String> event) {
        Log.d(TAG, "onUpdateExampleTag2 oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

    @KvoWatch(name = K_ExampleSource.index)
    public void onUpdateIndex(KvoEvent<ExampleSource, Integer> event) {
        Log.d(TAG, "onUpdateIndex oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }

    @KvoWatch(name = K_ExampleSource.sChar)
    public void onUpdateChat(KvoEvent<ExampleSource, Character> event) {
        Log.d(TAG, "onUpdateChat oldValue: " + event.getOldValue() + ", newValue: " + event.getNewValue());

    }
}
