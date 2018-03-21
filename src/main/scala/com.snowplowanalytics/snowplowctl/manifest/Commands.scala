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

import cats.data.{ReaderT, EitherT, NonEmptyList}
import cats.effect._
import cats.implicits._

import io.circe.syntax._

import com.snowplowanalytics.manifest.core._
import com.snowplowanalytics.manifest.core.ProcessingManifest.ItemId
import com.snowplowanalytics.manifest.dynamodb.DynamoDbManifest

import com.snowplowanalytics.snowplowctl.Common.readFile
import com.snowplowanalytics.snowplowctl.Config.ResolvableState
import com.snowplowanalytics.snowplowctl.manifest.Utils._

/** Functions responsible for subcommand execution */
object Commands {

  val SnowplowctlAuthor: Agent =
    Agent(generated.ProjectMetadata.name, generated.ProjectMetadata.version)

  def dumpManifest: ManifestIO[List[String]] =
    ReaderT { (manifest: ManifestClient) =>
      for { records <- manifest.list } yield records.map(_.asJson.noSpaces)
    }

  def createManifestTable: ManifestIO[Unit] =
    ReaderT { (manifest: ManifestClient) =>
      DynamoDbManifest.create[BaseManifestF[IO, ?]](manifest.client, manifest.primaryTable)
    }

  def resolve(itemId: String, resolvableState: ResolvableState): ManifestIO[Unit] =
    ReaderT { (manifest: ManifestClient) =>
      for {
        item <- manifest.getItem(itemId)
        check <- item match {
          case Some(i) =>
            EitherT.rightT[IO, ManifestError](StateCheck.inspect(i))
          case None =>
            EitherT.leftT[IO, StateCheck](invalidRequest(s"Item [$itemId] does not exist"))
        }
        _ <- check match {
          case StateCheck.Ok => EitherT.leftT[IO, StateCheck](invalidRequest(s"Item [$itemId] is not blocked"))
          case StateCheck.Blocked(record) =>
            val run = manifest.put(itemId, record.application, record.recordId.some, State.Resolved, SnowplowctlAuthor.some, None)
            val wrongState = EitherT.leftT[IO, Unit](invalidRequest(s"Item [$itemId] is in [${record.state.show}] state. Appropriate resolvable state should be provided"))
            record.state match {
              case State.Failed =>
                if (resolvableState == ResolvableState.Failed) run else wrongState
              case State.Processing =>
                if (resolvableState == ResolvableState.Processing) run else wrongState
              case state => EitherT.leftT[IO, Unit](invalidRequest(s"Item [$itemId] with state [${state.show}] cannot be resolved. Contact manifest administrator"))
            }
        }
      } yield ()
    }

  def skip(itemId: ItemId, name: String, version: Option[String], instanceId: Option[String]): ManifestIO[String] =
    ReaderT { (manifest: ManifestClient) =>
      val application = Application(Agent(name, version.getOrElse("*")), instanceId)
      val shortHand = s"${application.name}:${application.version}:${application.instanceId.getOrElse("*")}"
      for { _ <- manifest.put(itemId, application, none, State.Skipped, SnowplowctlAuthor.some, None) } yield shortHand
    }

  def delete(itemId: ItemId): ManifestIO[Int] =
    ReaderT { (manifest: ManifestClient) =>
      for {
        item <- manifest.getItemRecords(itemId)
        count <- NonEmptyList.fromList(item) match {
          case Some(records) =>
            records.reverse.traverse(manifest.deleteRecord).map(_.length)
          case None =>
           EitherT.leftT[IO, Int](invalidRequest(s"Item [$itemId] does not exist"))
        }
      } yield count
    }

  def importManifest(filePath: String): ManifestIO[Int] =
    ReaderT { (manifest: ManifestClient) =>
      val items = for {
        content <- EitherT.right[String](readFile(filePath))
        records <- linesToRecords(content.split("\n").toList)
        items   <- groupToItems(manifest.resolver)(records)
      } yield items

      for {
        list  <- items.leftMap(ManifestError.parseError)
        count <- list.traverse(putItem(manifest)).map(_.length)
      } yield count
    }

  def getItem(itemId: ItemId, json: Boolean): ManifestIO[String] =
    ReaderT { (manifest: ManifestClient) =>
      if (!json) { for {
        item <- manifest.getItem(itemId)
        message <- item match {
          case Some(i) => EitherT.pure[IO, ManifestError](Utils.showItem(i))
          case None => EitherT.leftT[IO, String](invalidRequest(s"Item [$itemId] does not exist"))
        }
      } yield message } else { for {
        records <- manifest.getItemRecords(itemId)
        message <- records match {
          case Nil => EitherT.leftT[IO, String](invalidRequest(s"Item [$itemId] does not exist"))
          case recs => EitherT.pure[IO, ManifestError](recs.map(_.asJson.noSpaces).mkString("\n"))
        }
      } yield message }
    }

}
