package com.drumge.kvo.plugin.api

import com.drumge.easy.plugin.utils.EasyUtils
import com.drumge.easy.plugin.utils.TypeUtils
import com.drumge.kvo.annotation.*
import javassist.*
import javassist.bytecode.AccessFlag
import org.gradle.api.Project

class KvoHandler {
    private static final String TAG = "KvoHandler"

    private static final String SOURCE_CLASS_SUFFIX = '_K_KvoSource.class'
    private static final String WATCH_CLASS_SUFFIX = '_K_KvoTargetProxy.class'
    private static final String CREATOR_CLASS_SUFFIX = '_K_KvoTargetCreator'
    private static final String SET_METHOD_PREFIX = "set"
    private static final String GET_NAME_METHOD_PREFIX = "kw_"
    private static final String INIT_VALUE_METHOD_PREFIX = "initValue_"
    private static final String I_SOURCE_CLASS = "com.drumge.kvo.inner.IKvoSource"
    private static final String I_TARGET_CLASS = "com.drumge.kvo.inner.IKvoTarget"
    private static final String KVO_EVENT_CLASS = "com.drumge.kvo.api.KvoEvent"
    private static final String KVO_IMP = "com.drumge.kvo.inner.KvoImp.getInstance()"
    private static final String KVO_SOURCE_TAG_FIELD = "_kvoSourceTagList"
    private static final String KVO_SOURCE_TAG_ADD_METHOD = "_addKvoSourceTag"
    private static final String KVO_SOURCE_TAG_REMOVE_METHOD = "_removeKvoSourceTag"
    private static final String KVO_SOURCE_TAG_REMOVE_ALL_METHOD = "_removeKvoSourceAllTag"
    private static final String KVO_SOURCE_TAG_CONTAIN_METHOD = "_containKvoSourceTag"
    private static final String KVO_SOURCE_GET_METHOD_PREFIX = "_get_"

    private Project mProject
    private int mRegisterCount = 0

    private ClassPool pool
    private final List<String> classPaths = new ArrayList<>()
    private final List<ClassPath> classPathList = new ArrayList<>()
    private final List<String> sourcePath = new ArrayList<>()

    public KvoHandler(Project project) {
        mProject = project
        pool = new ClassPool(true)
    }

    public KvoHandler(Project project, ClassPool pool) {
        mProject = project
        if (pool == null) {
            this.pool = new ClassPool(true)
        } else {
            this.pool = pool
        }
    }

    public void handleKvoSource(String source) {
        Log.i(TAG, "handle sourcePath: %s", source)
        if (!source.endsWith(File.separator)) {
            source = source + File.separator
        }
        appendClassPath(source)
        handleSource(source)
        sourcePath.add(source)
//        handleKvo(source)
    }

    void onAfterDirectory() {
        sourcePath.each {
            handleKvo(it)
        }
    }

    public void finish() {
        removeAllPoolClass()
    }

    /**
     * Appends a directory or a jar (or zip) file to the end of the
     * search path.
     * @param path
     */
    public final void appendClassPath(String path) {
        Log.i(TAG, "appendClassPath path: %s", path)
        if (path == null || path.length() == 0 || classPaths.contains(path)) {
            return
        }
        // todo compile_library_classes 目录下的 module class 不包含 transform 插入代码
        path = path.replace('compile_library_classes', 'runtime_library_classes')
        classPaths.add(path)
        ClassPath cp = pool.appendClassPath(path)
        classPathList.add(cp)
    }

    public final void appendDirClass(Collection<String> inputs) {
        inputs.each { String input ->
            appendClassPath(input)
        }
    }

    /**
     * 在执行结束之后调用该方法释放掉ClassPath，不然有可能java.exe进程不能结束
     */
    private final void removeAllPoolClass() {
        classPathList.each { ClassPath cp ->
            pool.removeClassPath(cp)
        }
        pool.clearImportedPackages()
        classPathList.clear()
        classPaths.clear()
        sourcePath.clear()
    }

    /**
     * 处理注入代码入口，先处理 source
     * @param outPath
     * @return true -- 有注入代码，需要压缩回jar
     */
    private boolean handleSource(String outPath) {
        boolean inject = false
        Log.i(TAG, "handleSource path: %s", outPath)
        mProject.fileTree(outPath).findAll { File file ->
            String path = file.absolutePath
            Log.i(TAG, "handleSource  path file: %s", path)
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
            addTargetInterface(targetName, outPath)
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
        Log.i(TAG, "plugin processSource className: %s, output: %s", className, output)
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
            sb.append("${KVO_IMP}.notifyWatcher(\$0, \"${info.bindName}\", ${ov}, ${nv});\n")

            info.bindMethod.insertBefore(sb.toString())
            genFieldGetMethod(source, info)
        }
        genKvoSourceTag(source)

        source.writeFile(output)
    }

    /**
     * 添加观察者所在对象实现 IKvoTarget 限定接口，做类型检查作用
     * @param targetName
     * @param outPath
     */
    private void addTargetInterface(String targetName, String outPath) {
        CtClass target = pool.get(targetName)
        CtClass iTarget = pool.get(I_TARGET_CLASS)
        if (target.isFrozen()) {
            target.defrost()
        }
        target.addInterface(iTarget)
        target.writeFile(outPath)
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
//        targetCls.addInterface()
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
        pool.importPackage("java.util.Set")
        pool.importPackage("java.util.concurrent.CopyOnWriteArraySet")

        CtField field = CtField.make("private final Set ${KVO_SOURCE_TAG_FIELD} = new CopyOnWriteArraySet();", source)
        source.addField(field)

        CtMethod add = CtMethod.make("public boolean ${KVO_SOURCE_TAG_ADD_METHOD}(String tag) {\n" +
                "if (\$1 == null || \$1.length() == 0) {\n" +
                "            return false;\n" +
                "        }\n" +
                "return \$0.${KVO_SOURCE_TAG_FIELD}.add(\$1);\n" +
                "}", source)
        source.addMethod(add)

        CtMethod remove = CtMethod.make("public boolean ${KVO_SOURCE_TAG_REMOVE_METHOD}(String tag) {\n" +
                "if (\$1 == null || \$1.length() == 0) {\n" +
                "            return false;\n" +
                "        }\n" +
                "return \$0.${KVO_SOURCE_TAG_FIELD}.remove(\$1);\n" +
                "}", source)
        source.addMethod(remove)

//        CtMethod removeAll = CtMethod.make("final public boolean ${KVO_SOURCE_TAG_REMOVE_ALL_METHOD}(String tag) {\n" +
//                "if (\$1 == null || \$1.length() == 0) {\n" +
//                "            return false;\n" +
//                "        }\n" +
//                "        List rm = new ArrayList();\n" +
//                "        for (int i = 0; i < \$0.${KVO_SOURCE_TAG_FIELD}.size(); ++i) {\n" +
//                "                if (tag.equals(\$0.${KVO_SOURCE_TAG_FIELD}.get(i))) {\n" +
//                "                    rm.add(\$0.${KVO_SOURCE_TAG_FIELD}.get(i));\n" +
//                "                }\n" +
//                "            }\n" +
//                "        return \$0.${KVO_SOURCE_TAG_FIELD}.removeAll(rm);\n" +
//                "}", source)
//        source.addMethod(removeAll)

        CtMethod contain = CtMethod.make("public boolean ${KVO_SOURCE_TAG_CONTAIN_METHOD}(String tag) {\n" +
                "if (\$1 == null || \$1.length() == 0) {\n" +
                "            return false;\n" +
                "        }\n" +
                "return \$0.${KVO_SOURCE_TAG_FIELD}.contains(\$1);\n" +
                "}", source)
        source.addMethod(contain)
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
            if (setMethodName == m.name) {
                CtClass[] params = m.parameterTypes
                if (params.size() == 1) {
                    if (field.type in params[0]) {
                        return m
                    } else if (check) {
                        String msg = "${className}#${setMethodName} with ${params[0].name} can not be ${field.type.name}"
                        throw new RuntimeException(msg)
                    }
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
}