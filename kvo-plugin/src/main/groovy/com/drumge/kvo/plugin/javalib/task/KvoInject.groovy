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
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

@Builder(builderStrategy = SimpleStrategy.class, prefix = '')
class KvoInject extends DefaultTask {
    String TAG = "KvoInject"

    private KvoHandler mKvoHandler
    private final WorkerExecutor workerExecutor

    @InputFiles
    FileCollection sourceFiles = getProject().files()
    @OutputDirectory
    File destinationDir

    @Inject
    KvoInject(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    void sources(FileCollection fileCollection) {
        Log.i(TAG, "sources files: %s", fileCollection.files)
        sourceFiles += fileCollection
    }

    @TaskAction
    void inject() {
        Log.i(TAG, "inject destinationDir: %s, sourceFiles: %s", destinationDir.absolutePath, sourceFiles.files)
        mKvoHandler = new KvoHandler(getProject())

//        sourceFiles.files.each {
//            println(TAG + ', ' + it)
//        }

        mKvoHandler.appendDirClass(getProject().compileJava.classpath.files.path)
        workerExecutor.submit(KvoInjectRunnable.class, new Action<WorkerConfiguration>() {
            @Override
            void execute(WorkerConfiguration config) {
                config.params(mKvoHandler, sourceFiles, destinationDir)
            }
        })
//        copySource()

//        mKvoHandler.handleKvoSource(destinationDir.absolutePath)
//        mKvoHandler.onAfterDirectory()
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