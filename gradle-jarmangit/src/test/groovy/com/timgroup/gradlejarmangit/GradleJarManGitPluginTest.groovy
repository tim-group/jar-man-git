package com.timgroup.gradlejarmangit

import org.apache.maven.model.Model
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.maven.PomFilterContainer
import org.gradle.api.internal.artifacts.ModuleInternal
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.listener.DefaultListenerManager
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

class GradleJarManGitPluginTest {

    @Test
    void generatesRepoInfo() {
        Map<String, String> repoInfo = GradleJarManGitPlugin.repoInfo()
        assertThat(repoInfo.get("Git-Origin"), containsString("jar-man-git"))
        assertThat(repoInfo.get("Git-Branch"), is(equalTo("master")))
        assertThat(repoInfo.get("Git-Head-Rev").length(), is(equalTo(40)))
        assertThat(repoInfo.get("Git-Repo-Is-Clean"), is(either(equalTo("true")) | equalTo("false")))
    }

    @Test
    void addsMetadataToManifest() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'jarmangit'

        def jar = project.tasks.create("testJarTask", Jar)
        assertThat(jar.manifest.getAttributes().get("Git-Branch").toString(), is(equalTo("master")))
    }

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
