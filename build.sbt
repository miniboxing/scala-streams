import JmhKeys._

name := "scala-streams"

version := "1.0"

scalaVersion := "2.11.4"

// scalacOptions ++= Seq("-optimise", "-Yclosure-elim", "-Yinline")

// dependencies:

resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots"))

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
  "org.scala-lang" % "scala-reflect" % "2.11.4"
)

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

// jvm options:

// unmanagedJars in Compile := Seq(Attributed.blank(file("jmh-profilers/target/jmh-profilers-0.1.jar")))

javaOptions in run ++= Seq("-XX:+UnlockCommercialFeatures", 
			   "-XX:+FlightRecorder",
			   "-Xms3G" , 
			   "-verbose:gc", 
			   "-XX:-UseAdaptiveSizePolicy")

// test options

testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, 
				      "-maxSize", "5", 
				      "-minSuccessfulTests", 
				      "100", "-workers", "1", 
				      "-verbosity", "1")

// jmh:

jmhSettings

// eclipse:

com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys.withSource := true

// miniboxing:

libraryDependencies += "org.scala-miniboxing.plugins" %% "miniboxing-runtime" % "0.4-SNAPSHOT"

addCompilerPlugin("org.scala-miniboxing.plugins" %% "miniboxing-plugin" % "0.4-SNAPSHOT")

scalacOptions ++= Seq("-P:minibox:warn", "-P:minibox:mark-all")
