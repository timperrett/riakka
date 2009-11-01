import sbt._

class RiakkaProject(info: ProjectInfo) extends DefaultProject(info) {

  override def useDefaultConfigurations = true
  
  val dispatch = "net.databinder" % "dispatch-http_2.7.5" % "0.5.0" // % "runtime->default"
  val scalatest = "org.scala-tools.testing" % "scalatest" % "0.9.5" // % "test->default"
  val configgy = "net.lag" % "configgy" % "1.3" intransitive()
  val json = "net.liftweb" % "lift-json" % "1.1-SNAPSHOT"

  val lag_net = "lag.net repository" at "http://www.lag.net/repo"
  val codehaus = "codehaus repository" at "http://repository.codehaus.org/"
  val scala_snapshots = "scala snapshots" at "http://scala-tools.org/repo-snapshots/"
  
}