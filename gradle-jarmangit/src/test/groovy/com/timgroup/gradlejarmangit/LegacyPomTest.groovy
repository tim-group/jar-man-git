package com.timgroup.gradlejarmangit

import org.apache.maven.model.Model
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.PomFilterContainer
import org.gradle.api.internal.artifacts.ModuleInternal
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider
import org.gradle.api.tasks.Upload
import org.gradle.listener.DefaultListenerManager
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
import static org.junit.Assert.assertThat

class LegacyPomTest {

    @Test
    void addsMetadataToPom() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'maven'
        project.apply plugin: 'jarmangit'

        Upload uploadArchives = project.tasks.create("uploadArchives", Upload)
        uploadArchives.setConfiguration(new DefaultConfiguration("", "", null, null, new DefaultListenerManager(), new DependencyMetaDataProvider() {
            @Override
            ModuleInternal getModule() {
                return null
            }
        }, null))

        project.evaluate();

        String url = null
        uploadArchives.repositories.mavenDeployer { m ->
            def foo = (PomFilterContainer)m
            url = ((Model)foo.getPom().getModel()).getScm().getUrl()
        }

        assertThat(url, containsString("jar-man-git"))
    }
}
