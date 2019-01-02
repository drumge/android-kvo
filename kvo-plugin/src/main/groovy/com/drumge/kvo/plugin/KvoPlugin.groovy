package com.drumge.kvo.plugin

import com.android.build.gradle.AppPlugin
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
            if (project.hasProperty('easy_plugin') && project.easy_plugin.plugins.hasProperty('kvo')) {
                kvoTransform = project.easy_plugin.plugins.kvo.transform = new KvoTransform(project)
            }
            project.gradle.taskGraph.afterTask {
                if (it.name == 'compileDebugJavaWithJavac' || it.name == 'compileReleaseJavaWithJavac') {
                    if (kvoTransform != null) {
                        kvoTransform.addClassPath(it.classpath.files.path)
                    }
                }
            }
        } else {
            new KvoJavaLibTransform(project)
        }
    }
}