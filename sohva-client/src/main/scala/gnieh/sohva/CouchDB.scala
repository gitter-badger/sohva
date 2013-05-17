/*
* This file is part of the sohva project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*couch.http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package gnieh.sohva

import strategy._

/** A CouchDB instance.
 *  Allows users to access the different databases and information.
 *  This is the key class to start with when one wants to work with couchdb.
 *  Through this one you will get access to the databases.
 *
 *  @author Lucas Satabin
 *
 */
trait CouchDB {

  type Result[T]

  /** The couchdb instance host name. */
  val host: String

  /** The couchdb instance port. */
  val port: Int

  /** The couchdb instance version. */
  val version: String

  /** The Json (de)serializer */
  val serializer: JsonSerializer

  /** Returns the database on the given couch instance. */
  def database(name: String, credit: Int = 0, strategy: Strategy = BarneyStinsonStrategy): Database

  /** Returns the replicator database */
  def replicator(name: String = "_replicator", credit: Int = 0, strategy: Strategy = BarneyStinsonStrategy): Replicator

  /** Returns the names of all databases in this couch instance. */
  def _all_dbs: Result[List[String]]

  /** Returns the requested number of UUIDS (by default 1). */
  def _uuids(count: Int = 1): Result[List[String]]

  /** Indicates whether this couchdb instance contains the given database */
  def contains(dbName: String): Result[Boolean]

  /** Exposes the interface for managing couchdb users. */
  val users: Users

  protected[sohva] def passwordSha(password: String): (String, String)

}

// the different object that may be returned by the couchdb server

sealed trait DbResult

final case class OkResult(ok: Boolean, id: Option[String], rev: Option[String]) extends DbResult

final case class ErrorResult(id: Option[String], error: String, reason: String) extends DbResult

private[sohva] final case class Uuids(uuids: List[String])
