package com.github.drumge.kvo.example.kotlin

import com.drumge.kvo.annotation.KvoIgnore
import com.drumge.kvo.annotation.KvoSource

/**
 * Created by chenrenzhan on 2019/5/11.
 *
 */
@KvoSource
class KtSource {
    public var publicVar: Int = 1
    public val publicVal: String = "efde"
    @KvoIgnore
    var aa: Int = 0
    var example: String? = "3"
    var index: Int? = 0
    var time: Long = 0
    var mShort: Short = 0
    var mByte: Byte = 0
    var mInt: Int = 0
    var mFloat: Float = 0.toFloat()
    var mDouble: Double = 0.toDouble()
    var mBoolean: Boolean = false
    var sChar: Char = ' '
}