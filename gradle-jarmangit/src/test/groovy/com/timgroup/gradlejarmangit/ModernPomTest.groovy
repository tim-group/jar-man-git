package com.timgroup.gradlejarmangit

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.internal.xml.XmlTransformer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

class ModernPomTest {

    @Test
    void addsMetadataToPom() {
        Project project = ProjectBuilder.builder().build()
        project.with {
            apply plugin: 'java'
            apply plugin: 'maven-publish'
            apply plugin: 'jarmangit'

            publishing {
                publications {
                    mavenJava(MavenPublication) {
                        from components.java
                    }
                }
            }
        }

        project.evaluate();

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
        project.with {
            apply plugin: 'java-base'
            apply plugin: 'maven-publish'
            apply plugin: 'jarmangit'
        }

        project.evaluate();
    }
}
