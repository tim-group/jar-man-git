package com.timgroup
package sbtjarmangit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import sbt.Keys._
import sbt._

object SbtJarManGit extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  private lazy val scmInfo = repoInfo

  override lazy val projectSettings: Seq[sbt.Def.Setting[_]] = Seq(
      packageOptions in (Compile, packageBin) += Package.ManifestAttributes(scmInfo.toList: _*),
      pomExtra := <scm>
        <url>{scmInfo.getOrElse("Git-Origin", "")}</url>
        <tag>{scmInfo.getOrElse("Git-Head-Rev", "")}</tag>
      </scm>
  )

  def repoInfo: Map[String, String] = {
    val builder = (new FileRepositoryBuilder).readEnvironment.findGitDir

    if (builder.getGitDir == null && builder.getWorkTree == null) {
      return Map()
    }

    val repository = builder.build

    val origin = repository.getConfig.getString("remote", "origin", "url")
    val head = ObjectId.toString(repository.findRef(Constants.HEAD).getObjectId)
    val branch = repository.getBranch

    val isClean = new Git(repository).status.call.isClean.toString

    Map(
      "Git-Origin" -> origin,
      "Git-Branch" -> branch,
      "Git-Head-Rev" -> head,
      "Git-Repo-Is-Clean" -> isClean
    )
  }
}