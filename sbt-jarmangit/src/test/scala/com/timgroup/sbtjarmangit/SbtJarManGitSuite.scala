package com.timgroup.sbtjarmangit

import org.scalatest.FunSuite

class SbtJarManGitSuite extends FunSuite {

  test("generates repo info") {
    val repoInfo: Map[String, String] = SbtJarManGit.repoInfo

    assert(repoInfo("Git-Origin").contains("jar-man-git"))
    assert(repoInfo("Git-Branch") == "master")
    assert(repoInfo("Git-Head-Rev").length == 40)
    assert(repoInfo("Git-Repo-Is-Clean") == "true" || repoInfo("Git-Repo-Is-Clean") == "false")
  }

}
