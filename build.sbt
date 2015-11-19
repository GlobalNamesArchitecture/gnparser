import sbt.Keys._

val commonSettings = Seq(
  version := "0.2.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  organization := "org.globalnames",
  homepage := Some(new URL("http://globalnames.org/")),
  description := "Fast and elegant parser for taxonomic scientific names",
  startYear := Some(2008),
  licenses := Seq("MIT" -> new URL("https://github.com/GlobalNamesArchitecture/gnparser/blob/master/LICENSE")),
  resolvers ++= Seq(
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "bintray-fge"    at "https://dl.bintray.com/fge/maven/"
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
          <url>http://myltsev.name</url>
        </developer>
      </developers>)

val noPublishingSettings = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

/////////////////////// DEPENDENCIES /////////////////////////

val json4s      = "org.json4s"         %% "json4s-jackson"         % "3.2.11"
val javaUuid    = "com.fasterxml.uuid" %  "java-uuid-generator"    % "3.1.3"
val lang3       = "org.apache.commons" %  "commons-lang3"          % "3.4"
val parboiled   = "org.globalnames"    %% "parboiled"              % "2.2.1"
val scalaz      = "org.scalaz"         %% "scalaz-core"            % "7.1.3"
val specs2core  = "org.specs2"         %% "specs2-core"            % "3.6.3" % Test
val jsvalidate  = "com.github.fge"     %  "json-schema-validator"  % "2.2.6" % Test

/////////////////////// PROJECTS /////////////////////////

lazy val root = project.in(file("."))
  .aggregate(parser, examples, runner, web)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.10.3", "2.11.7")
  )

lazy val parser = (project in file("./parser"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnparser",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.globalnames.parser",
    test in assembly := {},

    libraryDependencies ++= {
      val shapeless = scalaVersion.value match {
        case v if v.startsWith("2.10") =>
          Seq("com.chuusai" % "shapeless_2.10.4" % "2.1.0",
              compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
        case _ =>
          Seq("com.chuusai" %% "shapeless" % "2.2.3")
      }
      shapeless ++ Seq(json4s, javaUuid, lang3, parboiled, scalaz, specs2core,
                       jsvalidate)
    },

    scalacOptions in Test ++= Seq("-Yrangepos"),

    initialCommands in console :=
      """import org.globalnames.parser.{ScientificNameParser => SNP, _}
        |import scala.util.{Failure, Success, Try}
        |import org.parboiled2._""".stripMargin
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
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-runner",
    executableScriptName := "gnparse",
    packageName := "gnparser",
    bashScriptExtraDefines := Seq(
      s"""declare -r script_name="${executableScriptName.value}""""
    ),
    mainClass in Compile := Some("org.globalnames.GnParser")
  )

lazy val examples = (project in file("./examples/java-scala"))
  .dependsOn(parser)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-examples"
  )

lazy val web = (project in file("./web"))
  .dependsOn(parser)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(noPublishingSettings: _*)
  .settings(
    name := "gnparser-web",
    packageName := "gnparser-web",
    pipelineStages := Seq(digest, gzip),
    libraryDependencies ++= Seq(specs2 % Test)
  )
