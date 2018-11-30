package com.drumge.kvo.plugin.javalib

import com.drumge.kvo.plugin.javalib.task.KvoInject
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

class KvoJavaLibTransform {

    private Project mProject
    private KvoInject kvoJar
    private boolean isKvoJarTask

    KvoJavaLibTransform(Project project) {
        mProject = project
        println(project)

        createKvoJarTask()

        afterEvaluate()
        hookJar()

    }

    private void createKvoJarTask() {
        kvoJar = mProject.tasks.create('kvoJar', KvoInject.class) {
            group = 'kvo'
            description = 'build kvo lib'

            String buildClassPath = mProject.compileJava.destinationDir.absolutePath
            sources mProject.compileJava.outputs.files
            destinationDir mProject.file(buildClassPath.replace('classes', 'kvo'))
        }

        mProject.compileJava.finalizedBy kvoJar
    }

    private void afterEvaluate() {
        mProject.afterEvaluate {
            isKvoJarTask = mProject.kvoJar.isEnabled()
        }
    }

    private void hookJar() {
        mProject.tasks.withType(Jar) {
            from(mProject.files(kvoJar.destinationDir))
            Set<String> sourcePath = kvoJar.sourceFiles.files.path
            exclude { item ->
                if (isKvoJarTask) {
                    for (String path : sourcePath) {
                        if (item.file.absolutePath.contains(path)) {
                            return true
                        }
                    }
                    return false
                } else {
                    return item.file.absolutePath.contains(kvoJar.destinationDir)
                }
            }
        }
    }
}