package com.timgroup.gradlejarmangit

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.jar.JarFile
import java.util.jar.Manifest

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

class GradleJarManGitPluginTest {
    @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    File settingsFile

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    @Test
    void generatesRepoInfo() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'java-base'

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

        Map<String, String> repoInfo = GradleJarManGitPlugin.repoInfo(project)
        assertThat(repoInfo.get("Git-Origin"), containsString("jar-man-git"))
        assertThat(repoInfo.get("Git-Branch"), is(equalTo("master")))
        assertThat(repoInfo.get("Git-Head-Rev").length(), is(equalTo(40)))
        assertThat(repoInfo.get("Git-Repo-Is-Clean"), is(either(equalTo("true")) | equalTo("false")))
    }

    @Test
    void "adds metadata to manifest"() {
        settingsFile << """
rootProject.name = 'testee'
"""
        buildFile << """
plugins {
  id 'com.timgroup.jarmangit'
  id 'java'
}
"""

        def result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("assemble")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":jar").outcome, is(equalTo(TaskOutcome.SUCCESS)))

        def jarFile = new JarFile(testProjectDir.root.toPath().resolve("build/libs/testee.jar").toFile())
        assertThat(jarFile.manifest, containsAttribute("Git-Branch", equalTo("master")))
        jarFile.close()
    }

    @Test
    void "adds metadata to POM using traditional maven"() {
        settingsFile << """
rootProject.name = 'testee'
"""
        buildFile << """
plugins {
  id 'com.timgroup.jarmangit'
  id 'java'
  id 'maven'
}

uploadArchives {
  repositories {
    mavenDeployer {
      repository(url: "file://localhost\${project.buildDir}/repo")
    }
  }
}

group = 'com.example'
"""


        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("upload")
                .withPluginClasspath()
                .build()

        assertThat(result.task(":jar").outcome, is(equalTo(TaskOutcome.SUCCESS)))
        assertThat(result.task(":uploadArchives").outcome, is(equalTo(TaskOutcome.SUCCESS)))

        def pomFile = testProjectDir.root.toPath().resolve("build/repo/com/example/testee/unspecified/testee-unspecified.pom").toFile()
        def pomContent = new XmlParser().parse(pomFile)
        assertThat(pomContent.scm.url.text(), containsString("jar-man-git"))
    }

    @Test
    void "adds metadata to POM using maven-publish"() {
        settingsFile << """
rootProject.name = 'testee'
"""
        buildFile << """
plugins {
  id 'com.timgroup.jarmangit'
  id 'java'
  id 'maven-publish'
}

group = 'com.example'

publishing {
  repositories {
    maven {
      url "\${project.buildDir}/repo"
    }
  }
  publications {
    mavenJava(MavenPublication) {
      from components.java
    }
  }
}
"""

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("publish")
                .withPluginClasspath()
                .build()

        assertThat(result.task(":jar").outcome, is(equalTo(TaskOutcome.SUCCESS)))
        assertThat(result.task(":generatePomFileForMavenJavaPublication").outcome, is(equalTo(TaskOutcome.SUCCESS)))
        assertThat(result.task(":publishMavenJavaPublicationToMavenRepository").outcome, is(equalTo(TaskOutcome.SUCCESS)))

        def pomFile = testProjectDir.root.toPath().resolve("build/repo/com/example/testee/unspecified/testee-unspecified.pom").toFile()
        def pomContent = new XmlParser().parse(pomFile)
        assertThat(pomContent.scm.url.text(), containsString("jar-man-git"))
    }

    @Test
    void "doesn't crash if publishing not configured"() {
        settingsFile << """
rootProject.name = 'testee'
"""
        buildFile << """
plugins {
  id 'com.timgroup.jarmangit'
  id 'java'
}
"""

        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("build")
                .withPluginClasspath()
                .build()
    }

    static def containsAttribute(String name, Matcher<?> valueMatcher) {
        return new TypeSafeDiagnosingMatcher<Manifest>() {
            @Override
            protected boolean matchesSafely(Manifest item, Description mismatchDescription) {
                def value = item.getMainAttributes().getValue(name)
                if (value == null) {
                    mismatchDescription.appendText("attributes present: ").appendValue(item.getMainAttributes().keySet())
                    return false
                }
                valueMatcher.describeMismatch(value, mismatchDescription)
                return valueMatcher.matches(value)
            }

            @Override
            void describeTo(Description description) {
                description.appendText("has attribute ").appendValue(name).appendText(" ").appendDescriptionOf(valueMatcher)
            }
        }
    }
}
