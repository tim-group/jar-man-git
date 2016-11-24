package com.timgroup.gradlejarmangit

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

public final class GradleJarManGitPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.tasks.withType(Jar) { jar ->
      jar.manifest.attributes(
          "Git-Origin": "a",
          "Git-Branch": "b",
          "Git-Head-Rev": "c",
          "Git-Repo-Is-Clean": "d"
      )
    }
  }
}
