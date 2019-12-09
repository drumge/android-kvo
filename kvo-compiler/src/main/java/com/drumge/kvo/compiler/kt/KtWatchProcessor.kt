package com.drumge.kvo.compiler.kt

import com.drumge.kvo.annotation.KvoAssist
import com.drumge.kvo.annotation.KvoWatch
import com.drumge.kvo.api.KvoEvent
import com.drumge.kvo.compiler.CREATOR_CLASS_SUFFIX
import com.drumge.kvo.compiler.EQUALS_TARGET_METHOD
import com.drumge.kvo.compiler.EVENT_GET_TAG
import com.drumge.kvo.compiler.GET_NAME_METHOD_PREFIX
import com.drumge.kvo.compiler.INIT_VALUE_METHOD_PREFIX
import com.drumge.kvo.compiler.IS_TARGET_VALID_METHOD
import com.drumge.kvo.compiler.JAVA_DOC
import com.drumge.kvo.compiler.KVO_PROXY_CREATOR_INSTANCE
import com.drumge.kvo.compiler.KvoTargetInfo
import com.drumge.kvo.compiler.Log
import com.drumge.kvo.compiler.NOTIFY_WATCHER_EVENT
import com.drumge.kvo.compiler.NOTIFY_WATCHER_NAME
import com.drumge.kvo.compiler.PROXY_CLASS_SUFFIX
import com.drumge.kvo.compiler.ProcessVariableElement
import com.drumge.kvo.compiler.TAG
import com.drumge.kvo.compiler.TARGET_CLASS_FIELD
import com.drumge.kvo.compiler.javaToKotlinType
import com.drumge.kvo.compiler.typeNameWithoutTypeArguments
import com.drumge.kvo.compiler.writeFile
import com.drumge.kvo.inner.IKvoTargetCreator
import com.drumge.kvo.inner.IKvoTargetProxy
import com.drumge.kvo.inner.log.KLog
import com.drumge.kvo.inner.thread.KvoThread
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.lang.ref.WeakReference
import java.util.HashSet
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

/**
 * Created by chenrenzhan on 2019/5/20.
 *
 */
class KtWatchProcessor(private val processingEnv: ProcessingEnvironment) {

    fun genTargetClass(info: KvoTargetInfo) {
        val targetType = info.target.asType().asTypeName()
        val weakTargetType = WeakReference::class.asClassName().parameterizedBy(targetType)
        val targetClassName = info.simpleName + PROXY_CLASS_SUFFIX
        val builder = TypeSpec.classBuilder(targetClassName)
            .addKdoc(JAVA_DOC)
            .addSuperinterface(IKvoTargetProxy::class.asClassName().parameterizedBy(targetType))

        if (info.target is TypeElement) {
            val te = info.target as TypeElement
            for (tpe in te.typeParameters) {
                builder.addTypeVariable(TypeVariableName.invoke(tpe.simpleName.toString()))
            }
        }

        val fTarget = PropertySpec.builder(TARGET_CLASS_FIELD, weakTargetType, KModifier.PRIVATE)
            .build()

        val target = "target"
        val constructor = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PUBLIC)
            .addParameter(target, targetType)
            .addCode("this.%L = %T(%L);\n", TARGET_CLASS_FIELD, weakTargetType, target)
            .build()

        val p = "obj"
        val equals = FunSpec.builder("equals")
            .returns(Boolean::class)
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addParameter(p, Any::class.asTypeName().copy(true))
            .addCode("if (this === %L) {\n" +
                "   return true;\n" +
                "} else if (%L is %L) {\n" +
                "   return this.%L.get() === %L.%L.get();\n" +
                "}\n" +
                "return false;\n", p, p, targetClassName, TARGET_CLASS_FIELD, p, TARGET_CLASS_FIELD)
            .build()

        val hashCode = FunSpec.builder("hashCode")
            .returns(Int::class)
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addCode("if (%L.get() != null) {\n" +
                "   return %L.get()!!.hashCode();\n" +
                "} \n" +
                "return super.hashCode();\n", TARGET_CLASS_FIELD, TARGET_CLASS_FIELD)
            .build()

        val notify = FunSpec.builder("notifyWatcher")
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addParameter(NOTIFY_WATCHER_NAME, String::class)
            .addParameter(NOTIFY_WATCHER_EVENT, KvoEvent::class.asTypeName().parameterizedBy(Any::class.asTypeName(),
                Any::class.asTypeName()))
            .addCode(genNotifyWatchBlock(info))
            .build()

        builder.addProperty(fTarget)
            .addFunctions(genWatchNameMethods(info))
            .addFunctions(genInitValueMethods(info))
            .addFunction(constructor)
            .addFunction(notify)
            .addFunction(equals)
            .addFunction(genEqualsTargetMethod(targetType))
            .addFunction(genIsTargetValidMethod())
            .addFunction(hashCode)

        writeFile(info.packageName, builder, processingEnv.filer)
    }

    fun genCreatorClass(info: KvoTargetInfo) {
        val creatorClassName = info.simpleName + CREATOR_CLASS_SUFFIX
        val proxyClassName = info.simpleName + PROXY_CLASS_SUFFIX
        val targetType = info.target.asType().asTypeName()
        val proxyType = ClassName.bestGuess(info.packageName + "." + proxyClassName)


        val builder = TypeSpec.classBuilder(creatorClassName)
            .addKdoc(JAVA_DOC)
            .addSuperinterface(IKvoTargetCreator::class.asClassName().parameterizedBy(proxyType, targetType))
            .addOriginatingElement(ProcessVariableElement(info.target))

        if (info.target is TypeElement) {
            val te = info.target as TypeElement
            for (tpe in te.typeParameters) {
                builder.addTypeVariable(TypeVariableName.invoke(tpe.simpleName.toString()))
            }
        }

        val creatorMethod = FunSpec.builder("createTarget")
            .addParameter("target", targetType)
            .addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .returns(proxyType)
            .addCode("return %T(target);\n", proxyType)
            .build()

        val creatorType = ClassName.bestGuess(info.packageName + "." + creatorClassName)

        val newCreator = FunSpec.builder("registerCreator")
            .returns(Int::class)
            .addAnnotation(JvmStatic::class)
            .addModifiers(KModifier.PUBLIC)
            .addCode("%L.registerTarget(%L::class.java, %T());\n", KVO_PROXY_CREATOR_INSTANCE, info.target.toString(),
                creatorType)
            .addCode("return 0;\n", creatorType)
            .build()

        val companion = TypeSpec.companionObjectBuilder()
            .addFunction(newCreator)
            .build()

        builder.addFunction(creatorMethod)
            .addType(companion)

        writeFile(info.packageName, builder, processingEnv.filer)
    }

    private fun genEqualsTargetMethod(targetType: TypeName): FunSpec {
        return FunSpec.builder(EQUALS_TARGET_METHOD)
            .returns(Boolean::class)
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL, KModifier.OVERRIDE)
            .addParameter(TARGET_CLASS_FIELD, Object::class)
            .addCode("if (%L is %L) { \n" +
                "return this.%L.get() === %L;\n" +
                "} \n" +
                "return false;\n",
                TARGET_CLASS_FIELD, typeNameWithoutTypeArguments(targetType.toString()), TARGET_CLASS_FIELD, TARGET_CLASS_FIELD)
            .build()
    }

    private fun genIsTargetValidMethod(): FunSpec {
        return FunSpec.builder(IS_TARGET_VALID_METHOD)
            .returns(Boolean::class)
            .addModifiers(KModifier.PUBLIC, KModifier.FINAL, KModifier.OVERRIDE)
            .addCode("return this.%L.get() != null;\n",
                TARGET_CLASS_FIELD)
            .build()
    }

    private fun genNotifyWatchBlock(info: KvoTargetInfo): CodeBlock {
        val target = "target"
        val targetType = info.target.asType().asTypeName()
        val kLog = KLog::class.asTypeName()
        val methods = info.methods
        val block = CodeBlock.builder()
        block.add("val %N:%L? = %L.get();\n" +
            "      if (%L == null) {\n" +
            "          %T.error(%S, \"notifyWatcher target object is null, name:\" + name);\n" +
            "          return;\n" +
            "      }\n", target, targetType, TARGET_CLASS_FIELD, target, kLog, info.target.simpleName)
        for (e in methods) {
            val getName = GET_NAME_METHOD_PREFIX + e.simpleName.toString() + "()"
            val initName = INIT_VALUE_METHOD_PREFIX + e.simpleName.toString()
            val w = e.getAnnotation(KvoWatch::class.java)
            val thread = w.thread
            val kvoWatchName = KvoWatch::class.asClassName()
            val kvoThreadName = KvoThread::class.asClassName()
            val methodName = e.simpleName.toString()
            val ps = e.parameters
            val param = ps[0]
            val types = getTypes(param)
            val eventType = KvoEvent::class.asTypeName().parameterizedBy(types.get(0), types.get(1))
            Log.w(TAG, "genNotifyWatchBlock " + eventType)
            block.add("if (%L.getSource() is %L \n" +
                "       && (%L.getNewValue() == null || %L.getNewValue() is %L) \n" +
                "       && (%S.equals(%L) || this.%L.equals(%L)) && %S.equals(%L.%L())) {\n" +
                "   var notifyEvent: %T\n" +
                "   if (%S.equals(%L)) {\n" +
                "       notifyEvent = this.%L(%L as %T);\n" +
                "   } else {\n" +
                "       notifyEvent = %L as %T;\n" +
                "   }\n" +
                "   if (KvoWatch.Thread.%L == %T.Thread.MAIN) {\n" +
                "       %T.getInstance().mainThread(Runnable {\n" +
                "               %L.%L(notifyEvent);\n" +
                "         });\n" +
                "   } else if (KvoWatch.Thread.%L == %T.Thread.WORK) {\n" +
                "       %T.getInstance().workThread(Runnable {\n" +
                "               %L.%L(notifyEvent);\n" +
                "       });\n" +
                "   } else {\n" +
                "       %L.%L(notifyEvent);\n" +
                "   }\n" +
                "}\n", NOTIFY_WATCHER_EVENT, types.get(0), NOTIFY_WATCHER_EVENT, NOTIFY_WATCHER_EVENT, types.get(1),
                IKvoTargetProxy.INIT_METHOD_NAME, NOTIFY_WATCHER_NAME, getName, NOTIFY_WATCHER_NAME, w.tag,
                NOTIFY_WATCHER_EVENT, EVENT_GET_TAG, eventType,
                IKvoTargetProxy.INIT_METHOD_NAME, NOTIFY_WATCHER_NAME, initName, NOTIFY_WATCHER_EVENT, eventType,
                NOTIFY_WATCHER_EVENT, eventType,
                thread, kvoWatchName, kvoThreadName, target, methodName, thread,
                kvoWatchName, kvoThreadName, target, methodName, target, methodName)
        }
        return block.build()
    }

    private fun genWatchNameMethods(info: KvoTargetInfo): Set<FunSpec> {
        val methods = info.methods
        val ms = HashSet<FunSpec>(methods.size)
        for (e in methods) {
            val name = e.simpleName.toString()
            val m = FunSpec.builder(GET_NAME_METHOD_PREFIX + name)
                .returns(String::class)
                .addKdoc("the value is intermediate product, will be change finally, don't care about it.\n")
                .addModifiers(KModifier.PRIVATE, KModifier.FINAL)
                .addCode("return %S;\n", name)
                .build()
            ms.add(m)
        }
        return ms
    }

    private fun genInitValueMethods(info: KvoTargetInfo): Set<FunSpec> {
        val methods = info.methods
        val ms = HashSet<FunSpec>(methods.size)
        for (e in methods) {
            val name = e.simpleName.toString()
            val ps = e.parameters
            val param = ps[0]
            val types = getTypes(param)
            val annotation = AnnotationSpec.builder(KvoAssist::class.asTypeName())
                .addMember("name = %S", types.get(0))
                .build()
            val eventType = KvoEvent::class.asTypeName().parameterizedBy(types.get(0), types.get(1))
            val m = FunSpec.builder(INIT_VALUE_METHOD_PREFIX + name)
                .returns(eventType)
                .addKdoc("the value is intermediate product, will be change finally, don't care about it.\n")
                .addModifiers(KModifier.PRIVATE, KModifier.FINAL)
                .addAnnotation(annotation)
                .addParameter(NOTIFY_WATCHER_EVENT, eventType)
                .addCode("return KvoEvent.newEvent(%L.getSource(), null, null, %L.getTag());\n", NOTIFY_WATCHER_EVENT,
                    NOTIFY_WATCHER_EVENT)
                .build()
            ms.add(m)
        }
        return ms
    }

    private fun getTypes(type: VariableElement): List<ClassName> {

        val typeList = getTypeArguments(type.asType())
        if (typeList != null && typeList.isNotEmpty()) {
            val types = ArrayList<ClassName>(typeList.size)
            for (t in typeList) {
                types.add(javaToKotlinType(typeNameWithoutTypeArguments(t.toString())))
            }
            return types
        }
        return ArrayList()
    }

    private fun getTypeArguments(targetType: Any): List<TypeMirror>? {
        val cls = targetType.javaClass
        try {
            for (m in cls.methods) {
                if ("getTypeArguments" == m.name) {
                    val obj = m.invoke(targetType)
                    return obj as List<TypeMirror>
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}