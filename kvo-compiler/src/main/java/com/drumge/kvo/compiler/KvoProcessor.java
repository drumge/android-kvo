package com.drumge.kvo.compiler;

import com.drumge.kvo.annotation.KvoAssist;
import com.drumge.kvo.annotation.KvoIgnore;
import com.drumge.kvo.annotation.KvoSource;
import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.api.KvoEvent;
import com.drumge.kvo.inner.IKvoTargetCreator;
import com.drumge.kvo.inner.IKvoTargetProxy;
import com.drumge.kvo.inner.thread.KvoThread;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Created by chenrenzhan on 2018/4/29.
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class KvoProcessor extends AbstractProcessor {
    private static final String TAG = "KvoProcessor";
    private static final String JAVA_DOC = "Automatically generated file. DO NOT MODIFY.\n";
    private static final String KVO_PROXY_CREATOR_INSTANCE = "com.drumge.kvo.inner.KvoTargetProxyCreator.getInstance()";
    private static final String KVO_EVENT_NAME = KvoEvent.class.getName();
    private static final String SOURCE_FILED_CLASS_PREFIX = "K_";
    private static final String GET_NAME_METHOD_PREFIX = "kw_";
    private static final String INIT_VALUE_METHOD_PREFIX = "initValue_";
    private static final String SOURCE_CLASS_SUFFIX = "$$KvoSource";
    private static final String PROXY_CLASS_SUFFIX = "$$KvoTargetProxy";
    private static final String CREATOR_CLASS_SUFFIX = "$$KvoTargetCreator";
    private static final String TARGET_CLASS_FIELD = "target";
    private static final String NOTIFY_WATCHER_NAME = "name";
    private static final String NOTIFY_WATCHER_EVENT = "event";
    private static final String EVENT_GET_TAG = "getTag";
    private static final String EQUALS_TARGET_METHOD = "equalsTarget";

    private static final String REG_KVO_EVENT_PARAM = ".*?KvoEvent[<](.+?),(.+?)[<>].*";


    private final Map<String, String> mBindFields = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        Log.init(processingEnv.getMessager());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotation = new LinkedHashSet<>();
        annotation.add(KvoSource.class.getCanonicalName());
        annotation.add(KvoWatch.class.getCanonicalName());
        Log.i(TAG, "getSupportedAnnotationTypes: %s", annotation);
        return annotation;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Log.w(TAG, "kvo processor process begin");
        processKvo(set, roundEnvironment);
        Log.w(TAG, "kvo processor process end");
        return true;
    }

    private void processKvo(Set<? extends TypeElement> set, RoundEnvironment env) {
        processSource(env);
        processWatch(env);
    }

    /**
     * 生成观察者所在类的辅助类 $$KvoTargetProxy
     * @param env
     */
    private void processWatch(RoundEnvironment env) {
        Set<? extends Element> elements = env.getElementsAnnotatedWith(KvoWatch.class);
        Set<KvoTargetInfo> targets = new HashSet<>();
        for (Element eMethod : elements) {
            ExecutableElement em = (ExecutableElement) eMethod;
//            Log.i(TAG, "processWatch %s, parent: %s", em, em.getEnclosingElement());
            List<? extends VariableElement> ps = em.getParameters();
            VariableElement param;
            if (ps.size() != 1 || !(param = ps.get(0)).asType().toString().startsWith(KVO_EVENT_NAME)
                    || getTypes(param).length != 2) {
                Log.e(TAG, "%s#%s is illegal, the parameters must be size: %s, @KvoWatch can have only one parameter KvoEvent<S, V>, and must have assign the type of <S, V>", em.getEnclosingElement(), em, em.getParameters().size());
            }
            Element ce = em.getEnclosingElement();
            String cls = ce.toString();
            String simpleName = ce.getSimpleName().toString();
            String packageName = cls.replace("." + simpleName, "");
            KvoTargetInfo info = findTarget(targets, packageName, simpleName);
            if (info == null) {
                info = new KvoTargetInfo(packageName, simpleName);
                targets.add(info);
            }
            info.setTarget(ce);
            info.addMethod(em);
        }
        for (KvoTargetInfo info : targets) {
//            Log.i(TAG, "processWatch info.simpleName: %s, info.target: %s", info.simpleName, info.target);
            genTargetClass(info);
            genCreatorClass(info);
        }
    }

    private KvoTargetInfo findTarget(Set<KvoTargetInfo> targets, String packageName, String simpleName) {
        for (KvoTargetInfo info : targets) {
            if (info.equals(packageName, simpleName)) {
                return info;
            }
        }
        return null;
    }

    private String[] getTypes(VariableElement var) {
        Pattern pattern = Pattern.compile(REG_KVO_EVENT_PARAM);
        String str = var.asType().toString();
        Matcher m = pattern.matcher(str);
        if (m.find() && m.groupCount() == 2) {
            return new String[]{m.group(1), m.group(2)};
        }
        return new String[0];
    }

    private void genTargetClass(KvoTargetInfo info) {
//        Log.i(TAG, "genTargetClass info.simpleName: %s, info.target: %s", info.simpleName, info.target);
        TypeName targetType = TypeName.get(info.target.asType());
        String targetClassName = info.simpleName + PROXY_CLASS_SUFFIX;
        TypeSpec.Builder builder= TypeSpec.classBuilder(targetClassName)
                .addJavadoc(JAVA_DOC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(IKvoTargetProxy.class), targetType));

        FieldSpec fTarget = FieldSpec.builder(targetType, TARGET_CLASS_FIELD, Modifier.FINAL, Modifier.PRIVATE).build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(targetType, TARGET_CLASS_FIELD, Modifier.FINAL)
                .addCode("this.$L = $L;\n", TARGET_CLASS_FIELD, TARGET_CLASS_FIELD)
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
                        "   return this.$L == (($L) $L).$L;\n" +
                        "}\n" +
                        "return false;\n", p, p, targetClassName, TARGET_CLASS_FIELD, targetClassName, p, TARGET_CLASS_FIELD)
                .build();

        MethodSpec hashCode = MethodSpec.methodBuilder("hashCode")
                .returns(TypeName.INT)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addCode("if ($L != null) {\n" +
                        "   return $L.hashCode();\n" +
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

    private MethodSpec genEqualsTargetMethod(TypeName targetType) {
        MethodSpec method = MethodSpec.methodBuilder(EQUALS_TARGET_METHOD)
                .returns(boolean.class)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(TypeName.get(Object.class), TARGET_CLASS_FIELD, Modifier.FINAL)
                .addCode("if ($L instanceof $T) { return this.$L == $L;} return false;",
                        TARGET_CLASS_FIELD, targetType, TARGET_CLASS_FIELD, TARGET_CLASS_FIELD)
                .build();
        return method;
    }

    private void genCreatorClass(KvoTargetInfo info) {
//        Log.i(TAG, "genCreatorClass info.simpleName: %s, info.target: %s", info.simpleName, info.target);
        String creatorClassName = info.simpleName + CREATOR_CLASS_SUFFIX;
        String proxyClassName = info.simpleName + PROXY_CLASS_SUFFIX;
        TypeName targetType = TypeName.get(info.target.asType());
        TypeName proxyType = ClassName.get(info.packageName, proxyClassName);

        TypeSpec.Builder builder= TypeSpec.classBuilder(creatorClassName)
                .addJavadoc(JAVA_DOC)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(IKvoTargetCreator.class), proxyType, targetType));

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
            String[] types = getTypes(param);
            AnnotationSpec annotation = AnnotationSpec.builder(KvoAssist.class)
                    .addMember("name", "$S", types[0])
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

    private CodeBlock genNotifyWatchBlock(KvoTargetInfo info) {
        Set<ExecutableElement> methods = info.methods;
        CodeBlock.Builder block = CodeBlock.builder();
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
            String[] types = getTypes(param);
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
                    "}\n", NOTIFY_WATCHER_EVENT, types[0], NOTIFY_WATCHER_EVENT, NOTIFY_WATCHER_EVENT, types[1],
                    IKvoTargetProxy.INIT_METHOD_NAME, NOTIFY_WATCHER_NAME, getName, NOTIFY_WATCHER_NAME, w.tag(), NOTIFY_WATCHER_EVENT,
                    EVENT_GET_TAG, IKvoTargetProxy.INIT_METHOD_NAME, NOTIFY_WATCHER_NAME, initName, NOTIFY_WATCHER_EVENT, NOTIFY_WATCHER_EVENT,
                    thread, kvoWatchName, kvoThreadName, TARGET_CLASS_FIELD, methodName, thread,
                    kvoWatchName, kvoThreadName, TARGET_CLASS_FIELD, methodName, TARGET_CLASS_FIELD, methodName);
        }
        return block.build();
    }


    /**
     * 检查 @KvoSource 注解修饰的被观察对象，生成对应属性的name
     * @param env
     */
    private void processSource(RoundEnvironment env) {
        Set<? extends Element> elements = env.getElementsAnnotatedWith(KvoSource.class);
        Map<String, KvoSourceInfo> allClass = new HashMap<>();
        for (Element eClass : elements) {
            if (!(eClass instanceof TypeElement)) {
                Log.i(TAG, "processSource is not TypeElement %s", eClass);
                continue;
            }
            TypeElement te = (TypeElement) eClass;
            TypeElement inner = null;
            String className = te.getQualifiedName().toString();
            Log.i(TAG, "processSource className: %s, %s", className, te.toString());
            if (te.getNestingKind() == NestingKind.MEMBER) { // 内部类
                String simpleName = te.getSimpleName().toString();
                className = className.substring(0, className.length() - simpleName.length() - 1);
                inner = te;
            }
            KvoSourceInfo sourceInfo = allClass.get(className);
            if (sourceInfo == null) {
                sourceInfo = new KvoSourceInfo();
                sourceInfo.className = className;
                allClass.put(className, sourceInfo);
            }
            if (inner == null) {
                sourceInfo.clsElement = te;
            } else {
                if (sourceInfo.innerCls == null) {
                    sourceInfo.innerCls = new ArrayList<>();
                }
                sourceInfo.innerCls.add(inner);
            }
        }

        for (KvoSourceInfo info : allClass.values()) {
            genKSource(info);
        }
    }

    private void genKSource(KvoSourceInfo info) {
        TypeSpec.Builder kvoSourceBuilder = null;
        String originalSimpleName = getSimpleName(info.className);
        String simpleName = SOURCE_FILED_CLASS_PREFIX + originalSimpleName;
        String pack = getPackage(info.className);
        if (info.clsElement != null) {
            List<FieldSpec> fields = getPrivateFields(info.clsElement);
            kvoSourceBuilder = TypeSpec.interfaceBuilder(simpleName)
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc(JAVA_DOC)
                    .addFields(fields);
            genTempClass(info.clsElement, SOURCE_CLASS_SUFFIX, null);
        }
        if (info.innerCls != null && !info.innerCls.isEmpty()) {
            List<TypeSpec> innerList = new ArrayList<>(info.innerCls.size());
            for (TypeElement te : info.innerCls) {
                List<FieldSpec> fields = getPrivateFields(te);
                TypeSpec typeSpec = TypeSpec.interfaceBuilder(te.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addFields(fields)
                        .addJavadoc(JAVA_DOC)
                        .build();
                innerList.add(typeSpec);
                genTempClass(pack, originalSimpleName + "$" + te.getSimpleName() + SOURCE_CLASS_SUFFIX, null);
            }
            if (kvoSourceBuilder == null) {
                kvoSourceBuilder = TypeSpec.interfaceBuilder(simpleName)
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc(JAVA_DOC);
            }
            kvoSourceBuilder.addTypes(innerList);
        }
        try {
            JavaFile clsJavaFile = JavaFile.builder(pack, kvoSourceBuilder.build()).build();
            clsJavaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<FieldSpec> getPrivateFields(TypeElement te) {
        KvoSource kvoSource = te.getAnnotation(KvoSource.class);
        boolean check = kvoSource.check();
        List<? extends Element> cElements = te.getEnclosedElements();
        List<FieldSpec> fields = new ArrayList<>();
        for (Element e : cElements) {
            if (e.getKind() != ElementKind.FIELD) {
                continue;
            }
            if (e.getModifiers().contains(Modifier.FINAL)) {
                continue;
            }
            if (check) {
                checkFieldLegal(te, e);
            }
            String fieldName;
            if (e.getModifiers().contains(Modifier.PRIVATE)) {
                String name = e.getSimpleName().toString();
                fieldName = name;
                fields.add(genField(fieldName, name));
            }
        }
        return fields;
    }

    private FieldSpec genField(String name, String value) {
        return FieldSpec.builder(String.class, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(format("\"%s\"", value)).build();
    }

    private void genTempClass(Element eClass, String suffix, Iterable<MethodSpec> methods) {
        String pack = getPackage(eClass.toString());
        String simpleName = eClass.getSimpleName().toString() + suffix;
        genTempClass(pack, simpleName, methods);
    }

    private void genTempClass(String pack, String simpleName, Iterable<MethodSpec> methods) {
        TypeSpec.Builder builder= TypeSpec.classBuilder(simpleName)
                .addJavadoc(JAVA_DOC + "\n just intermediate class, not been packaged in apk");
        if (methods != null) {
            builder.addMethods(methods);
        }
        try {
            JavaFile clsJavaFile = JavaFile.builder(pack, builder.build()).build();
            clsJavaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPackage(String name) {
        int idx = name.lastIndexOf(".");
        if (idx < 0) {
            return "";
        }
        return name.substring(0, idx);
    }

    private String getSimpleName(String name) {
        int idx = name.lastIndexOf(".");
        return name.substring(idx < 0 ? 0 : idx + 1, name.length());
    }

    private boolean checkFieldLegal(Element eClass, Element e) {
        if (e.getKind() == ElementKind.FIELD
                && e.getAnnotation(KvoIgnore.class) == null
                && !e.getModifiers().contains(Modifier.PRIVATE)) {
            Log.e(TAG, "%s#%s is illegal, it may need to be private, or you may add @KvoIgnore to the field, or add @KvoSource(check = false) to the class", eClass, e);
            return false;
        }
        return true;
    }

    private static class Log {
        private static Messager messager;
        static void init(Messager messager) {
            Log.messager = messager;
        }

        static void i(String tag, String msg, Object... format) {
            messager.printMessage(Diagnostic.Kind.NOTE, format("[%s]\t", tag) + format(msg, format));
        }

        static void w(String tag, String msg, Object... format) {
            messager.printMessage(Diagnostic.Kind.WARNING, format("[%s]\t", tag) + format(msg, format));
        }

        static void e(String tag, String msg, Object... format) {
            messager.printMessage(Diagnostic.Kind.ERROR, format("[%s]\t", tag) + format(msg, format));
        }
    }

    private static String format(String msg, Object... format) {
        return String.format(Locale.getDefault(), msg, format);
    }
}
