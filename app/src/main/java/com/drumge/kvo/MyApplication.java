package com.drumge.kvo;

import android.app.Application;

/**
 * Created by chenrenzhan on 2018/5/24.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        KvoInit.initKvo();
    }
}
