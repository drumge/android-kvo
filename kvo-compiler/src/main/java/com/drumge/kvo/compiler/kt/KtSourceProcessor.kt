package com.drumge.kvo.compiler.kt

import com.drumge.kvo.annotation.KvoSource
import com.drumge.kvo.compiler.JAVA_DOC
import com.drumge.kvo.compiler.KvoSourceInfo
import com.drumge.kvo.compiler.SOURCE_CLASS_SUFFIX
import com.drumge.kvo.compiler.SOURCE_FILED_CLASS_PREFIX
import com.drumge.kvo.compiler.checkFieldLegal
import com.drumge.kvo.compiler.format
import com.drumge.kvo.compiler.getPackage
import com.drumge.kvo.compiler.getSimpleName
import com.drumge.kvo.compiler.writeFile
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.util.ArrayList
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * Created by chenrenzhan on 2019/5/18.
 *
 */
class KtSourceProcessor(private val processingEnv: ProcessingEnvironment) {

    fun genKSource(info: KvoSourceInfo) {
        val originalSimpleName = getSimpleName(info.className)
        val simpleName = SOURCE_FILED_CLASS_PREFIX + originalSimpleName
        val pack = getPackage(info.className)
        var kvoSourceBuilder = TypeSpec.objectBuilder(simpleName)
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
            .addKdoc(JAVA_DOC)
        if (info.clsElement != null) {
            val fields = getPrivateFields(info.clsElement)
            kvoSourceBuilder.addProperties(fields)
            genTempClass(info.clsElement, SOURCE_CLASS_SUFFIX)
        }
        if (info.innerCls != null && !info.innerCls.isEmpty()) {
            val innerList = ArrayList<TypeSpec>(info.innerCls.size)
            for (te in info.innerCls) {
                val fields = getPrivateFields(te)
                val typeSpec = TypeSpec.interfaceBuilder(te.simpleName.toString())
                    .addModifiers(KModifier.PUBLIC, KModifier.FINAL)
                    .addProperties(fields)
                    .addKdoc(JAVA_DOC)
                    .build()
                innerList.add(typeSpec)
                genTempClass(pack, originalSimpleName + "$" + te.simpleName + SOURCE_CLASS_SUFFIX)
            }
            kvoSourceBuilder.addTypes(innerList)
        }

        writeFile(pack, kvoSourceBuilder, processingEnv.filer)
    }

    private fun getPrivateFields(te: TypeElement): List<PropertySpec> {
        val kvoSource = te.getAnnotation(KvoSource::class.java)
        val check = kvoSource.check
        val cElements = te.enclosedElements
        val fields = ArrayList<PropertySpec>()
        for (e in cElements) {
            if (e.kind != ElementKind.FIELD) {
                continue
            }
            if (e.modifiers.contains(Modifier.FINAL)) {
                continue
            }
            if (check) {
                checkFieldLegal(te, e)
            }
            val fieldName: String
            if (e.modifiers.contains(Modifier.PRIVATE)) {
                val name = e.simpleName.toString()
                fieldName = name
                fields.add(genField(fieldName, name))
            }
        }
        return fields
    }

    private fun genField(name: String, value: String): PropertySpec {
        return PropertySpec.builder(name, String::class, KModifier.CONST, KModifier.FINAL, KModifier.PUBLIC)
            .initializer(format("\"%s\"", value))
            .build()
    }

    private fun genTempClass(eClass: Element, suffix: String) {
        val pack = getPackage(eClass.toString())
        val simpleName = "${eClass.simpleName}$suffix"
        genTempClass(pack, simpleName)
    }

    private fun genTempClass(pack: String, simpleName: String) {
        val builder = TypeSpec.classBuilder(simpleName)
            .addKdoc("$JAVA_DOC\n just intermediate class, not been packaged in apk")
        writeFile(pack, builder, processingEnv.filer)
    }
}
