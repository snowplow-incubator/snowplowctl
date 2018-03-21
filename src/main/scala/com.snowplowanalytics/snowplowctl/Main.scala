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

import cats.implicits._

import com.snowplowanalytics.snowplowctl.manifest.{ Commands, Utils }

object Main {
  def main(args: Array[String]): Unit = {
    SnowplowCtlCommand.snowplowCtl.parse(args) match {
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.Dump, resolver)) =>
        val records = for {
          client <- Utils.getClient(tableName, resolver)
          records <- Commands.dumpManifest.run(client).value
        } yield records
        records.unsafeRunSync() match {
          case Right(jsons) =>
            println(jsons.mkString("\n"))
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.Create, resolver)) =>
        val result = for {
          client <- Utils.getClient(tableName, resolver)
          created <- Commands.createManifestTable.run(client).value
        } yield created
        result.unsafeRunSync() match {
          case Right(_) =>
            println(s"DynamoDB table [$tableName] successfully created!")
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.Resolve(itemId, resolvableState), resolver)) =>
        val result = for {
          client <- Utils.getClient(tableName, resolver)
          status <- Commands.resolve(itemId, resolvableState).run(client).value
        } yield status
        result.unsafeRunSync() match {
          case Right(_) =>
            println(s"Item [$itemId] with [$resolvableState] has been resolved successfully")
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.SkipItem(itemId, app, version, instance), resolver)) =>
        val result = for {
          client <- Utils.getClient(tableName, resolver)
          status <- Commands.skip(itemId, app, version, instance).run(client).value
        } yield status
        result.unsafeRunSync() match {
          case Right(shortHand) =>
            println(s"Item [$itemId] has been successfully skipped for [$shortHand]")
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.DeleteItem(itemId), resolver)) =>
        val result = for {
          client <- Utils.getClient(tableName, resolver)
          status <- Commands.delete(itemId).run(client).value
        } yield status
        result.unsafeRunSync() match {
          case Right(count) =>
            println(s"Item [$itemId] with $count records was deleted successfully")
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.Import(path), resolver)) =>
        val result = for {
          client <- Utils.getClient(tableName, resolver)
          status <- Commands.importManifest(path).run(client).value
        } yield status
        result.unsafeRunSync() match {
          case Right(count) =>
            println(s"$count items were imported from $path")
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }
      case Right(SnowplowCtlCommand.ManifestCommand(tableName, SnowplowCtlCommand.GetItem(itemId, json), resolver)) =>
        val result = for {
          client <- Utils.getClient(tableName, resolver)
          message <- Commands.getItem(itemId, json).run(client).value
        } yield message
        result.unsafeRunSync() match {
          case Right(message) =>
            println(message)
          case Left(error) =>
            System.err.println(error.show)
            System.exit(1)
        }

      case Right(SnowplowCtlCommand.ShowVersion) => println(generated.ProjectMetadata.version)
      case Right(command) => println(command)
      case Left(help) => System.err.println(help)
    }
  }
}
