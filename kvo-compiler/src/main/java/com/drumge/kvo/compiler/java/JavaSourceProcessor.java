package com.drumge.kvo.compiler.java;

import com.drumge.kvo.annotation.KvoSource;
import com.drumge.kvo.compiler.KvoSourceInfo;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static com.drumge.kvo.compiler.ConstantKt.JAVA_DOC;
import static com.drumge.kvo.compiler.ConstantKt.SOURCE_CLASS_SUFFIX;
import static com.drumge.kvo.compiler.ConstantKt.SOURCE_FILED_CLASS_PREFIX;
import static com.drumge.kvo.compiler.KvoCompilerUtilsKt.checkFieldLegal;
import static com.drumge.kvo.compiler.KvoCompilerUtilsKt.format;
import static com.drumge.kvo.compiler.KvoCompilerUtilsKt.getPackage;
import static com.drumge.kvo.compiler.KvoCompilerUtilsKt.getSimpleName;

/**
 * Created by chenrenzhan on 2019/5/18.
 */
public class JavaSourceProcessor {
    private ProcessingEnvironment processingEnv;

    public JavaSourceProcessor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void genKSource(KvoSourceInfo info) {
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
}
