package com.timgroup.sbtjarmangit

import org.scalatest.FunSuite

class SbtJarManGitSuite extends FunSuite {

  test("generates repo info") {
    val repoInfo: List[(String, String)] = SbtJarManGit.repoInfo

    val keys = repoInfo.map(t => t._1)
    assert(keys == List(
      "Git-Origin",
      "Git-Branch",
      "Git-Head-Rev",
      "Git-Repo-Is-Clean"
    ))

    val values = repoInfo.map(t => t._2)
    assert(values(0).contains("jar-man-git"))
    assert(values(1) == "master")
    assert(values(2).length == 40)
    assert(values(3) == "true" || values(3) == "false")
  }

}
