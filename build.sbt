import sbt.Keys._

val commonSettings = Seq(
  version := "2.2.0-2015.08.13-800a44ec7ee3565eab727996bcc7ccdcf69bced6-STAGE-SNAPSHOT",
  scalaVersion := "2.11.7",
  organization := "org.globalnames",
  homepage := Some(new URL("http://globalnames.org/")),
  description := "Fast and elegant parser for taxonomic scientific names",
  startYear := Some(2008),
  licenses := Seq("MIT" -> new URL("https://github.com/GlobalNamesArchitecture/gnparser/blob/master/LICENSE")),
  javacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-source", "1.6",
    "-target", "1.6",
    "-Xlint:unchecked",
    "-Xlint:deprecation"),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-language:_",
    "-target:jvm-1.6",
    "-Xlog-reflective-calls"))

val noPublishingSettings = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

lazy val root = project.in(file("."))
  .aggregate(parser)
  .settings(noPublishingSettings: _*)

lazy val parser = (project in file("./parser"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "global-names-parser",
    version := "0.1.0-SNAPSHOT",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.globalnames.parser",
    test in assembly := {},

    libraryDependencies ++= Seq(
      "org.json4s"         %% "json4s-jackson"         % "3.2.11",
      "org.littleshoot"    %  "littleshoot-commons-id" % "1.0.3",
      "org.apache.commons" %  "commons-lang3"          % "3.4",
      "org.globalnames"    %% "parboiled"              % "2.2.0-2015.09.11-SNAPSHOT",
      "com.chuusai"        %% "shapeless"              % "2.2.3",
      "org.scalaz"         %% "scalaz-core"            % "7.1.3",
      "org.specs2"         %% "specs2-core"            % "3.6.3" % "test"
    ),

    resolvers ++= Seq(
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    ),

    scalacOptions in Test ++= Seq("-Yrangepos"),

    initialCommands in console :=
      """import org.globalnames.parser.{ScientificNameParser => SNP, _}
        |import scala.util.{Failure, Success, Try}
        |import org.parboiled2._""".stripMargin
  )
