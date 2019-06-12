package com.drumge.kvo.compiler;

import com.drumge.kvo.annotation.KvoSource;
import com.drumge.kvo.annotation.KvoWatch;
import com.drumge.kvo.compiler.java.JavaSourceProcessor;
import com.drumge.kvo.compiler.java.JavaWatchProcessor;
import com.drumge.kvo.compiler.kt.KtSourceProcessor;
import com.drumge.kvo.compiler.kt.KtWatchProcessor;
import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import kotlin.Metadata;

import static com.drumge.kvo.compiler.ConstantKt.KVO_EVENT_NAME;
import static com.drumge.kvo.compiler.ConstantKt.TAG;

/**
 * Created by chenrenzhan on 2018/4/29.
 *
 * java poet 参考资料： https://xsfelvis.github.io/2018/06/06/%E8%B0%88%E8%B0%88APT%E5%92%8CJavaPoet%E7%9A%84%E4%B8%80%E4%BA%9B%E6%8A%80%E5%B7%A7/
 * https://blog.csdn.net/l540675759/article/details/82931785
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class KvoProcessor extends AbstractProcessor {

    private JavaSourceProcessor mJavaSourceProcessor;
    private JavaWatchProcessor mJavaWatchProcessor;
    private KtSourceProcessor mKtSourceProcessor;
    private KtWatchProcessor mKtWatchProcessor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        Log.init(processingEnv.getMessager());
        Log.w(TAG, "getSupportedAnnotationTypes init");
        mJavaSourceProcessor = new JavaSourceProcessor(processingEnv);
        mJavaWatchProcessor = new JavaWatchProcessor(processingEnv);
        mKtSourceProcessor = new KtSourceProcessor(processingEnv);
        mKtWatchProcessor = new KtWatchProcessor(processingEnv);
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
        Log.w(TAG, "kvo processKvo set: %s, env: %s", set, env);
        processSource(env);
        processWatch(env);
    }

    /**
     * 生成观察者所在类的辅助类 $$KvoTargetProxy
     * @param env
     */
    private void processWatch(RoundEnvironment env) {
        Set<? extends Element> elements = env.getElementsAnnotatedWith(KvoWatch.class);
        Log.w(TAG, "processWatch elements: %s", elements);
        Set<KvoTargetInfo> targets = new HashSet<>();
        for (Element eMethod : elements) {
            ExecutableElement em = (ExecutableElement) eMethod;
            Log.w(TAG, "processWatch %s, parent: %s", em, em.getEnclosingElement());
            List<? extends VariableElement> ps = em.getParameters();
            VariableElement param;
            if (ps.size() != 1 || !(param = ps.get(0)).asType().toString().startsWith(KVO_EVENT_NAME)
                    || mJavaWatchProcessor.getTypes(param).size() != 2) {
                Log.e(TAG,
                        "%s#%s is illegal, the parameters must be size: %s, @KvoWatch can have only one parameter KvoEvent<S, V>, and must have assign the type of <S, V>",
                        em.getEnclosingElement(), em, em.getParameters().size());
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
            if (isJavaFile(info.target)) { // java
                mJavaWatchProcessor.genTargetClass(info);
                mJavaWatchProcessor.genCreatorClass(info);
            } else { // kotlin
                mKtWatchProcessor.genTargetClass(info);
                mKtWatchProcessor.genCreatorClass(info);
            }
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

    /**
     * 检查 @KvoSource 注解修饰的被观察对象，生成对应属性的name
     * @param env
     */
    private void processSource(RoundEnvironment env) {
        Set<? extends Element> elements = env.getElementsAnnotatedWith(KvoSource.class);
        Map<String, KvoSourceInfo> allClass = new HashMap<>();
        Log.w(TAG, "processSource elements: %s", elements);
        for (Element eClass : elements) {
            if (!(eClass instanceof TypeElement)) {
                Log.w(TAG, "processSource is not TypeElement %s", eClass);
                continue;
            }
            TypeElement te = (TypeElement) eClass;
            TypeElement inner = null;
            String className = te.getQualifiedName().toString();
            Log.w(TAG, "processSource isJavaFile: %b, className: %s, %s", isJavaFile(eClass), className, te.toString());
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
            // Log.w(TAG, "processSource isJavaFile: %b, className: %s, %s", isJavaFile(info.clsElement), info.className, info
            //         .clsElement.toString());
            if (isJavaFile(info.clsElement)) { // java
                mJavaSourceProcessor.genKSource(info);
            } else { // kotlin
                Log.w(TAG, "processSource kotlin");
                mKtSourceProcessor.genKSource(info);
            }
        }
    }

    /**
     * if true mean this class is java class
     */
    private boolean isJavaFile(Element element) {
        return element.getAnnotation(Metadata.class) == null;
    }
}
