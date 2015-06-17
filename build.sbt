name := "GnParser"
version := "1.0"
scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.3.1" % "test",
  "io.spray" %% "spray-json" % "1.3.2",
  "org.parboiled" %% "parboiled" % "2.1.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scalacOptions in Test ++= Seq("-Yrangepos")
