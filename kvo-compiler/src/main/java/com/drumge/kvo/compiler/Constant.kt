package com.drumge.kvo.compiler

import com.drumge.kvo.api.KvoEvent

/**
 * Created by chenrenzhan on 2019/5/18.
 *
 */


const val TAG = "KvoProcessor"

const val JAVA_DOC = "Automatically generated file. DO NOT MODIFY.\n"
const val KVO_PROXY_CREATOR_INSTANCE = "com.drumge.kvo.inner.KvoTargetProxyCreator.getInstance()"
@JvmField
val KVO_EVENT_NAME = KvoEvent::class.java.name
const val SOURCE_FILED_CLASS_PREFIX = "K_"
const val GET_NAME_METHOD_PREFIX = "kw_"
const val INIT_VALUE_METHOD_PREFIX = "initValue_"
const val SOURCE_CLASS_SUFFIX = "_K_KvoSource"
const val PROXY_CLASS_SUFFIX = "_K_KvoTargetProxy"
const val CREATOR_CLASS_SUFFIX = "_K_KvoTargetCreator"
const val TARGET_CLASS_FIELD = "weakTarget"
const val NOTIFY_WATCHER_NAME = "name"
const val NOTIFY_WATCHER_EVENT = "event"
const val EVENT_GET_TAG = "getTag"
const val EQUALS_TARGET_METHOD = "equalsTarget"
const val IS_TARGET_VALID_METHOD = "isTargetValid"