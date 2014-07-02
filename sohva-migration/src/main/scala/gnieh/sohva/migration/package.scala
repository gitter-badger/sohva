/*
 * This file is part of the sohva project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.sohva

import net.liftweb.json.JObject

/** This package exposes the API to perform document migrations in a database.
 *
 *  @author Lucas Satabin
 */
package object migration {

  /** A migrator migrate a type of document to zero or more new documents.
   *  It is responsible for one type and its subtypes only.
   *  This kind of migrator works with raw json objects and are not typed
   *  at any moment.
   */
  type RawMigrator = JObject => List[JObject]

  /** A migrator migrate a type of document to zero or more new documents.
   *  It is responsible for one type and its subtypes only.
   */
  type TypedMigrator[T <: IdRev] = T => List[IdRev]

}
