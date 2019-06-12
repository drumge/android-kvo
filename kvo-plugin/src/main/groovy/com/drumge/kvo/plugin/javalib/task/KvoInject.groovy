package com.drumge.kvo.plugin.javalib.task

import com.drumge.kvo.plugin.api.KvoHandler
import com.drumge.kvo.plugin.api.Log
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@Builder(builderStrategy = SimpleStrategy.class, prefix = '')
class KvoInject extends DefaultTask {
    String TAG = "KvoInject"

    private KvoHandler mKvoHandler

    @InputFiles
    FileCollection sourceFiles = getProject().files()
    @OutputDirectory
    File destinationDir

    void sources(FileCollection fileCollection) {
        Log.i(TAG, "sources files: %s", fileCollection.files)
        sourceFiles += fileCollection
    }

    @TaskAction
    void inject() {
        Log.i(TAG, "inject destinationDir: %s, sourceFiles: %s", destinationDir.absolutePath, sourceFiles.files)
        mKvoHandler = new KvoHandler(getProject())

        sourceFiles.files.each {
            println(TAG + ', ' + it)
        }

        copySource()

        mKvoHandler.appendDirClass(getProject().compileJava.classpath.files.path)

        mKvoHandler.handleKvoSource(destinationDir.absolutePath)
    }

    @Override
    Task doLast(Action<? super Task> action) {
        Log.i(TAG, "doLast")
        mKvoHandler.finish()
        return super.doLast(action)
    }

    private void copySource() {
        getProject().copy {
            into(destinationDir)
            exclude('**/*.java')
            sourceFiles.each {
                from(it)
            }
        }
    }
}