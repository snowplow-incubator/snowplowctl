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

import cats.effect._

import org.json4s.jackson.JsonMethods.parse

import com.snowplowanalytics.iglu.client.Resolver

object Common {

  def readFile(path: String): IO[String] =
    IO(scala.io.Source.fromFile(path).mkString)

  def getResolver(path: String): IO[Resolver] =
    for {
      content <- readFile(path)
      json <- IO(parse(content))
      resolver <- Resolver.parse(json).toEither match {
        case Left(errors) => IO.raiseError(new RuntimeException(errors.map(_.getMessage).list.mkString(", ")))
        case Right(r) => IO.pure(r)
      }
    } yield resolver
}
