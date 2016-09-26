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

import scala.concurrent.Future

import spray.client.pipelining._

import spray.httpx.unmarshalling._

import spray.http.StatusCodes

/** A show function that can be queried.
 *
 *  @author Lucas Satabin
 */
class Show(
    val design: String,
    val db: Database,
    val show: String) {

  import db.ec

  protected[this] def uri = db.uri / "_design" / design / "_show" / show

  /** Indicates whether this view exists */
  def exists: Future[Boolean] =
    for (h <- db.couch.rawHttp(Head(uri)))
      yield h.status == StatusCodes.OK

  /** Returns the result of querying the show function with the document with the given identifier
   *  or `None` for the `null` document.
   */
  def query[T: Unmarshaller](docId: Option[String] = None, format: Option[String] = None): Future[T] =
    for {
      resp <- db.couch.rawHttp(Get(uri / docId <<? format.map(f => ("format", f))))
    } yield resp.as[T] match {
      case Left(error) => throw new SohvaException(f"Unable to deserialize show result for show function $show and format $format: $error")
      case Right(v)    => v
    }

  override def toString =
    uri.toString

}
