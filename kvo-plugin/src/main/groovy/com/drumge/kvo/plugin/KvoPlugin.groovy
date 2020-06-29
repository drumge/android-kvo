package com.drumge.kvo.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.drumge.easy.plugin.api.IPlugin
import com.drumge.kvo.plugin.javalib.KvoJavaLibTransform
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class KvoPlugin implements IPlugin {

    private KvoTransform kvoTransform
    private Project mProject
    private List<String> variantNames = new ArrayList<>()

    KvoPlugin(Project project) {
        mProject = project
        println('================  KvoPlugin  ================')
//        boolean isApp = project.plugins.withType(AppPlugin)
//        boolean isAndroidLib = project.plugins.withType(LibraryPlugin)
//        boolean isJavaLib = project.plugins.withType(JavaPlugin)
        boolean isApp = project.plugins.hasPlugin('com.android.application')
        boolean isAndroidLib = project.plugins.hasPlugin('com.android.library')
        boolean isJavaLib = project.plugins.hasPlugin('java')

        if (isApp || isAndroidLib) {
            createTransform(project)
            getAllVariants(project)

            project.gradle.taskGraph.afterTask {
                if (mProject != it.project) {
                    return
                }
                if (contains(variantNames, it.name)) {
                    if (kvoTransform != null) {
                        kvoTransform.addClassPath(it.classpath.files.path)
                    }
                }
            }
        } else {
            new KvoJavaLibTransform(project)
        }
    }

    private void createTransform(Project project) {
        if (project.hasProperty('easy_plugin') && project.easy_plugin.plugins.hasProperty('kvo')) {
            kvoTransform = project.easy_plugin.plugins.kvo.transform = new KvoTransform(project)
        }
    }

    private void getAllVariants(Project project) {
        project.afterEvaluate {
            if (project.plugins.withType(AppPlugin)) {
                project.android.applicationVariants.all {

                    String name = captureName(it.name)
                    variantNames.add("compile${name}JavaWithJavac")
                }
            } else if (project.plugins.withType(LibraryPlugin)) {
                project.android.libraryVariants.all {
                    String name = captureName(it.name)
                    variantNames.add("compile${name}JavaWithJavac")
                }
            }
            println('KvoPlugin ' + project + ' , variantNames ' + variantNames)
        }
    }

    //首字母大写
    static String captureName(String name) {
        char[] cs = name.toCharArray()
        cs[0] -= 32
        return String.valueOf(cs)

    }

    static boolean contains(Collection<String> collection, String e) {
        for (String str : collection) {
            if (str == e) {
                return true
            }
        }
        return false

    }
}