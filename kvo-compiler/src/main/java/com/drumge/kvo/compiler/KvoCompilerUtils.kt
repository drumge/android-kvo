package com.drumge.kvo.compiler

import com.drumge.kvo.annotation.KvoIgnore
import com.drumge.kvo.compiler.Log.w
import com.squareup.javapoet.TypeName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

/**
 * Created by chenrenzhan on 2019/5/18.
 *
 */

object Log {
    private var messager: Messager? = null

    @JvmStatic
    fun init(messager: Messager) {
        Log.messager = messager
    }

    @JvmStatic
    fun i(tag: String, msg: String, vararg format: Any) {
        messager!!.printMessage(Diagnostic.Kind.NOTE, format("[%s]\t", tag) + format(msg, *format))
    }

    @JvmStatic
    fun w(tag: String, msg: String, vararg format: Any) {
        messager!!.printMessage(Diagnostic.Kind.WARNING, format("[%s]\t", tag) + format(msg, *format))
    }

    @JvmStatic
    fun e(tag: String, msg: String, vararg format: Any) {
        messager!!.printMessage(Diagnostic.Kind.ERROR, format("[%s]\t", tag) + format(msg, *format))
    }
}

fun writeFile(pack: String, builder: TypeSpec.Builder, filer: Filer) {
    try {
        val ktFile = FileSpec.get(pack, builder.build())
        ktFile.writeTo(filer)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun checkFieldLegal(eClass: Element, e: Element): Boolean {
    if (e.kind == ElementKind.FIELD
        && e.getAnnotation(KvoIgnore::class.java) == null
        && !e.modifiers.contains(Modifier.PRIVATE)) {
        Log.e(TAG,
            "%s#%s is illegal, it may need to be private, or you may add @KvoIgnore to the field, or add @KvoSource(check = false) to the class",
            eClass, e)
        return false
    }
    return true
}

fun typeNameWithoutTypeArguments(targetType: TypeName): String {
    val name = targetType.toString()
    return typeNameWithoutTypeArguments(name)
}

fun typeNameWithoutTypeArguments(name: String): String {
    val index = name.indexOf("<")
    var typeName = name
    if (index > 0) {
        typeName = name.substring(0, index)
    }
    return typeName
}

fun format(msg: String, vararg format: Any): String {
    return String.format(Locale.getDefault(), msg, *format)
}

fun getPackage(name: String): String {
    val idx = name.lastIndexOf(".")
    return if (idx < 0) {
        ""
    } else name.substring(0, idx)
}

fun getSimpleName(name: String): String {
    val idx = name.lastIndexOf(".")
    return name.substring(if (idx < 0) 0 else idx + 1, name.length)
}

/**
 * 获取需要把java类型映射成kotlin类型的ClassName  如：java.lang.String 在kotlin中的类型为kotlin.String 如果是空则表示该类型无需进行映射
 */
fun Element.javaToKotlinType(): ClassName {
    return javaToKotlinType(this.asType().asTypeName().toString())
}

fun javaToKotlinType(type: String): ClassName {
    val className = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(type))?.asSingleFqName()?.asString()
    return if (className == null) {
        ClassName.bestGuess(type)
    } else {
        ClassName.bestGuess(className)
    }
}

/**
 * 写入数据
 */
fun writeText(filePath: String, content: String) {
    createFile(filePath)
    File(filePath).writeText(content)
}

/**
 * 创建文件
 * filePath 文件路径
 */
fun createFile(filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        file.createNewFile()
    }
}

//val className = element.javaToKotlinType() ?: element.asType().asTypeName()

