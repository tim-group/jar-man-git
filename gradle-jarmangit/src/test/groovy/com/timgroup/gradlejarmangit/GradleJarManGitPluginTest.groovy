package com.timgroup.gradlejarmangit

import org.gradle.api.Project
import org.gradle.api.artifacts.maven.PomFilterContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.impldep.org.apache.maven.model.Model
import org.gradle.internal.xml.XmlTransformer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

class GradleJarManGitPluginTest {

    @Test
    void generatesRepoInfo() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'com.timgroup.jarmangit'

        def extraProperties = project.getExtensions().getByType(ExtraPropertiesExtension)

        extraProperties.set("jarmangit.origin", "git://git.example.com/repo")
        extraProperties.set("jarmangit.revision", "4b825dc642cb6eb9a060e54bf8d69288fbee4904")
        extraProperties.set("jarmangit.branch", "master")
        extraProperties.set("jarmangit.dirty", "false")

        Map<String, String> repoInfo = GradleJarManGitPlugin.repoInfo(project)
        assertThat(repoInfo.get("Git-Origin"), is(equalTo("git://git.example.com/repo")))
        assertThat(repoInfo.get("Git-Branch"), is(equalTo("master")))
        assertThat(repoInfo.get("Git-Head-Rev"), is(equalTo("4b825dc642cb6eb9a060e54bf8d69288fbee4904")))
        assertThat(repoInfo.get("Git-Repo-Is-Clean"), is(equalTo("true")))
    }

    @Test
    void generatesRepoInfoFromProjectProperties() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'com.timgroup.jarmangit'

        Map<String, String> repoInfo = GradleJarManGitPlugin.repoInfo(project)
        assertThat(repoInfo.get("Git-Origin"), containsString("jar-man-git"))
        assertThat(repoInfo.get("Git-Branch"), is(equalTo("master")))
        assertThat(repoInfo.get("Git-Head-Rev").length(), is(equalTo(40)))
        assertThat(repoInfo.get("Git-Repo-Is-Clean"), is(either(equalTo("true")) | equalTo("false")))
    }

    @Test
    void addsMetadataToManifest() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'com.timgroup.jarmangit'

        def jar = project.tasks.create("testJarTask", Jar)
        assertThat(jar.manifest.getAttributes().get("Git-Branch").toString(), is(equalTo("master")))
    }

    @Test
    void addsMetadataToPomUsingTraditionalMaven() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'maven'
        project.apply plugin: 'com.timgroup.jarmangit'

        def configuration = project.configurations.create("testing")
        Upload uploadArchives = project.tasks.create("uploadArchives", Upload)
        uploadArchives.setConfiguration(configuration)

        project.evaluate()

        String url = null
        uploadArchives.repositories.mavenDeployer { m ->
            def foo = (PomFilterContainer)m
            def model = foo.getPom().getModel()
            url = ((Model) model).getScm().getUrl()
        }

        assertThat(url, containsString("jar-man-git"))
    }

    @Test
    void addsMetadataToPomUsingMavenPublish() {
        Project project = ProjectBuilder.builder().build()
        project.with {
            apply plugin: 'java'
            apply plugin: 'maven-publish'
            project.apply plugin: 'com.timgroup.jarmangit'

            publishing {
                publications {
                    mavenJava(MavenPublication) {
                        from components.java
                    }
                }
            }
        }

        project.evaluate()

        def publications = project.extensions.getByType(PublishingExtension).publications
        def publication = publications.iterator().next()
        def mavenPublication = (MavenPublication) publication
        def mavenPom = (MavenPomInternal) mavenPublication.pom

        XmlTransformer xmlTransformer = new XmlTransformer()
        xmlTransformer.addAction(mavenPom.xmlAction)
        def resultXml = xmlTransformer.transform("<pom></pom>")

        assertThat(resultXml.replaceAll("\\s+", ""), containsString("jar-man-git</url></scm>"))
    }

    @Test
    void doesntCrashIfNoPublishingConfigured() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'
        project.apply plugin: 'com.timgroup.jarmangit'

        project.evaluate()
    }
}
