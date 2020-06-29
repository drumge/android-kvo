package com.drumge.kvo.plugin

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformOutputProvider
import com.drumge.easy.plugin.api.BaseEasyTransform
import com.drumge.easy.plugin.api.IEasyTransformSupport
import com.drumge.kvo.plugin.api.KvoHandler
import com.drumge.kvo.plugin.api.Log
import org.gradle.api.Project

class KvoTransform extends BaseEasyTransform {
    private static final String TAG = "KvoTransform"

    private Project mProject
    private IEasyTransformSupport mSupport
    private KvoHandler mHandler
    private boolean hasJavaCompiler = false
    private Collection<String> mClassPath = new ArrayList<>()


    KvoTransform(Project project) {
        super(project)
        mProject = project
//        mHandler = new KvoHandler(project, pool)
//        mHandler.appendClassPath(mProject.android.bootClasspath[0].toString())
    }

    void addClassPath(Collection<String> classPath) {
//        Log.i(TAG, mProject.name + " addClassPath classPath: %s", classPath)
        hasJavaCompiler = true
        mClassPath.addAll(classPath)
    }

    @Override
    void onTransformSupport(IEasyTransformSupport support) {
        super.onTransformSupport(support)
        mSupport = support
    }

    @Override
    void onBeforeTransform(Context context, TransformOutputProvider outputProvider, boolean isIncremental) {
        super.onBeforeTransform(context, outputProvider, isIncremental)
        Log.i(TAG, mProject.name + " onBeforeTransform ")
        mHandler = new KvoHandler(mProject)
        mHandler.appendClassPath(mProject.android.bootClasspath[0].toString())
        mHandler.appendDirClass(mClassPath)
    }

    @Override
    boolean isNeedUnzipJar(JarInput jarInput, File outputFile) {
//        String name = jarInput.name
//        Log.i(TAG, mProject.name + " isNeedUnzipJar name: %s, status: %s", name, jarInput.status)
        return false
    }

    @Override
    boolean onUnzipJarFile(JarInput jarInput, String unzipPath, File outputFile) {
//        String name = jarInput.name
//        Log.d(TAG, mProject.name + " onUnzipJarFile name: %s,  unzipPath: %s", name, unzipPath)
        return false
    }

    @Override
    void onAfterJar() {
        super.onAfterJar()
        Log.d(TAG, mProject.name + " onAfterJar ")
    }

    @Override
    void onEachDirectoryOutput(DirectoryInput directoryInput, File outputDirFile) {
        super.onEachDirectoryOutput(directoryInput, outputDirFile)
        mHandler.appendClassPath(outputDirFile.absolutePath)
    }

    @Override
    void onChangeFile(DirectoryInput directoryInput, File outputDirFile, File file) {
        super.onChangeFile(directoryInput, outputDirFile, file)
        if (hasJavaCompiler) {
            mHandler.handleFile(outputDirFile, file)
        }
    }

    @Override
    void onAfterDirectory() {
        super.onAfterDirectory()
        Log.i(TAG, mProject.name + " onAfterDirectory ")
        mSupport.execute {
            mHandler.onAfterDirectory()
        }
        mSupport.waitForTasks()
    }

    @Override
    void onAfterTransform() {
        super.onAfterTransform()
    }

    @Override
    void onFinally() {
        super.onFinally()
        mHandler.finish()
        mClassPath.clear()
        Log.i(TAG, project.name + " onFinally ")
    }
}