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
package control

import gnieh.sohva.async.{
  Update => AUpdate
}

import spray.httpx.unmarshalling.Unmarshaller

import spray.json._

import scala.util.Try

class Update(val wrapped: AUpdate) extends gnieh.sohva.Update[Try] {

  def exists: Try[Boolean] =
    synced(wrapped.exists)

  def query[Body: RootJsonWriter, Resp: Unmarshaller](body: Body, docId: Option[String] = None, parameters: Map[String, String] = Map()): Try[Resp] =
    synced(wrapped.query[Body, Resp](body, docId, parameters))

  def queryForm[Resp: Unmarshaller](data: Map[String, String], docId: String, parameters: Map[String, String] = Map()): Try[Resp] =
    synced(wrapped.queryForm[Resp](data, docId, parameters))

}
