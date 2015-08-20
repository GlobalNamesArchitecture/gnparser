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
  "commons-lang" % "commons-lang" % "2.6",
  "org.globalnames" %% "parboiled" % "2.2.0-cca58cba7126660158f6aa45f1cb4e1bdb8ddfe6-SNAPSHOT",
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
