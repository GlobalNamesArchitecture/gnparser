organization := "org.globalnames"
name := "GnParser"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.7"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.globalnames.parser",
    test in assembly := {}
  )

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "org.littleshoot" % "littleshoot-commons-id" % "1.0.3",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "org.globalnames" %% "parboiled" % "2.2.0-2015.08.13-800a44ec7ee3565eab727996bcc7ccdcf69bced6-SNAPSHOT",
  "com.chuusai"     %% "shapeless" % "2.2.3",
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "org.specs2" %% "specs2-core" % "3.6.3" % "test"
)

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

scalacOptions in Test ++= Seq("-Yrangepos")

initialCommands in console :=
  """import org.globalnames.parser.{ScientificNameParser => SNP, _}
    |import scala.util.{Failure, Success, Try}
    |import org.parboiled2._""".stripMargin
