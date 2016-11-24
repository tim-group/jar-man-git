package com.timgroup.gradlejarmangit

import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.tasks.TaskAction

import org.junit.Rule


class GradleJarManGitPluginTest {

    @Test
    public void resolvesDependenciesFromAutobumpFile() {
        def Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'jarmangit'

        def configuration = project.configurations.create("compileFoo")
        configuration.dependencies.add(new DefaultExternalModuleDependency('junit', 'junit', 'autobump'))

// why is the documentation for writing tests for gradle plugins so bad?
//        assertTrue(project.dependencies)
    }
}
