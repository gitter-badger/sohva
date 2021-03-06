import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

lazy val globalSettings = Seq(
  resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  organization := "org.gnieh",
  licenses += ("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/gnieh/sohva")),
  version := "2.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  libraryDependencies ++= globalDependencies,
  parallelExecution := false,
  fork in Test := true,
  scalacOptions ++= Seq("-deprecation", "-feature")
)

lazy val scalariform = scalariformSettings ++ Seq(
  ScalariformKeys.preferences :=
    ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
)

lazy val globalDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.10",
  "org.gnieh" %% "diffson" % "2.0.2",
  "io.spray" %% "spray-json" % "1.3.2",
  "org.slf4j" % "slf4j-api" % "1.7.21"
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  // The Nexus repo we're publishing to.
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { x => false },
  pomExtra := (
    <scm>
      <url>https://github.com/gnieh/sohva</url>
      <connection>scm:git:git://github.com/gnieh/sohva.git</connection>
      <developerConnection>scm:git:git@github.com:gnieh/sohva.git</developerConnection>
      <tag>HEAD</tag>
    </scm>
    <developers>
      <developer>
        <id>satabin</id>
        <name>Lucas Satabin</name>
        <email>lucas.satabin@gnieh.org</email>
      </developer>
    </developers>
    <ciManagement>
        <system>travis</system>
        <url>https://travis-ci.org/#!/gnieh/sohva</url>
      </ciManagement>
      <issueManagement>
        <system>github</system>
        <url>https://github.com/gnieh/sohva/issues</url>
      </issueManagement>
    )
  )

lazy val sohva = project.in(file("."))
  .dependsOn(json % "compile-internal, test-internal")
  .enablePlugins(SiteScaladocPlugin, JekyllPlugin)
  .settings(globalSettings: _*)
  .settings(publishSettings: _*)
  .settings(osgiSettings: _*)
  .settings(scalariformSettings: _*)
  .settings (
    name := "sohva",
    description := "Couchdb client library",
    fork in test := true,
    mappings in (Compile, packageBin) ++= mappings.in(json, Compile, packageBin).value,
    mappings in (Compile, packageSrc) ++= mappings.in(json, Compile, packageSrc).value,
    sources in (Compile, doc) ++= sources.in(json, Compile, doc).value,
    com.typesafe.sbt.site.jekyll.JekyllPlugin.autoImport.requiredGems := Map(
        "jekyll" -> "3.3.0"),
    resourceDirectories in Compile := List(),
    OsgiKeys.exportPackage := Seq(
      "gnieh.sohva",
      "gnieh.sohva.*"
    ),
    OsgiKeys.additionalHeaders := Map (
      "Bundle-Name" -> "Sohva CouchDB Client"
    ),
    OsgiKeys.bundleSymbolicName := "org.gnieh.sohva",
    OsgiKeys.privatePackage := Seq())

lazy val json = project.in(file("sohva-json"))
  .settings(globalSettings: _*)
  .settings(
    libraryDependencies ++= globalDependencies,
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _))
