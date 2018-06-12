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
package com.snowplowanalytics.snowplowctl
package manifest

import cats.data._
import cats.effect._
import cats.implicits._

import io.circe.parser.parse

import com.snowplowanalytics.iglu.client.Resolver

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.auth.profile.ProfileCredentialsProvider

import com.snowplowanalytics.manifest.core.{Application, Item, ManifestError, Record, State, StateCheck}
import com.snowplowanalytics.manifest.core.Common.DefaultResolver
import com.snowplowanalytics.manifest.dynamodb.DynamoDbManifest

import com.snowplowanalytics.snowplowctl.SnowplowCtlCommand.AwsConfig


object Utils {

  /** Create Processing manifest client, with custom resolver config (or with embedded schemas only) */
  def getClient(tableName: String,
                resolverConfig: Option[String],
                awsConfig: AwsConfig): IO[ManifestClient] = {
    val standard = AmazonDynamoDBClientBuilder.standard()
    val builder = awsConfig.awsRegion.fold(standard)(standard.withRegion)
    val client = awsConfig.awsProfile.fold(builder)(p => builder.withCredentials(new ProfileCredentialsProvider(p))).build()

    for {
      dynamoClient <- IO(client)
      resolver <- resolverConfig match {
        case Some(path) => Common.getResolver(path)
        case None => IO.pure(DefaultResolver)
      }
      manifestClient = DynamoDbManifest[BaseManifestF[IO, ?]](dynamoClient, tableName, resolver)
    } yield manifestClient
  }

  /** Get human-readable representation of Manifest Item */
  def showItem(item: Item): String = {
    val recordsShow = item.records.toList.flatMap { r =>
      val state = r.state match {
        case State.Processing =>
          val consumation = item
            .records
            .find(r2 => r2.previousRecordId.contains(r.recordId))
            .map(s => s"${s.state.show} at ${s.timestamp}")
            .getOrElse("UNCONSUMED")
          s"PROCESSING ${r.timestamp} ($consumation)".some
        case _ => None
      }
      state.map { s => (s"${app(r.application)}", s) }
    }

    val max = recordsShow.map(_._1.length).maximumOption.getOrElse(0)

    val skipped = item.records.filter(_.state == State.Skipped).map(r => app(r.application)) match {
      case Nil => ""
      case some => s"Skipped for: ${some.mkString(", ")}"
    }

    val status = StateCheck.inspect(item) match {
      case StateCheck.Ok => ""
      case StateCheck.Blocked(record) =>
        val payload = record.payload.map(p => s"PAYLOAD: ${p.data.noSpaces}").getOrElse("")
        val blocked = s"BLOCKED by ${record.state.show} from ${record.timestamp}"
        List(blocked, payload).filterNot(_.isEmpty).mkString("\n")
    }

    val newRecord = item.records
      .find(_.state == State.New)
      .map(r => s"Added by ${app(r.application)} at ${r.timestamp}")
      .getOrElse("")

    val meta = List(newRecord, status, skipped).filterNot(_.isEmpty).mkString("\n")

    s"""Item [${item.id}]
       |$meta
       |${recordsShow.map(showCols(max)).mkString("\n")}""".stripMargin
  }

  def invalidRequest(message: String): ManifestError =
    ManifestError.Corrupted(ManifestError.Corruption.InvalidRequest(message))

  def linesToRecords(lines: List[String]): EitherT[IO, String, List[Record]] = EitherT.fromEither[IO] {
    val records = lines.map(line => parse(line).flatMap(_.as[Record]))
    records.sequence[Either[io.circe.Error, ?], Record].leftMap(_.show)
  }

  def groupToItems(resolver: Resolver)(records: List[Record]): EitherT[IO, String, List[Item]] = EitherT.fromEither {
    val grouped = records.groupByNel(_.itemId)
    val items = grouped.map { case (_, rs) => Item(rs) }
    items.toList.traverse(_.ensure(resolver)).leftMap(_.show)
  }

  def putItem(manifest: ManifestClient)(item: Item) = for {
    _ <- item.records.traverse(manifest.putRecord)
    _ <- EitherT.right[ManifestError](IO(println(s"Added ${item.records.length} records for item ${item.id}")))
  } yield ()

  private def app(application: Application): String = {
    val instance = application.instanceId.map(x => s":$x").getOrElse("")
    s"${application.name}:${application.version}$instance"
  }

  private def showCols(max: Int)(cols: (String, String)) = {
    val colLen = cols._1.length
    val pad = if (colLen < max) cols._1 + (" " * (max - colLen)) else cols._1
    s"+ $pad ${cols._2}"
  }
}
