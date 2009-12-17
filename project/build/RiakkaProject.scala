import sbt._

class RiakkaProject(info: ProjectInfo) extends DefaultProject(info) {

  override def useDefaultConfigurations = true
  override def crossScalaVersions = Set("2.7.7", "2.8.0.Beta1-RC1")
  override def parallelExecution = true

  val dispatch = "net.databinder" %% "dispatch-lift-json" % "0.6.4"
  val slf4j_api = "org.slf4j" % "slf4j-api" % "1.5.6"
  val slf4j_simple = "org.slf4j" % "slf4j-simple" % "1.5.6"

  val scalatest = ScalaVersion.currentString match {
        case x: String if x.startsWith("2.7") => "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"
        case "2.8.0.Beta1-RC1" => "org.scalatest" % "scalatest" % "1.0.1-for-scala-2.8.0.Beta1-RC1-SNAPSHOT" % "test->default"
        case x => error("Unsupported Scala version " + x)
      }

  val codehaus = "codehaus repository" at "http://repository.codehaus.org/"
  val databinder_net = "databinder.net repository" at "http://www.databinder.net/repo"
  val scala_snapshots = "scala snapshots" at "http://scala-tools.org/repo-snapshots/"

}