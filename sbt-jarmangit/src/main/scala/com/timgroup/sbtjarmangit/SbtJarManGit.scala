package com.timgroup
package sbtjarmangit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import sbt.Keys._
import sbt._

object SbtJarManGit extends AutoPlugin {
  override def trigger = allRequirements

  override lazy val projectSettings: Seq[sbt.Def.Setting[_]] = Seq(
    packageOptions in (Compile, packageBin) += Package.ManifestAttributes(repoInfo: _*)
  )

  def repoInfo: List[(String, String)] = {
    val repository = (new FileRepositoryBuilder).readEnvironment.findGitDir.build

    val origin = repository.getConfig.getString("remote", "origin", "url")
    val head = ObjectId.toString(repository.findRef(Constants.HEAD).getObjectId)
    val branch = repository.getBranch

    val isClean = new Git(repository).status.call.isClean.toString

    List(
      "Git-Origin" -> origin,
      "Git-Branch" -> branch,
      "Git-Head-Rev" -> head,
      "Git-Repo-Is-Clean" -> isClean
    )
  }
}