package com.timgroup.gradlejarmangit

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

public final class GradleJarManGitPlugin implements Plugin<Project> {

  Map repoInfo() {
    repository = (new FileRepositoryBuilder()).readEnvironment.findGitDir.build

    origin = repository.getConfig.getString("remote", "origin", "url")
    head = ObjectId.toString(repository.findRef(Constants.HEAD).getObjectId)
    branch = repository.getBranch

    isClean = new Git(repository).status.call.isClean.toString

    Map(
      "Git-Origin": origin,
      "Git-Branch": branch,
      "Git-Head-Rev": head,
      "Git-Repo-Is-Clean": isClean
    )
  }

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
