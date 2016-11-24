package com.timgroup.gradlejarmangit

import org.apache.maven.model.Model
import org.apache.maven.model.Scm
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployer
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar

final class GradleJarManGitPlugin implements Plugin<Project> {

  static Map<String, String> repoInfo() {
    Repository repository = new FileRepositoryBuilder().readEnvironment().findGitDir().build()

    String origin = repository.getConfig().getString("remote", "origin", "url")
    String head = ObjectId.toString(repository.findRef(Constants.HEAD).getObjectId())
    String branch = repository.getBranch()
    String isClean = new Git(repository).status().call().isClean().toString()

    [
      "Git-Origin": origin,
      "Git-Branch": branch,
      "Git-Head-Rev": head,
      "Git-Repo-Is-Clean": isClean
    ]
  }

  @Override
  void apply(Project project) {
    project.tasks.withType(Jar) { jar -> jar.manifest.attributes(repoInfo()) }

    Upload uploadArchives = (Upload)project.getTasks().withType(Upload.class).findByName("uploadArchives")
    if (uploadArchives != null) {
        def info = repoInfo()
        uploadArchives.repositories.withType(MavenDeployer.class).all {deployer ->
            Scm scm = ((Model)deployer.pom.getModel()).getScm()
            scm.setTag(info.get("Git-Head-Rev"))
            scm.setUrl(info.get("Git-Origin"))
        }
    }

  }
}