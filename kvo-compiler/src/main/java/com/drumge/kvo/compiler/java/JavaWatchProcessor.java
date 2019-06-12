package com.drumge.kvo.compiler.java;

import com.drumge.kvo.annotation.KvoAssist;
import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.KvoEvent;
import com.drumge.kvo.compiler.KvoTargetInfo;
import com.drumge.kvo.inner.IKvoTargetCreator;
import com.drumge.kvo.inner.IKvoTargetProxy;
import com.drumge.kvo.inner.log.KLog;
import com.drumge.kvo.inner.thread.KvoThread;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static com.drumge.kvo.compiler.ConstantKt.CREATOR_CLASS_SUFFIX;
import static com.drumge.kvo.compiler.ConstantKt.EQUALS_TARGET_METHOD;
import static com.drumge.kvo.compiler.ConstantKt.EVENT_GET_TAG;
import static com.drumge.kvo.compiler.ConstantKt.GET_NAME_METHOD_PREFIX;
import static com.drumge.kvo.compiler.ConstantKt.INIT_VALUE_METHOD_PREFIX;
import static com.drumge.kvo.compiler.ConstantKt.IS_TARGET_VALID_METHOD;
import static com.drumge.kvo.compiler.ConstantKt.JAVA_DOC;
import static com.drumge.kvo.compiler.ConstantKt.KVO_PROXY_CREATOR_INSTANCE;
import static com.drumge.kvo.compiler.ConstantKt.NOTIFY_WATCHER_EVENT;
import static com.drumge.kvo.compiler.ConstantKt.NOTIFY_WATCHER_NAME;
import static com.drumge.kvo.compiler.ConstantKt.PROXY_CLASS_SUFFIX;
import static com.drumge.kvo.compiler.ConstantKt.TARGET_CLASS_FIELD;
import static com.drumge.kvo.compiler.KvoCompilerUtilsKt.typeNameWithoutTypeArguments;

/**
 * Created by chenrenzhan on 2019/5/20.
 */
public class JavaWatchProcessor {
    private ProcessingEnvironment processingEnv;

    public JavaWatchProcessor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void genTargetClass(KvoTargetInfo info) {
        TypeName targetType = TypeName.get(info.target.asType());
        ParameterizedTypeName weakTargetType = ParameterizedTypeName.get(ClassName.get(WeakReference.class), targetType);
        String targetClassName = info.simpleName + PROXY_CLASS_SUFFIX;
        TypeSpec.Builder builder= TypeSpec.classBuilder(targetClassName)
                .addJavadoc(JAVA_DOC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(IKvoTargetProxy.class), targetType));

        if (info.target instanceof TypeElement) {
            TypeElement te = (TypeElement) info.target;
            for (TypeParameterElement tpe : te.getTypeParameters()) {
                builder.addTypeVariable(TypeVariableName.get(tpe));
            }
        }

        FieldSpec fTarget = FieldSpec.builder(weakTargetType, TARGET_CLASS_FIELD, Modifier.FINAL, Modifier.PRIVATE).build();

        String target = "target";
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(targetType, target, Modifier.FINAL)
                .addCode("this.$L = new $T($L);\n", TARGET_CLASS_FIELD, weakTargetType, target)
                .build();

        String p = "obj";
        MethodSpec equals = MethodSpec.methodBuilder("equals")
                .returns(TypeName.BOOLEAN)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.OBJECT, p)
                .addCode("if (this == $L) {\n" +
                        "   return true;\n" +
                        "} else if ($L instanceof $L) {\n" +
                        "   return this.$L.get() == (($L) $L).$L.get();\n" +
                        "}\n" +
                        "return false;\n", p, p, targetClassName, TARGET_CLASS_FIELD, targetClassName, p, TARGET_CLASS_FIELD)
                .build();

        MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addCode("if ($L.get() != null) {\n" +
                        "   return $L.get().hashCode();\n" +
                        "} \n" +
                        "return super.hashCode();\n", TARGET_CLASS_FIELD, TARGET_CLASS_FIELD)
                .build();

        MethodSpec notify = MethodSpec.methodBuilder("notifyWatcher")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, NOTIFY_WATCHER_NAME, Modifier.FINAL)
                .addParameter(KvoEvent.class, NOTIFY_WATCHER_EVENT, Modifier.FINAL)
                .addCode(genNotifyWatchBlock(info))
                .build();

        builder.addField(fTarget)
                .addMethods(genWatchNameMethods(info))
                .addMethods(genInitValueMethods(info))
                .addMethod(constructor)
                .addMethod(notify)
                .addMethod(equals)
                .addMethod(genEqualsTargetMethod(targetType))
                .addMethod(genIsTargetValidMethod())
                .addMethod(hashCode);

        TypeSpec proxy = builder.build();
        try {
            JavaFile clsJavaFile = JavaFile.builder(info.packageName, proxy)
                    .addStaticImport(KvoWatch.Thread.class, "*")
                    .build();
            clsJavaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void genCreatorClass(KvoTargetInfo info) {
        String creatorClassName = info.simpleName + CREATOR_CLASS_SUFFIX;
        String proxyClassName = info.simpleName + PROXY_CLASS_SUFFIX;
        TypeName targetType = TypeName.get(info.target.asType());
        TypeName proxyType = ClassName.get(info.packageName, proxyClassName);

        TypeSpec.Builder builder= TypeSpec.classBuilder(creatorClassName)
                .addJavadoc(JAVA_DOC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(IKvoTargetCreator.class), proxyType, targetType));

        if (info.target instanceof TypeElement) {
            TypeElement te = (TypeElement) info.target;
            for (TypeParameterElement tpe : te.getTypeParameters()) {
                builder.addTypeVariable(TypeVariableName.get(tpe));
            }
        }

        MethodSpec creatorMethod = MethodSpec.methodBuilder("createTarget")
                .addParameter(targetType, "target")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(proxyType)
                .addCode("return new $T(target);\n", proxyType)
                .build();

        ClassName creatorType = ClassName.get(info.packageName, creatorClassName);
        MethodSpec newCreator = MethodSpec.methodBuilder("registerCreator")
                .returns(int.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addCode("$L.registerTarget($L.class, new $T());\n", KVO_PROXY_CREATOR_INSTANCE, info.target.toString(), creatorType)
                .addCode("return 0;\n", creatorType)
                .build();


        builder.addMethod(creatorMethod)
                .addMethod(newCreator);

        try {
            JavaFile clsJavaFile = JavaFile.builder(info.packageName, builder.build())
                    .build();
            clsJavaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MethodSpec genEqualsTargetMethod(TypeName targetType) {
        MethodSpec method = MethodSpec.methodBuilder(EQUALS_TARGET_METHOD)
                .returns(boolean.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.get(Object.class), TARGET_CLASS_FIELD, Modifier.FINAL)
                .addCode("if ($L instanceof $L) { return this.$L.get() == $L;} return false;",
                        TARGET_CLASS_FIELD, typeNameWithoutTypeArguments(targetType), TARGET_CLASS_FIELD, TARGET_CLASS_FIELD)
                .build();
        return method;
    }

    private MethodSpec genIsTargetValidMethod() {
        MethodSpec method = MethodSpec.methodBuilder(IS_TARGET_VALID_METHOD)
                .returns(boolean.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode("return this.$L.get() != null;\n",
                        TARGET_CLASS_FIELD)
                .build();
        return method;
    }

    private CodeBlock genNotifyWatchBlock(KvoTargetInfo info) {
        String target = "target";
        TypeName targetType = TypeName.get(info.target.asType());
        TypeName kLog = TypeName.get(KLog.class);
        Set<ExecutableElement> methods = info.methods;
        CodeBlock.Builder block = CodeBlock.builder();
        block.add("final $L $L = $L.get();\n" +
                "      if ($L == null) {\n" +
                "          $T.error($S, \"notifyWatcher target object is null, name: %s\", name);\n" +
                "          return;\n" +
                "      }\n", targetType, target, TARGET_CLASS_FIELD, target, kLog, info.target.getSimpleName());
        for (ExecutableElement e : methods) {
            String getName = GET_NAME_METHOD_PREFIX + e.getSimpleName().toString() + "()";
            String initName = INIT_VALUE_METHOD_PREFIX + e.getSimpleName().toString();
            KvoWatch w = e.getAnnotation(KvoWatch.class);
            KvoWatch.Thread thread = w.thread();
            ClassName kvoWatchName = ClassName.get(KvoWatch.class);
            ClassName kvoThreadName = ClassName.get(KvoThread.class);
            String methodName = e.getSimpleName().toString();
            List<? extends VariableElement> ps = e.getParameters();
            VariableElement param = ps.get(0);
            List<String> types = getTypes(param);
            block.add("if ($L.getSource() instanceof $L \n" +
                            "       && ($L.getNewValue() == null || $L.getNewValue() instanceof $L) \n" +
                            "       && ($S.equals($L) || this.$L.equals($L)) && $S.equals($L.$L())) {\n" +
                            "   final KvoEvent[] events = new KvoEvent[1];\n" +
                            "   if ($S.equals($L)) {\n" +
                            "       events[0] = this.$L($L);\n" +
                            "   } else {\n" +
                            "       events[0] = $L;\n" +
                            "   }\n" +
                            "   if ($L == $T.Thread.MAIN) {\n" +
                            "       $T.getInstance().mainThread(new Runnable() {\n" +
                            "           @Override\n" +
                            "           public void run() {\n" +
                            "               $L.$L(events[0]);\n" +
                            "           }\n" +
                            "         });\n" +
                            "   } else if ($L == $T.Thread.WORK) {\n" +
                            "       $T.getInstance().workThread(new Runnable() {\n" +
                            "           @Override\n" +
                            "           public void run() {\n" +
                            "               $L.$L(events[0]);\n" +
                            "           }\n" +
                            "       });\n" +
                            "   } else {\n" +
                            "       $L.$L(events[0]);\n" +
                            "   }\n" +
                            "}\n", NOTIFY_WATCHER_EVENT, types.get(0), NOTIFY_WATCHER_EVENT, NOTIFY_WATCHER_EVENT, types.get(1),
                    IKvoTargetProxy.INIT_METHOD_NAME, NOTIFY_WATCHER_NAME, getName, NOTIFY_WATCHER_NAME, w.tag(), NOTIFY_WATCHER_EVENT,
                    EVENT_GET_TAG, IKvoTargetProxy.INIT_METHOD_NAME, NOTIFY_WATCHER_NAME, initName, NOTIFY_WATCHER_EVENT, NOTIFY_WATCHER_EVENT,
                    thread, kvoWatchName, kvoThreadName, target, methodName, thread,
                    kvoWatchName, kvoThreadName, target, methodName, target, methodName);
        }
        return block.build();
    }

    private Set<MethodSpec> genWatchNameMethods(KvoTargetInfo info) {
        Set<ExecutableElement> methods = info.methods;
        Set<MethodSpec> ms = new HashSet<>(methods.size());
        for (ExecutableElement e : methods) {
            String name =  e.getSimpleName().toString();
            MethodSpec m = MethodSpec.methodBuilder(GET_NAME_METHOD_PREFIX + name)
                    .returns(String.class)
                    .addJavadoc("the value is intermediate product, will be change finally, don't care about it.\n")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .addCode("return $S;\n", name)
                    .build();
            ms.add(m);
        }
        return ms;
    }

    private Set<MethodSpec> genInitValueMethods(KvoTargetInfo info) {
        Set<ExecutableElement> methods = info.methods;
        Set<MethodSpec> ms = new HashSet<>(methods.size());
        for (ExecutableElement e : methods) {
            String name =  e.getSimpleName().toString();
            List<? extends VariableElement> ps = e.getParameters();
            VariableElement param = ps.get(0);
            List<String> types = getTypes(param);
            AnnotationSpec annotation = AnnotationSpec.builder(KvoAssist.class)
                    .addMember("name", "$S", types.get(0))
                    .build();
            MethodSpec m = MethodSpec.methodBuilder(INIT_VALUE_METHOD_PREFIX + name)
                    .returns(KvoEvent.class)
                    .addJavadoc("the value is intermediate product, will be change finally, don't care about it.\n")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .addAnnotation(annotation)
                    .addParameter(KvoEvent.class, NOTIFY_WATCHER_EVENT)
                    .addCode("return KvoEvent.newEvent($L.getSource(), null, null, $L.getTag());\n", NOTIFY_WATCHER_EVENT, NOTIFY_WATCHER_EVENT)
                    .build();
            ms.add(m);
        }
        return ms;
    }

    public List<String> getTypes(VariableElement var) {
        List<? extends TypeMirror> typeList = getTypeArguments(var.asType());
        if (typeList != null && !typeList.isEmpty()) {
            List<String> types = new ArrayList<>(typeList.size());
            for (TypeMirror type : typeList) {
                types.add(typeNameWithoutTypeArguments(type.toString()));
            }
            return types;
        }
        return Collections.EMPTY_LIST;
    }

    private List<? extends TypeMirror> getTypeArguments(Object targetType) {
        Class cls = targetType.getClass();
        try {
            for (Method m : cls.getMethods()) {
                if ("getTypeArguments".equals(m.getName())) {
                    Object obj = m.invoke(targetType);
                    return (List) obj;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
