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

import net.liftweb.json._
import java.text.SimpleDateFormat

case class SohvaJsonException(msg: String, inner: Exception) extends Exception(msg, inner)

/** The interface for the Json serializer/deserializer.
 *  Allows for changing the implementation and using your favorite
 *  json library.
 *
 *  @author Lucas Satabin
 *
 */
class JsonSerializer(couch: CouchDB, custom: List[CustomSerializer[_]]) {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS")
  } + new UserSerializer(couch) + new SecurityDocSerializer(couch.version) ++ custom.map(_.serializer(couch.version))

  import Implicits._

  /** Serializes the given object to a json string */
  def toJson[T: Manifest](obj: T) = obj match {
    case i: Int => compact(render(JInt(i)))
    case i: BigInt => compact(render(JInt(i)))
    case l: Long => compact(render(JInt(l)))
    case d: Double => compact(render(JDouble(d)))
    case f: Float => compact(render(JDouble(f)))
    case d: BigDecimal => compact(render(JDouble(d.doubleValue)))
    case b: Boolean => compact(render(JBool(b)))
    case s: String => compact(render(JString(s)))
    case _ => compact(render(Extraction.decompose(obj)))
  }

  /** Deserializes from the given json string to the object if possible or throws a
   *  `SohvaJsonExpcetion`
   */
  def fromJson[T: Manifest](json: String): T =
    try {
      Serialization.read[T](json)
    } catch {
      case e: Exception =>
        throw SohvaJsonException("Unable to extract from the json string \"" + json + "\"", e)
    }

  /** Deserializes from the given json string to the object if possible or returns
   *  `None` otherwise
   */
  def fromJsonOpt[T: Manifest](json: String) =
    Extraction.extractOpt(JsonParser.parse(json))

}

/** The schema of user document is more flexible now, but in couchdb pre 1.2
 *  one had to compute the password salt and SHA himself
 *
 *  @author Lucas Satabin */
private class UserSerializer(couch: CouchDB) extends Serializer[CouchUser] {
  private val CouchUserClass = classOf[CouchUser]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), CouchUser] = {
    case (TypeInfo(CouchUserClass, _), _) => throw new MappingException("CouchUser should never be deserialized")
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case user: CouchUser if couch.version < "1.2" =>
      val (salt, password_sha) = couch.passwordSha(user.password)
      JObject(List(
        JField("_id", JString(user._id)),
        JField("_rev", user._rev.map(r => JString(r)).getOrElse(JNothing)),
        JField("name", JString(user.name)),
        JField("type", JString("user")),
        JField("roles", JArray(user.roles map (r => JString(r)))),
        JField("password_sha", JString(password_sha)),
        JField("salt", JString(salt))
      ))
  }

}

/** Before couchdb 1.2, the `members` field of security documents was named `readers`
 *
 *  @author Lucas Satabin */
private class SecurityDocSerializer(version: String) extends Serializer[SecurityDoc] {

  private val SecurityDocClass = classOf[SecurityDoc]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), SecurityDoc] = {
    case (TypeInfo(SecurityDocClass, _), json) if version < "1.2" =>
      val members = (json \ "readers").extract[SecurityList]
      val admins = (json \ "admins").extract[SecurityList]
      SecurityDoc(admins, members)
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case security: SecurityDoc if version < "1.2" =>
      JObject(List(
        JField("readers", Extraction.decompose(security.members)),
        JField("admins", Extraction.decompose(security.admins))
      ))
  }
}

/** Implement this trait to define a custom serializer that may
 *  handle object differently based on the CouchDB version
 *
 *  @author Lucas Satabin */
trait CustomSerializer[T] {
  def serializer(couchVersion: String): Serializer[T]
}
