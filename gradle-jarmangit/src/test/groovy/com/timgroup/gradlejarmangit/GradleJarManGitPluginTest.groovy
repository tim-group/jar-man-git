package com.timgroup.gradlejarmangit

import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
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

        def configuration = project.configurations.create("compileFoo")
        configuration.dependencies.add(new DefaultExternalModuleDependency('junit', 'junit', 'autobump'))

// why is the documentation for writing tests for gradle plugins so bad?
//        assertTrue(project.dependencies)
    }
}
