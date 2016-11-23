package com.timgroup
package sbtjarmangit

import sbt._
import Keys._

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib._
import org.eclipse.jgit.api.Git

object SbtJarManGit extends AutoPlugin {

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

  val jarManGitSettings = Seq(packageOptions += { Package.ManifestAttributes(repoInfo: _*) })
}