lazy val root = project.in(file("."))
  .settings(
    name := "snowplowctl",
    version := "0.1.0-rc2",
    organization := "com.snowplowanalytics",
    scalaVersion := "2.11.12",
    initialCommands := "import com.snowplowanalytics.snowplowctl._"
  )
  .settings(BuildSettings.assemblySettings)
  .settings(BuildSettings.buildSettings)
  .settings(BuildSettings.scalifySettings)
  .settings(BuildSettings.fpAddons)
  .settings(BuildSettings.publishSettings)
  .settings(
    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    ),
    libraryDependencies ++= Seq(
      Dependencies.decline,
      Dependencies.catsEffect,
      Dependencies.processingManifest,
      Dependencies.igluClient,
      Dependencies.igluCoreCirce,
      Dependencies.circe,
      Dependencies.circeJavaTime,
      Dependencies.fs2,
      Dependencies.fs2Io,

      Dependencies.specs2,
      Dependencies.scalaCheck
    )
  )
  .settings(BuildSettings.helpersSettings)

