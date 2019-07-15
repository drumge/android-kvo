package com.drumge.kvo.compiler;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class ProcessVariableElement implements VariableElement {
    //TypeElement
    private Element enClosingElement;

    public ProcessVariableElement(Element enClosingElement) {
        this.enClosingElement = enClosingElement;
    }

    private static final String VALUE = "KVO";

    @Override
    public Object getConstantValue() {
        return VALUE;
    }

    @Override
    public TypeMirror asType() {
        return null;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.FIELD;
    }

    @Override
    public Set<Modifier> getModifiers() {
        Set<Modifier> modifierSet = new HashSet<>();
        modifierSet.add(Modifier.PRIVATE);
        return modifierSet;
    }

    @Override
    public Name getSimpleName() {
        return null;
    }

    @Override
    public Element getEnclosingElement() {
        return enClosingElement;
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return null;
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return null;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return null;
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> aClass) {
        return null;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> elementVisitor, P p) {
        return null;
    }
}
