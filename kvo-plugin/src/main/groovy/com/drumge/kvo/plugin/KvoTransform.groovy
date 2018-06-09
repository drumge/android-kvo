package com.drumge.kvo.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.drumge.easy.plugin.utils.EasyUtils
import com.drumge.easy.plugin.utils.TypeUtils
import com.drumge.kvo.annotation.KvoAssist
import com.drumge.kvo.annotation.KvoBind
import com.drumge.kvo.annotation.KvoIgnore
import com.drumge.kvo.annotation.KvoSource
import com.drumge.kvo.annotation.KvoWatch
import com.drumge.easy.plugin.api.BaseEasyTransform
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.bytecode.AccessFlag
import org.gradle.api.Project

class KvoTransform extends BaseEasyTransform {
    private static final String TAG = "KvoTransform"

    private static final String KVO_API_ARTIFACT = 'com.github.drumge:kvo-api'
    private static final String KVO_ANNOTATION_ARTIFACT = 'com.github.drumge:kvo-annotation'

    private static final String SOURCE_CLASS_SUFFIX = '$$KvoSource.class'
    private static final String WATCH_CLASS_SUFFIX = '$$KvoTargetProxy.class'
    private static final String CREATOR_CLASS_SUFFIX = '$$KvoTargetCreator'
    private static final String SET_METHOD_PREFIX = "set"
    private static final String GET_NAME_METHOD_PREFIX = "kw_"
    private static final String INIT_VALUE_METHOD_PREFIX = "initValue_"
    private static final String KVO_CLASS = "com.drumge.kvo.api.Kvo"
    private static final String I_SOURCE_CLASS = "com.drumge.kvo.api.inner.IKvoSource"
    private static final String KVO_EVENT_CLASS = "com.drumge.kvo.api.KvoEvent"
    private static final String KVO = "${KVO_CLASS}.getInstance()"
    private static final String KVO_SOURCE_TAG_FIELD = "_kvoSourceTag"
    private static final String KVO_SOURCE_TAG_SET_METHOD = "_setKvoSourceTag"
    private static final String KVO_SOURCE_TAG_GET_METHOD = "_getKvoSourceTag"
    private static final String KVO_SOURCE_GET_METHOD_PREFIX = "_get_"

    private Project mProject
    private List<String> mAllHandleUnzipJar = new ArrayList<>()
    private int mRegisterCount = 0

    KvoTransform(Project project) {
        super(project)
        mProject = project
    }

    @Override
    boolean isNeedUnzipJar(JarInput jarInput, File outputFile) {
        String name = jarInput.name
//        Log.i(TAG, "isNeedUnzipJar name: %s", name)
        if (name.contains(KVO_API_ARTIFACT) || name.contains(KVO_ANNOTATION_ARTIFACT)) {
            appendClassPath(jarInput.file.absolutePath)
            return false
        }
        return isTargetJar(jarInput)
    }

    @Override
    boolean onUnzipJarFile(JarInput jarInput, String unzipPath, File outputFile) {
        String name = jarInput.name
//        Log.d(TAG, "onUnzipJarFile name: %s,  unzipPath: %s", name, unzipPath)
        appendClassPath(unzipPath)
        if (name.contains(KVO_API_ARTIFACT)) {
            return true
        } else if (isTargetJar(jarInput)) {
            mAllHandleUnzipJar.add(unzipPath)
        }
        return false
    }

    @Override
    void onAfterJar() {
        super.onAfterJar()
        Log.d(TAG, "onAfterJar ")

        mAllHandleUnzipJar.findAll { String unzipJar ->
            return handleSource(unzipJar)
        }.each { String unzipJar ->
            support.rezipUnzipJarFile(unzipJar)
        }

        mAllHandleUnzipJar.findAll { String unzipJar ->
            return handleKvo(unzipJar)
        }.each { String unzipJar ->
            support.rezipUnzipJarFile(unzipJar)
        }
    }

    @Override
    void onEachDirectoryOutput(DirectoryInput directoryInput, File outputs) {
        super.onEachDirectoryOutput(directoryInput, outputs)
        Map<File, Status> change =  directoryInput.getChangedFiles()
        Log.d(TAG, "onEachDirectoryOutput change: %s", change)

        String outPath = outputs.absolutePath + File.separator
        appendClassPath(outPath)
        handleSource(outPath)
        handleKvo(outPath)
    }

    @Override
    void onAfterTransform() {
        super.onAfterTransform()
    }

    @Override
    void onFinally() {
        super.onFinally()
    }

    /**
     * 处理注入代码入口，先处理 source
     * @param outPath
     * @return true -- 有注入代码，需要压缩回jar
     */
    private boolean handleSource(String outPath) {
        boolean inject = false
        mProject.fileTree(outPath).findAll { File file ->
            String path = file.absolutePath
            return path.endsWith(SOURCE_CLASS_SUFFIX)
        }.each { File file ->
            inject = true
            String path = file.absolutePath
            String proxyName = path.replace(outPath, '').replaceAll(EasyUtils.regSeparator(), '.')
            String kvoSourceName = proxyName.replace(SOURCE_CLASS_SUFFIX, '')
            processSource(kvoSourceName, outPath)
            mProject.delete(file)
        }
        return inject
    }

    /**
     * 处理注入代码入口，包括directory和jar
     * @param outPath
     * @return true -- 有注入代码，需要压缩回jar
     */
    private boolean handleKvo(String outPath) {
        boolean inject = false
        mProject.fileTree(outPath).findAll { File file ->
            String path = file.absolutePath
            return path.endsWith(WATCH_CLASS_SUFFIX)
        }.each { File file ->
            inject = true
            String path = file.absolutePath
            String proxyName = path.replace(outPath, '').replaceAll(EasyUtils.regSeparator(), '.')
            String targetName = proxyName.replace(WATCH_CLASS_SUFFIX, '')
            String proxy = proxyName.replace('.class', '')
            processWatch(proxy, targetName, outPath)
            injectRegister(targetName, outPath)
        }
        return inject
    }

    /**
     * 处理被观察的对象，往set方法插入通知观察者代码，插入设置tag代码
     * @param className
     * @param output
     */
    private void processSource(String className, String output) {
        CtClass source = pool.getCtClass(className)
        if (source.isFrozen()) {
            source.defrost()
        }
        Set<SourceBindInfo> bindMethods = parseBindMethod(source)

        bindMethods.each { SourceBindInfo info ->
            String typeName = info.field.type.name
            String fieldName = info.field.name
            String ov = "oldValue"
            String nv = "newValue"
            boolean unbox = TypeUtils.isUnbox(typeName)
            StringBuilder sb = new StringBuilder()
            if (unbox) {
                String type = TypeUtils.box(typeName)
                sb.append("${type} ${ov} = ${type}.valueOf(\$0.${fieldName});\n")
                sb.append("${type} ${nv} = ${type}.valueOf(\$1);\n")
            } else {
                sb.append("${typeName} ${ov} = \$0.${fieldName};\n")
                sb.append("${typeName} ${nv} = \$1;\n")
            }
            sb.append("\$0.${fieldName} = \$1;")
            sb.append("${KVO}.notifyWatcher(\$0, \"${info.bindName}\", ${ov}, ${nv});\n")

            info.bindMethod.insertBefore(sb.toString())
            genFieldGetMethod(source, info)
        }
        genKvoSourceTag(source)

        source.writeFile(output)
    }

    /**
     * 处理观察者所在的对象，生成 $$KvoTargetProxy 辅助类
     * @param proxyName
     * @param className
     * @param outPath
     */
    private void processWatch(String proxyName, String className, String outPath) {
        CtClass proxy = pool.getCtClass(proxyName)
        if (proxy.isFrozen()) {
            proxy.defrost()
        }
        CtClass watch = pool.getCtClass(className)
        watch.declaredMethods.findAll { CtMethod m ->
            return m.hasAnnotation(KvoWatch.class)
        }.each { CtMethod m ->
            String name = m.name
            KvoWatch w = m.getAnnotation(KvoWatch.class)
            String bindName = w.name()
            CtMethod getName = proxy.getDeclaredMethod(GET_NAME_METHOD_PREFIX + name)
            getName.setBody("return \"${bindName}\";")

            CtMethod initValue = proxy.getDeclaredMethod(INIT_VALUE_METHOD_PREFIX + name)
            KvoAssist assist = initValue.getAnnotation(KvoAssist.class)
            String sourceName = assist.name()
            String getFieldName  = "((${sourceName})\$1.getSource()).${KVO_SOURCE_GET_METHOD_PREFIX}${bindName}()"
            initValue.setBody("return ${KVO_EVENT_CLASS}.newEvent(\$1.getSource(), ${getFieldName}, ${getFieldName}, \$1.getTag());")
        }
        proxy.writeFile(outPath)
    }

    private void injectRegister(String targetName, String outPath) {
        String creatorName = targetName + CREATOR_CLASS_SUFFIX
        CtClass targetCls = pool.get(targetName)
        if (targetCls.isFrozen()) {
            targetCls.defrost()
        }
        targetCls.addInterface()
        CtField register = CtField.make("private static int register${mRegisterCount++} = ${creatorName}#registerCreator();", targetCls)
        targetCls.addField(register)
        targetCls.writeFile(outPath)
    }

    private void genFieldGetMethod(CtClass source, SourceBindInfo info) {
        String methodName = KVO_SOURCE_GET_METHOD_PREFIX + info.bindName
        StringBuilder sb = new StringBuilder()
        String typeName = info.field.type.name
        boolean unbox = TypeUtils.isUnbox(typeName)
        if (unbox) {
            String type = TypeUtils.box(typeName)
            sb.append("public ${type} ${methodName}(){\n")
            sb.append("return ${type}.valueOf(\$0.${info.field.name});\n")
        } else {
            sb.append("public ${info.field.type.name} ${methodName}(){\n")
            sb.append("return \$0.${info.field.name};\n")
        }
        sb.append("}\n")

        CtMethod method = CtMethod.make(sb.toString(), source)
        source.addMethod(method)
    }

    private void genKvoSourceTag(CtClass source) {
        CtClass iSource = pool.get(I_SOURCE_CLASS)
        source.addInterface(iSource)
        CtField field = CtField.make("private String ${KVO_SOURCE_TAG_FIELD} = \"\";", source)
        source.addField(field)
        CtMethod set = CtMethod.make("final public void ${KVO_SOURCE_TAG_SET_METHOD}(String tag) {\n" +
                "   tag = tag == null ? \"\" : tag;" +
                "   \$0.${KVO_SOURCE_TAG_FIELD} = \$1;\n" +
                "}", source)
        source.addMethod(set)
        CtMethod get = CtMethod.make("final public String ${KVO_SOURCE_TAG_GET_METHOD}() {\n" +
                "   return \$0.${KVO_SOURCE_TAG_FIELD};\n" +
                "}", source)
        source.addMethod(get)
    }

    /**
     * 解析 @KvoSource 注解类中的绑定被监听属性的set方法
     * @param source
     * @return
     */
    private Set<SourceBindInfo> parseBindMethod(CtClass source) {
        Set<SourceBindInfo> bindMethods = new HashSet<>()
        source.declaredMethods.findAll { CtMethod method ->
            return method.hasAnnotation(KvoBind.class)
        }.each { CtMethod method ->
            KvoBind bind = method.getAnnotation(KvoBind.class)
            String name = bind.name()
            CtField field
            if ((field = checkBindMethod(source, method, name)) != null) {
                addBindMethod(bindMethods, field, name, method)
            }
        }
        KvoSource annotation = source.getAnnotation(KvoSource.class)
        boolean check = annotation.check()

        source.declaredFields.findAll { CtField field ->
            String fieldName = field.name
            return !field.hasAnnotation(KvoIgnore.class) && !containBindMethod(bindMethods, fieldName)
        }.each { CtField field ->
            if ((field.modifiers & AccessFlag.FINAL) == 0) {
                if (check && AccessFlag.isPublic(field.modifiers)) {
                    String msg = "${className}#${field.name} is illegal, it may need to be private, or you may add @KvoIgnore to the field, or add @KvoSource(check = false) to the class"
                    throw new RuntimeException(msg)
                }
                CtMethod method = checkDefaultMethod(source, field, check)
                if (method != null) {
                    addBindMethod(bindMethods, field, field.name, method)
                }
            }
        }
        return bindMethods
    }

    private void addBindMethod(Set<SourceBindInfo> bindMethods, CtField field, String bindName, CtMethod method) {
        if (containBindMethod(bindMethods, field.name)) {
            String msg = "#${field.name} is bind with more than one method"
            throw new RuntimeException(msg)
        }
        SourceBindInfo info = new SourceBindInfo(field, bindName, method)
        bindMethods.add(info)
    }

    private boolean containBindMethod(Set<SourceBindInfo> bindMethods, String fieldName) {
        for (SourceBindInfo info  : bindMethods) {
            if (info.field.name == fieldName) {
                return true
            }
        }
        return false
    }

    /**
     * 检查默认set+属性名字（属性名字首字母大写）的set方法检查
     * @param source
     * @param field
     * @return
     */
    private CtMethod checkDefaultMethod(CtClass source, CtField field, boolean check) {
        String setMethodName = "${SET_METHOD_PREFIX}${EasyUtils.upperFirstCase(field.name)}"
        for (CtMethod m : source.declaredMethods) {
            CtClass[] params = m.parameterTypes
            if (setMethodName == m.name && params.size() == 1) {
                if (field.type in params[0]) {
                    return m
                } else if (check) {
                    String msg = "${className}#${setMethodName} with ${params[0].name} can not be ${field.type.name}"
                    throw new RuntimeException(msg)
                }
            }
        }
        if (check) {
            String msg = "${source.name} can not found set method bind with field #${field.name}"
            throw new RuntimeException(msg)
        }
        return null
    }

    /**
     * 检查方法带有 @KvoBind 注解的方法
     * @param source
     * @param field
     * @return
     */
    private CtField checkBindMethod(CtClass source, CtMethod method, String name) {
        for (CtField field : source.declaredFields) {
            if (name != field.name) {
                continue
            }
            if (checkFieldMethod(method, field)) {
                return field
            } else {
                throw new RuntimeException("${source.name}#${method.name} with @KvoBind(name = \"${name}\") can not bind field #${name}, maybe illegal params")
            }
        }
        throw new RuntimeException("${source.name}#${method.name} with @KvoBind(name = \"${name}\") can not founded field #${name}")
    }

    private boolean checkFieldMethod(CtMethod method, CtField field) {
        CtClass[] params = method.parameterTypes
        if (params.size() == 1 && field.type in params[0]) {
            return true
        }
        return false
    }

    private boolean isTargetJar(JarInput jarInput) {
        isLocalProject(jarInput)
    }

    private boolean isLocalProject(JarInput jarInput) {
        return jarInput.scopes.contains(QualifiedContent.Scope.SUB_PROJECTS) || jarInput.scopes.contains(QualifiedContent.Scope.PROJECT)
    }

    private static String format(String msg, Object... format) {
        return String.format(Locale.getDefault(), msg, format)
    }

    private static class Log {
        static void d(String tag, String msg, Object... p) {
            System.out.println(format("debug: [%s]	%s", tag, format(msg, p)))
        }

        static void i(String tag, String msg, Object... p) {
            System.out.println(format("info: [%s]	%s", tag, format(msg, p)))
        }

        static void w(String tag, String msg, Object... p) {
            System.out.println(format("warm: [%s]	%s", tag, format(msg, p)))
        }

        static void e(String tag, String msg, Object... p) {
            System.err.println(format("error: [%s]\t%s", tag, format(msg, p)))
        }
    }
}