/*
 * Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

// SBT
import sbt._
import Keys._

// sbt-bintray
import bintray._
import bintray.BintrayPlugin._
import bintray.BintrayKeys._

// sbt-assembly
import sbtassembly._
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin.defaultShellScript


/**
 * Common settings-patterns for Snowplow apps and libaries.
 * To enable any of these you need to explicitly add Settings value to build.sbt
 */
object BuildSettings {

  // Makes package (build) metadata available withing source code
  lazy val scalifySettings = Seq(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "settings.scala"
    IO.write(file, """package com.snowplowanalytics.snowplowctl.generated
                      |object ProjectMetadata {
                      |  val version = "%s"
                      |  val name = "%s"
                      |  val organization = "%s"
                      |  val scalaVersion = "%s"
                      |}
                      |""".stripMargin.format(version.value, name.value, organization.value, scalaVersion.value))
      Seq(file)
    }.taskValue
  )

  lazy val buildSettings = Seq[Setting[_]](
    scalacOptions := Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused",
      "-Ywarn-value-discard",
      "-Ypartial-unification",
      "-language:higherKinds"
    ),
    javacOptions := Seq(
      "-source", "1.8",
      "-target", "1.8",
      "-Xlint"
    )
  )

  // sbt-assembly settings
  lazy val assemblySettings = Seq(
    assemblyJarName in assembly := { name.value },
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", _ @ _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    assemblyOption in assembly ~= { _.copy(prependShellScript = Some(defaultShellScript)) }
  )

  lazy val fpAddons = Seq(
    resolvers += Resolver.sonatypeRepo("releases"),

    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")
  )

  lazy val helpersSettings = Seq[Setting[_]](
    initialCommands := "import com.snowplowanalytics.snowplowctl._"
  )

  // Bintray publish settings
  lazy val publishSettings = bintraySettings ++ Seq[Setting[_]](
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayOrganization := Some("snowplow"),
    bintrayRepository := "snowplow-generic",
    publishMavenStyle := false,

    // Custom Bintray resolver used to publish package with custom Ivy patterns (custom path in Bintray)
    // It requires ~/.bintray/credentials file and bintrayOrganization setting
    publishTo in bintray := {
      for {
        bintrayOrg     <- bintrayOrganization.value
        credentials    <- BintrayCredentials.read(bintrayCredentialsFile.value)
        bintrayRepo     = BintrayRepo(credentials, Some(bintrayOrg), name.value)
        connectedRepo   = bintrayRepo.client.repo(bintrayOrg, bintrayRepository.value)
        bintrayPackage  = connectedRepo.get(name.value)
        ivyResolver     = BintrayIvyResolver(
          bintrayRepository.value,
          bintrayPackage.version(version.value),
          release = true)
      } yield new RawRepository(ivyResolver, "Raw Repository")
    }
  )
}
