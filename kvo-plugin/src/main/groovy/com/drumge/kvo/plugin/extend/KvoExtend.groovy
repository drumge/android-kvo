package com.drumge.kvo.plugin.extend

import com.drumge.easy.plugin.api.IExtend
import org.gradle.api.Action
import org.gradle.api.Project

class KvoExtend implements IExtend {
    private Project mProject

    int logLevel = -1


    private KvoExtend(Project project) {
        mProject = project
    }

    static KvoExtend createExtend(Project project, Action<KvoExtend> configuration) {
        KvoExtend extend = new KvoExtend(project)
        configuration.execute(extend)
        return extend
    }
}