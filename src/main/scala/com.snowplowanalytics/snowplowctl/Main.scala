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

import cats.effect.IO
import cats.implicits._

import SnowplowCtlCommand._
import manifest.Utils
import manifest.Commands._

object Main {

  def main(args: Array[String]): Unit = {
    SnowplowCtlCommand.parse(args) match {
      case Right(m @ ManifestCommand(_, Dump, _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(dumpManifest)
      case Right(m @ ManifestCommand(_, Create, _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(createManifestTable)
      case Right(m @ ManifestCommand(_, Resolve(itemId, resolvableState), _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(resolve(itemId, resolvableState))
      case Right(m @ ManifestCommand(_, SkipItem(itemId, app, version, instance), _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(skip(itemId, app, version, instance))
      case Right(m @ ManifestCommand(_, DeleteItem(itemId), _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(delete(itemId))
      case Right(m @ ManifestCommand(_, Import(path), _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(importManifest(path))
      case Right(m @ ManifestCommand(_, GetItem(itemId, json), _, _)) =>
        execute(m.table, m.resolver, m.awsConfig)(getItem(itemId, json))
      case Right(m @ ManifestCommand(_, Query(processedBy, requestedBy), _, _)) =>
        println(m.toString).unsafeRunSync()
        execute(m.table, m.resolver, m.awsConfig)(query(processedBy, requestedBy))
      case Right(ShowVersion) =>
        execute(println(generated.ProjectMetadata.version))
      case Left(help) => System.err.println(help)
    }
  }

  private[this] def execute(tableName: String, resolver: Option[String], awsConfig: AwsConfig)(io: ManifestIO[Unit]): Unit = {
    val result = for {
      client <- Utils.getClient(tableName, resolver, awsConfig)
      result <- io.run(client).value
    } yield result
    result.unsafeRunSync() match {
      case Right(_) => ()
      case Left(error) =>
        System.err.println(error.show)
        System.exit(1)
    }
  }

  private[this] def execute(io: IO[Unit]): Unit =
    io.unsafeRunSync()

}
