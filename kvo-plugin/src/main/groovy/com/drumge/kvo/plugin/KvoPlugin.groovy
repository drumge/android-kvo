package com.drumge.kvo.plugin

import com.android.build.gradle.AppPlugin
import com.drumge.easy.plugin.api.IPlugin
import com.drumge.easy.plugin.utils.DependenciesUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency

class KvoPlugin implements IPlugin {

    KvoPlugin(Project project) {
        println('================  KvoPlugin  ================')

    }
}