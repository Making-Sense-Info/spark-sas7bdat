import xerial.sbt.Sonatype.sonatypeCentralHost

name := "spark-sas7bdat"
organization := "info.makingsense"
description := "Spark data source for reading SAS sas7bdat files"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

// Maven Central (Central Portal) publishing settings
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeProfileName := "info.makingsense"

homepage := Some(url("https://github.com/Making-Sense-Info/spark-sas7bdat"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/Making-Sense-Info/spark-sas7bdat"),
    "scm:git@github.com:Making-Sense-Info/spark-sas7bdat.git"
  )
)
developers := List(
  Developer(
    id = "Making-Sense-Info",
    name = "Making Sense",
    email = "",
    url = url("https://github.com/Making-Sense-Info")
  )
)

scalaVersion := SparkVersions.Scala213Legacy
crossScalaVersions := Seq(
  SparkVersions.Scala211,
  SparkVersions.Scala212,
  SparkVersions.Scala213Legacy,
  SparkVersions.Scala213Spark4
)

lazy val sparkVersion = Def.setting {
  SparkVersions.resolveSparkVersion(
    scalaBinaryVersion.value,
    sys.props.get("spark.version")
  )
}

lazy val spark4Build = Def.setting {
  SparkVersions.isSpark4(sparkVersion.value)
}

version := SparkVersions.libraryVersion(sparkVersion.value)

scalacOptions ++= {
  if (spark4Build.value) Seq("-release:17")
  else Seq("-target:jvm-1.8")
}

javacOptions ++= {
  val v = if (spark4Build.value) "17" else "1.8"
  Seq(s"-source", v, s"-target", v, "-Xlint:-options")
}

Compile / run := Defaults.runTask(Compile / fullClasspath, Compile / mainClass, Compile / run / runner)

Test / parallelExecution := false
Test / fork := true

Test / javaOptions ++= {
  val common = Seq("-Duser.timezone=UTC")
  val moduleArgs =
    if (sys.props.get("java.specification.version").exists(_.split("\\.").headOption.exists(_.toInt >= 9))) {
      Seq(
        "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-exports=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-exports=java.base/sun.util.calendar=ALL-UNNAMED",
        "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
      )
    } else {
      Seq.empty
    }
  common ++ moduleArgs
}

libraryDependencies ++= {
  val scalaBin = scalaBinaryVersion.value
  val scalatestVersion = if (scalaBin == "2.11") "3.1.2" else "3.2.19"
  val log4jApiScalaVersion = if (scalaBin == "2.11") "12.0" else "13.1.0"
  Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion.value % Provided,
    "org.apache.spark" %% "spark-sql" % sparkVersion.value % Provided,
    "com.epam" % "parso" % "2.0.14",
    "org.apache.logging.log4j" %% "log4j-api-scala" % log4jApiScalaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test
  )
}

// sbt -Dspark.version=4.1.2 spark41Compile  (requires JDK 17+)
addCommandAlias(
  "spark41Compile",
  s"++${SparkVersions.Scala213Spark4} compile"
)
addCommandAlias(
  "spark41Test",
  s"++${SparkVersions.Scala213Spark4} test"
)

assembly / test := {}
// Fat JAR for PySpark / spark-submit --jars (includes Parso; Spark remains Provided).
// Thin JAR from `package` / Maven publish keeps the default name without -assembly.
assembly / assemblyJarName :=
  s"${name.value}-${version.value}-s_${scalaBinaryVersion.value}-assembly.jar"
assembly / assemblyMergeStrategy := {
  case PathList("module-info.class")              => MergeStrategy.discard
  case PathList("META-INF", "versions", _ @ _*) => MergeStrategy.first
  case PathList("META-INF", "MANIFEST.MF")       => MergeStrategy.discard
  case x                                          => (assembly / assemblyMergeStrategy).value(x)
}

artifactName := { (sv: ScalaVersion, module: ModuleID, art: Artifact) =>
  val base = s"${name.value}-${module.revision}-s_${sv.binary}"
  art.classifier match {
    case Some(classifier) => s"$base-$classifier.${art.extension}"
    case None             => s"$base.${art.extension}"
  }
}

publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishConfiguration := publishConfiguration.value.withOverwrite(true)
pomIncludeRepository := { _ => false }
publishMavenStyle := true

Compile / packageBin / mappings ++= Seq(
  file("LICENSE") -> "LICENSE",
  file("NOTICE") -> "NOTICE"
)
