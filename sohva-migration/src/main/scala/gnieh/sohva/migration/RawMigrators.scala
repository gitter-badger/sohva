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
package migration

import net.liftweb.json.{
  JObject,
  JField
}

/** Set of common useful migrators
 *
 *  @author Lucas Satabin
 */
trait RawMigrators {

  def addFields(creators: (JObject => JField)*): RawMigrator = {
    case doc @ JObject(fields) =>
      List(JObject(fields ++ creators.map(_(doc))))
  }

  def dropFields(first: JObject => String, rest: (JObject => String)*): RawMigrator = {
    case doc @ JObject(fields) =>
      val toRemove = (first +: rest).map(_(doc))
      List(JObject(fields.filterNot(f => toRemove.contains(f.name))))
  }

  def dropFields(first: String, rest: String*): RawMigrator = {
    case doc @ JObject(fields) =>
      val toRemove = first +: rest
      List(JObject(fields.filterNot(f => toRemove.contains(f.name))))
  }

  def split(f: JObject => List[JObject]): RawMigrator = f

}
