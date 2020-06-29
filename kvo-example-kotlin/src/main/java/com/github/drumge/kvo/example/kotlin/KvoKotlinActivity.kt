package com.github.drumge.kvo.example.kotlin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.drumge.kvo.annotation.KvoWatch
import com.drumge.kvo.api.Kvo
import com.drumge.kvo.api.KvoEvent
import kotlinx.android.synthetic.main.kotlin_activity_main.*

/**
 * Created by chenrenzhan on 2019/5/11.
 *
 */
class KvoKotlinActivity : Activity() {

    private val TAG = "KvoKotlinActivity"

    private lateinit var ktSource: KtSource
    private val name: String = "name"
    private val name1: String = "name1"
    private val name2: String = "name2"
    private val name3: String = "name3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.kotlin_activity_main)

        ktSource = KtSource()

        ktSource.aa = 10
        ktSource.sChar = 'c'

        Kvo.getInstance().bind(this, ktSource)

        exampleBtn.setOnClickListener {
            ktSource.example = exampleInput.text.toString()
        }

        timeBtn.setOnClickListener {
            ktSource.time = timeInput.text.toString().toLong()
        }
    }

//    @KvoWatch(name = K_KtSource.time, thread = KvoWatch.Thread.MAIN)
    @KvoWatch(name = "time", thread = KvoWatch.Thread.MAIN)
    fun onTimeChange(event: KvoEvent<KtSource, Long>) {
        toast(TAG, "onTimeChange oldValue: ${event.oldValue}, newValue: ${event.newValue}")
    }

//    @KvoWatch(name = K_JavaSource.example, thread = KvoWatch.Thread.MAIN)
    @KvoWatch(name = "example", thread = KvoWatch.Thread.MAIN)
    fun onExampleChange(event: KvoEvent<KtSource, String>) {
        toast(TAG, "onTimeChange oldValue: ${event.oldValue}, newValue: ${event.newValue}")
    }

    private fun toast(tag: String, msg: String) {
        Toast.makeText(this, "$tag: $msg", Toast.LENGTH_SHORT).show()
        Log.i(tag, msg)
    }
}