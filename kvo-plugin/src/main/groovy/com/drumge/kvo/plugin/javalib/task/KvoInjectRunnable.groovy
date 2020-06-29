package com.drumge.kvo.plugin.javalib.task

import com.drumge.kvo.plugin.api.KvoHandler
import org.apache.commons.io.FileUtils
import org.gradle.api.file.FileCollection


class KvoInjectRunnable implements Runnable {

    private KvoHandler mKvoHandler
    private FileCollection sourceFiles
    private File outDir

    KvoInjectRunnable(KvoHandler kvoHandler, FileCollection sourceFiles, File outDir) {
        mKvoHandler = kvoHandler
        this.sourceFiles = sourceFiles
        this.outDir = outDir
    }

    @Override
    void run() {
        sourceFiles.each {
            FileUtils.copyDirectory(it, outDir, new FileFilter() {
                @Override
                boolean accept(File file) {
                    return !file.absolutePath.endsWith(".java")
                }
            })
        }
        FileUtils.listFiles(outDir, ['class'], true).each {
            mKvoHandler.handleFile(outDir, it)
        }
//        mKvoHandler.handleKvoSource(destinationDir.absolutePath)
        mKvoHandler.onAfterDirectory()
    }
}