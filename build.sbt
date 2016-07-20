import sbt.Keys._

val commonSettings = Seq(
  version := "0.3.2-SNAPSHOT",
  scalaVersion := "2.11.8",
  organization := "org.globalnames",
  homepage := Some(new URL("http://globalnames.org/")),
  description := "Fast and elegant parser for taxonomic scientific names",
  startYear := Some(2008),
  licenses := Seq("MIT" -> new URL("https://github.com/GlobalNamesArchitecture/gnparser/blob/master/LICENSE")),
  resolvers ++= Seq(
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
  ),
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

val publishingSettings = Seq(
  publishMavenStyle := true,
  useGpg := true,
  publishTo <<= version { v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
    else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <scm>
      <url>git@github.com:GlobalNamesArchitecture/gnparser.git</url>
      <connection>scm:git:git@github.com:GlobalNamesArchitecture/gnparser.git</connection>
    </scm>
      <developers>
        <developer>
          <id>dimus</id>
          <name>Dmitry Mozzherin</name>
        </developer>
        <developer>
          <id>alexander-myltsev</id>
          <name>Alexander Myltsev</name>
          <url>http://myltsev.com</url>
        </developer>
      </developers>)

val noPublishingSettings = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

/////////////////////// DEPENDENCIES /////////////////////////

val akkaV        = "2.4.6"

val akkaHttpCore     = "com.typesafe.akka"  %% "akka-http-core"                    % akkaV
val akkaHttp         = "com.typesafe.akka"  %% "akka-http-experimental"            % akkaV
val akkaActor        = "com.typesafe.akka"  %% "akka-actor"                        % akkaV
val akkaJson         = "com.typesafe.akka"  %% "akka-http-spray-json-experimental" % akkaV
val spark            = "org.apache.spark"   %% "spark-core"                        % "1.6.1" % Provided
val shapeless        = "com.chuusai"        %% "shapeless"                         % "2.3.0"
val json4s           = "org.json4s"         %% "json4s-jackson"                    % "3.2.10"
val javaUuid         = "com.fasterxml.uuid" %  "java-uuid-generator"               % "3.1.4"
val lang3            = "org.apache.commons" %  "commons-lang3"                     % "3.4"
val parboiled        = "org.globalnames"    %% "parboiled"                         % "2.1.2.2"
val scalaz           = "org.scalaz"         %% "scalaz-core"                       % "7.1.7"
val scopt            = "com.github.scopt"   %% "scopt"                             % "3.4.0"
val specs2core       = "org.specs2"         %% "specs2-core"                       % "3.6.6" % Test
val akkaHttpTestkit  = "com.typesafe.akka"  %% "akka-http-testkit"                 % akkaV   % Test
val scalatest        = "org.scalatest"      %% "scalatest"                         % "2.2.6" % Test

/////////////////////// PROJECTS /////////////////////////

lazy val root = project.in(file("."))
  .aggregate(parser, exampleJavaScala, runner, sparkPython)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.6", "2.11.8")
  )

lazy val parser = (project in file("./parser"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnparser",
    crossScalaVersions := Seq("2.10.6", "2.11.8"),

    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "org.globalnames.parser",
    test in assembly := {},

    libraryDependencies ++= {
      Seq(shapeless, json4s, javaUuid, lang3, parboiled, scalaz, specs2core)
    },

    scalacOptions in Test ++= Seq("-Yrangepos"),

    initialCommands in console :=
      """import org.globalnames.parser.{ScientificNameParser => SNP, _}
        |import SNP.instance._
        |import org.globalnames.parser.Parser._
        |import scala.util.{Failure, Success, Try}
        |import org.parboiled2._
        |import org.json4s.jackson.JsonMethods._""".stripMargin
  )

lazy val benchmark = (project in file("./benchmark"))
  .dependsOn(parser)
  .enablePlugins(JmhPlugin)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-benchmark"
  )

lazy val runner = (project in file("./runner"))
  .dependsOn(parser)
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin, SbtTwirl)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-runner",
    executableScriptName := "gnparse",
    crossScalaVersions := Seq("2.11.8"),
    packageName := "gnparser",
    bashScriptExtraDefines := Seq(
      s"""declare -r script_name="${executableScriptName.value}""""
    ),
    libraryDependencies ++= Seq(scopt, akkaHttp, akkaHttpCore, akkaActor,
                                akkaJson, akkaHttpTestkit, scalatest),
    mainClass in Compile := Some("org.globalnames.GnParser"),
    mainClass in reStart :=
      Some("org.globalnames.parser.runner.web.controllers.WebServer"),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "org.globalnames.runner",
    dockerRepository := Some("gnames")
  )

lazy val exampleJavaScala = (project in file("./examples/java-scala"))
  .dependsOn(parser)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-examples"
  )

lazy val exampleSpark = (project in file("./examples/spark"))
  .dependsOn(parser)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-example-spark",
    libraryDependencies ++= Seq(spark)
  )

lazy val sparkPython = (project in file("./spark-python"))
  .dependsOn(parser)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnparser-spark-python",
    libraryDependencies ++= Seq(spark),
    projectDependencies := Seq(
      (projectID in parser).value.exclude("org.json4s", "json4s-jackson_2.10")
    )
  )
