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

import cats.data._
import cats.implicits._

import com.monovore.decline._

object Config {
  /** Processing Manifest states that can be resolved using RESOLVED record */
  sealed trait ResolvableState

  object ResolvableState {
    case object Processing extends ResolvableState
    case object Failed extends ResolvableState
  }

  implicit val resolvableStateArg: Argument[ResolvableState] =
    new Argument[ResolvableState] {
      def read(string: String): ValidatedNel[String, ResolvableState] = string match {
        case "FAILED" => ResolvableState.Failed.validNel
        case "PROCESSING" => ResolvableState.Processing.validNel
        case other => s"State [$other] is not valid recoverable state. Should be FAILED or PROCESSING".invalidNel
      }

    def defaultMetavar = "STATE"
  }
}
