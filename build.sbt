import sbt.Keys._
import scala.util.Try

val scalaV11 = "2.11.12"
val scalaV12 = "2.12.6"

val commonSettings = Seq(
  version := {
    val version = "1.0.3"
    val release =
      sys.props.get("release").flatMap { x => Try(x.toBoolean).toOption }.getOrElse(false)
    if (release) version
    else {
      val versionPostfix = sys.props.get("buildNumber").map { "-" + _ }.getOrElse("") + "-SNAPSHOT"
      version + versionPostfix
    }
  },
  scalaVersion := scalaV11,
  organization in ThisBuild := "org.globalnames",
  homepage := Some(new URL("http://globalnames.org/")),
  description := "Fast and elegant parser for taxonomic scientific names",
  startYear := Some(2008),
  licenses := Seq("MIT" -> new URL("https://github.com/GlobalNamesArchitecture/gnparser/blob/master/LICENSE")),
  resolvers ++= Seq(
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
    // , Resolver.typesafeRepo("snapshots")
    , Resolver.typesafeRepo("releases")
  ),
  javacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-source", "1.8",
    "-target", "1.8",
    "-Xlint:unchecked",
    "-Xlint:deprecation"),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-language:_",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls"),

  initialCommands in console :=
    """import org.globalnames.parser.{ScientificNameParser => SNP, _}
      |import SNP.instance._
      |import org.globalnames.parser.Parser._
      |import scala.util.{Failure, Success, Try}
      |import org.parboiled2._""".stripMargin
)

val publishingSettings = Seq(
  publishMavenStyle := true,
  useGpg := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT")) {
      Some("snapshots" at nexus + "content/repositories/snapshots")
    } else {
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
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

val akkaV            = "10.0.13"
val specs2V          = "4.3.3"

val akkaHttp         = "com.typesafe.akka"          %% "akka-http"                         % akkaV
val akkaJson         = "com.typesafe.akka"          %% "akka-http-spray-json"              % akkaV
val logbackClassic   = "ch.qos.logback"             %  "logback-classic"                   % "1.2.3"
val spark            = "org.apache.spark"           %% "spark-core"                        % "2.1.1"        % Provided
val shapeless        = "com.chuusai"                %% "shapeless"                         % "2.3.3"
val javaUuid         = "com.fasterxml.uuid"         %  "java-uuid-generator"               % "3.1.5"
val commonsText      = "org.apache.commons"         %  "commons-text"                      % "1.4"
val parboiled        = "org.globalnames"            %% "parboiled"                         % "2.1.4.1"
val scalaz           = "org.scalaz"                 %% "scalaz-core"                       % "7.2.26"
val scalaArm         = "com.jsuereth"               %% "scala-arm"                         % "2.0"
val scopt            = "com.github.scopt"           %% "scopt"                             % "3.7.0"
val specs2core       = "org.specs2"                 %% "specs2-core"                       % specs2V        % Test
val specs2extra      = "org.specs2"                 %% "specs2-matcher-extra"              % specs2V        % Test
val akkaHttpTestkit  = "com.typesafe.akka"          %% "akka-http-testkit"                 % akkaV          % Test
val scalatest        = "org.scalatest"              %% "scalatest"                         % "3.0.5"        % Test
val json4s           = "org.json4s"                 %% "json4s-jackson"                    % "3.6.0"        % Test

/////////////////////// PROJECTS /////////////////////////

lazy val `gnparser-root` = project.in(file("."))
  .aggregate(parser, `parser-render`, exampleJavaScala, runner, sparkPython)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq()
  )

lazy val parser = (project in file("./parser"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnparser",
    crossScalaVersions := Seq(scalaV11, scalaV12),

    test in assembly := {},

    libraryDependencies ++= {
      Seq(shapeless, javaUuid, commonsText, parboiled, scalaz, specs2core)
          // , "com.lihaoyi" %% "pprint" % "0.5.3"
    },

    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val `parser-render` = (project in file("./parser-render"))
  .dependsOn(parser)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnparser-render",
    crossScalaVersions := Seq(scalaV11, scalaV12),

    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "org.globalnames.parser",
    test in assembly := {},

    libraryDependencies ++= {
      Seq(shapeless, akkaJson, javaUuid, commonsText, parboiled, scalaz, specs2core, json4s)
//      , "com.lihaoyi" %% "pprint" % "0.5.3"
    },

    scalacOptions in Test ++= Seq("-Yrangepos"),

    initialCommands in console :=
      """import org.globalnames.parser.{ScientificNameParser => SNP, _}
        |import SNP.instance._
        |import org.globalnames.parser.Parser._
        |import scala.util.{Failure, Success, Try}
        |import org.parboiled2._""".stripMargin
  )

lazy val benchmark = (project in file("./benchmark"))
  .dependsOn(`parser-render`)
  .enablePlugins(JmhPlugin)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-benchmark"
  )

lazy val runner = (project in file("./runner"))
  .dependsOn(`parser-render`)
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin, SbtTwirl)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-runner",
    executableScriptName := "gnparser",
    crossScalaVersions := Seq(scalaV11),
    packageName := "gnparser",
    bashScriptExtraDefines := Seq(
      s"""declare -r script_name="${executableScriptName.value}""""
    ),
    libraryDependencies ++= Seq(scopt, akkaHttp,
                                logbackClassic, scalaArm, akkaJson,
                                akkaHttpTestkit, scalatest, specs2core, specs2extra),
    mainClass in Compile := Some("org.globalnames.parser.runner.GnParser"),
    mainClass in reStart := Some("org.globalnames.parser.runner.web.controllers.WebServer"),
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "org.globalnames.runner",
    dockerRepository := Some("gnames")
  )

lazy val exampleJavaScala = (project in file("./examples/java-scala"))
  .dependsOn(`parser-render`)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-examples",
    crossScalaVersions := Seq(scalaV11)
  )

lazy val exampleSpark = (project in file("./examples/spark"))
  .dependsOn(`parser-render`)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-example-spark",
    crossScalaVersions := Seq(scalaV11),
    libraryDependencies ++= Seq(spark)
  )

lazy val sparkPython = (project in file("./spark-python"))
  .dependsOn(`parser-render`)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnparser-spark-python",
    crossScalaVersions := Seq(scalaV11),
    libraryDependencies ++= Seq(spark),
    projectDependencies := Seq(
      (projectID in parser).value.exclude("org.json4s", "json4s-jackson_2.10")
    )
  )
