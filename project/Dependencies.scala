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
import sbt._

object Dependencies {

  object V {
    // Scala
    val decline            = "0.4.0"
    val catsEffect         = "0.10"
    val processingManifest = "0.1.0-M5"
    val igluClient         = "0.5.0"
    val igluCore           = "0.2.0"
    val circe              = "0.9.3"
    val fs2                = "0.10.5"
    val scalaz7            = "7.0.9"
    val json4sJackson      = "3.2.11"
    // Scala (test only)
    val specs2             = "4.0.4"
    val scalaCheck         = "1.13.4"
  }

  // Scala
  val decline            = "com.monovore"          %% "decline"                      % V.decline
  val catsEffect         = "org.typelevel"         %% "cats-effect"                  % V.catsEffect
  val processingManifest = "com.snowplowanalytics" %% "snowplow-processing-manifest" % V.processingManifest
  val igluClient         = "com.snowplowanalytics" %% "iglu-scala-client"            % V.igluClient
  val igluCoreCirce      = "com.snowplowanalytics" %% "iglu-core-circe"              % V.igluCore
  val circe              = "io.circe"              %% "circe-core"                   % V.circe
  val circeJavaTime      = "io.circe"              %% "circe-java8"                  % V.circe
  val fs2                = "co.fs2"                %% "fs2-core"                     % V.fs2
  val fs2Io              = "co.fs2"                %% "fs2-io"                       % V.fs2
  val scalaz7            = "org.scalaz"            %% "scalaz-core"                  % V.scalaz7
  val json4sJackson      = "org.json4s"            %% "json4s-jackson"               % V.json4sJackson
  // Scala (test only)
  val specs2             = "org.specs2"            %% "specs2-core"                  % V.specs2         % "test"
  val scalaCheck         = "org.scalacheck"        %% "scalacheck"                   % V.scalaCheck     % "test"
}
