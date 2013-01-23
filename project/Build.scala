
import sbt._
import Keys._

//import twirl.sbt.TwirlPlugin._


object Build extends Build {

  def general = Defaults.defaultSettings ++ Seq(
    organization := "com.stronglinks",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.9.2",
    resolvers ++= Seq(
      "spray repo" at "http://repo.spray.io",
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    )
  )
    
  lazy val root =
    Project("stator", file("."))
      .settings(general: _*)
      .settings(publish := (), publishLocal := ())
      .aggregate(statorApi, statorSbt)

  lazy val statorApi = 
    Project("stator-api", file("stator-api"))
      .settings(general: _*)
      .settings(libraryDependencies  +=
        "io.spray" % "twirl-api_2.9.2" % "0.6.0"
      )

  lazy val statorSbt =
    Project("stator-sbt", file("stator-sbt"))
      .settings(addSbtPlugin("io.spray" % "sbt-twirl" % "0.6.0"))
      .settings(general: _*)
      .settings(libraryDependencies  += "commons-io" % "commons-io" % "2.0")
      .settings(        
        Keys.sbtPlugin := true //,CrossBuilding.crossSbtVersions := Seq("0.12", "0.11.3", "0.11.2")
      )
      .dependsOn(statorApi)
}
