package com.drumge.kvo.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.BaseVariant
import com.drumge.easy.plugin.api.IPlugin
import com.drumge.kvo.plugin.javalib.KvoJavaLibTransform
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import com.android.build.gradle.LibraryPlugin

class KvoPlugin implements IPlugin {

    private KvoTransform kvoTransform

    KvoPlugin(Project project) {
        println('================  KvoPlugin  ================')
        boolean isApp = project.plugins.withType(AppPlugin)
        boolean isAndroidLib = project.plugins.withType(LibraryPlugin)
        boolean isJavaLib = project.plugins.withType(JavaPlugin)

        if (isApp || isAndroidLib) {
            final def variants
            if (isApp) {
                variants = project.android.applicationVariants
            } else if (isAndroidLib) {
                variants = project.android.libraryVariants
            }
            variants.all { BaseVariant variant ->
                variant.getJavaCompiler().doLast {
                    println(project.name + ' compiler java last')
                    println(it.classpath.files.path)
                    if (kvoTransform != null) {
                        kvoTransform.addClassPath(it.classpath.files.path)
                    }
                }
            }
            if (project.hasProperty('easy_plugin') && project.easy_plugin.plugins.hasProperty('kvo')) {
                kvoTransform = project.easy_plugin.plugins.kvo.transform = new KvoTransform(project)
            }
        } else {
            new KvoJavaLibTransform(project)
        }
    }
}