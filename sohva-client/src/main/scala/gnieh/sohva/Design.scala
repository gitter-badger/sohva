/*
* This file is part of the sohva project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package gnieh.sohva

import spray.httpx.unmarshalling.Unmarshaller

import scala.language.higherKinds

/** A design gives access to the different views.
 *  Use this class to get or create new views.
 *
 *  @author Lucas Satabin
 */
trait Design[Result[_]] {

  val name: String
  val language: String

  /** Check if the design exists.
   *
   *  @return true if it does, false otherwise
   */
  def exists: Result[Boolean]

  /** Create an empty design document if none exists.
   *  Raises an exception if the design already exists.
   *
   *  @return the design document if created..
   */
  def create: Result[DesignDoc]

  /** Returns the design document from the couchdb instance.
   *  Returns `None` if the design document does not exist.
   */
  def getDesignDocument: Result[Option[DesignDoc]]

  /** Deletes this design document from the couchdb instance */
  def delete: Result[Boolean]

  /** Creates or updates the view in this design
   *  with the given name, map function and reduce function.
   *  If the design does not exist yet, it is created.
   */
  def saveView(viewName: String,
    mapFun: String,
    reduceFun: Option[String] = None): Result[Unit]

  /** Creates or updates the view in this design with the given name.
   *  If the design does not exist yet, it is created.
   */
  def saveView(viewName: String, view: ViewDoc): Result[Unit]

  /** Deletes the view with the given name from the design */
  def deleteView(viewName: String): Result[Unit]

  /** Returns the view in this design document. */
  def view(viewName: String): View[Result]

  /** Creates or update the show function in this design with the given name.
   *  If the design does not exist yet, it is created.
   */
  def saveShow(showName: String, show: String): Result[Unit]

  /** Deletes the show function with the given name from the design */
  def deleteShow(showName: String): Result[Unit]

  /** Returns representation of the show function for this design. */
  def show(showName: String): Show[Result]

  /** Creates or update the update function in this design with the given name.
   *  If the design does not exist yet, it is created.
   */
  def saveList(listName: String, list: String): Result[Unit]

  /** Deletes the list function with the given name from the design */
  def deleteList(listName: String): Result[Unit]

  /** Returns representation of the list function with the given view. */
  def list(listName: String): CList[Result]

  /** Creates or update the update function in this design with the given name.
   *  If the design does not exist yet, it is created.
   */
  def saveUpdate(updateName: String, update: String): Result[Unit]

  /** Deletes the update function with the given name from the design */
  def deleteUpdate(updateName: String): Result[Unit]

  /** Returns representation of the update function with the given view. */
  def update(updateName: String): Update[Result]

  /** Creates or updates the document validation function.
   *  If the design does not exist yet, it is created.
   */
  def saveValidateFunction(validateFun: String): Result[Unit]

  /** Deletes the document validation function from the design */
  def deleteValidateFunction: Result[Unit]

  /** Creates or updates a filter function.
   *  If the design does not exist yet, it is created.
   */
  def saveFilter(name: String, filterFun: String): Result[Unit]

  /** Deletes the filter identified by its name from the design document */
  def deleteFilter(name: String): Result[Unit]

  /** Creates or updates the list of rewrite rules.
   *  If the design does not exist yet, it is created.
   */
  def saveRewriteRules(rules: List[RewriteRule]): Result[Unit]

  /** Retrieves the rewrite rules associated to this design document. */
  def getRewriteRules(): Result[List[RewriteRule]]

}

case class DesignDoc(
  _id: String,
  language: String,
  views: Map[String, ViewDoc],
  validate_doc_update: Option[String],
  updates: Map[String, String],
  filters: Map[String, String],
  shows: Map[String, String],
  lists: Map[String, String],
  rewrites: List[RewriteRule]) extends IdRev

case class RewriteRule(from: String, to: String, method: String, query: Map[String, String])
