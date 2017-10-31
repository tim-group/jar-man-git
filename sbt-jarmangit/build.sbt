version in ThisBuild := "1.0." + sys.env.getOrElse("BUILD_NUMBER", "0-SNAPSHOT")

organization in ThisBuild := "com.timgroup"

name := "sbt-jarmangit"

scalaVersion := "2.12.3"

sbtVersion in Global := "1.0.3"

crossSbtVersions := Vector("0.13.16", "1.0.3")

javaVersion in ThisBuild := "1.7"

javacOptions += "-g"

javacOptions += "-parameters"

publishTo in ThisBuild := Some("publish-repo" at "http://repo.youdevise.com:8081/nexus/content/repositories/yd-release-candidates")

credentials in ThisBuild += Credentials(new File("/etc/sbt/credentials"))

resolvers in ThisBuild += "TIM Group Repo" at "http://repo/nexus/content/groups/public"

sbtPlugin := true

libraryDependencies ++= Seq (
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.0.201609210915-r",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

scalaCompilerBridgeSource := {
    val sv = appConfiguration.value.provider.id.version
    ("org.scala-sbt" % "compiler-interface" % sv % "component").sources
}

