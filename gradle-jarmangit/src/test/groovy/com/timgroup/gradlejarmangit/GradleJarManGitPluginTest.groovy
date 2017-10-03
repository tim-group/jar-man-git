package com.timgroup.gradlejarmangit

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

        // yuk, this is taking settings from the local checkout of jar-man-git itself
        // this is arguably a bug, the plugin should only look at where the project lives
        new JarFile(testProjectDir.root.toPath().resolve("build/libs/testee.jar").toFile()).withCloseable { jarFile ->
            assertThat(jarFile.manifest, containsAttribute("Git-Head-Rev", is(sha1Hash())))
            assertThat(jarFile.manifest, containsAttribute("Git-Branch", is(equalTo("master"))))
            assertThat(jarFile.manifest, containsAttribute("Git-Origin", containsString("jar-man-git")))
            assertThat(jarFile.manifest, containsAttribute("Git-Repo-Is-Clean", is(either(equalTo("true")) | equalTo("false"))))
        }
    }

    @Test
    void "adds metadata from project properties to manifest"() {
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
            .withArguments(
                "-Pjarmangit.revision=4b825dc642cb6eb9a060e54bf8d69288fbee4904",
                "-Pjarmangit.branch=some-branch",
                "-Pjarmangit.origin=git://git.example.com/repo.git",
                "-Pjarmangit.dirty=false",
                "assemble")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":jar").outcome, is(equalTo(TaskOutcome.SUCCESS)))

        new JarFile(testProjectDir.root.toPath().resolve("build/libs/testee.jar").toFile()).withCloseable { jarFile ->
            assertThat(jarFile.manifest, containsAttribute("Git-Head-Rev", equalTo("4b825dc642cb6eb9a060e54bf8d69288fbee4904")))
            assertThat(jarFile.manifest, containsAttribute("Git-Branch", equalTo("some-branch")))
            assertThat(jarFile.manifest, containsAttribute("Git-Origin", equalTo("git://git.example.com/repo.git")))
            assertThat(jarFile.manifest, containsAttribute("Git-Repo-Is-Clean", equalTo("true")))
        }
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

    static def sha1Hash() {
        return new TypeSafeDiagnosingMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item, Description mismatchDescription) {
                def hexDigits = "0123456789abcdef"
                mismatchDescription.appendText("was ").appendValue(item)
                return item.length() == 40 && !item.chars.any { hexDigits.indexOf((int) it.charValue()) < 0 }
            }

            @Override
            void describeTo(Description description) {
                description.appendText("SHA-1 hash")
            }
        }
    }
}
