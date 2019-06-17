package com.github.drumge.kvo.example.kotlin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.drumge.kvo.annotation.KvoWatch
import com.drumge.kvo.api.Kvo
import com.drumge.kvo.api.KvoEvent
import com.github.drumge.kvo.example.java.K_JavaSource

/**
 * Created by chenrenzhan on 2019/5/11.
 *
 */
class KvoKotlinActivity : Activity() {

    private val TAG = "KvoKotlinActivity"

    private lateinit var ktSource: KtSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ktSource = KtSource()

        ktSource.aa = 10
        ktSource.sChar = 'c'

        Kvo.getInstance().bind(this, ktSource)
    }

    @KvoWatch(name = K_KtSource.time, thread = KvoWatch.Thread.MAIN)
    fun onTimeChange(event: KvoEvent<KtSource, Long>) {
        Log.i(TAG, "onTimeChange oldValue: ${event.oldValue}, newValue: ${event.newValue}, source: ${event.source}")
    }

//    @KvoWatch(name = K_JavaSource.example, thread = KvoWatch.Thread.MAIN)
    fun onExampleChange(event: KvoEvent<KtSource, String>) {
        Log.i(TAG, "onTimeChange oldValue: ${event.oldValue}, newValue: ${event.newValue}, source: ${event.source}")
    }
}