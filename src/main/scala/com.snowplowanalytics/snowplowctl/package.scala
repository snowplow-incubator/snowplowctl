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
package com.snowplowanalytics

import cats.data._
import cats.effect._

import com.snowplowanalytics.manifest.core.ManifestError
import com.snowplowanalytics.manifest.dynamodb.DynamoDbManifest

package object snowplowctl {
  /**
    * Base type for `ManifestAction` `MonadError`
    * Any IO error should be caught and casted to manifest's `IoError`
    * @tparam F effect type, usually IO, but can be replaced with pure
    */
  type BaseManifestF[F[_], A] = EitherT[F, ManifestError, A]

  /** Default Manifest: DynamoDB backend with IO */
  type ManifestClient = DynamoDbManifest[BaseManifestF[IO, ?]]

  /** Primary production IO action */
  type ManifestIO[A] = ReaderT[BaseManifestF[IO, ?], ManifestClient, A]
}
