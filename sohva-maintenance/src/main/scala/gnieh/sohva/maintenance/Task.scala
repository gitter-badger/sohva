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
package maintenance

import scala.concurrent.duration.Duration

/** Implement this trait to benefit from the maintenance infrastructure.
 *  It provides some utilities to maintenance tasks.
 *
 *  @author Lucas Satabin
 */
abstract class Task[Result[_]] {

  /** This task name */
  def name: String

  /** The CouchDB instance, this purger is working on */
  val couch: CouchDB[Result]

  /** The timeout after which the purging process is aborted if not finished */
  val timeout: Duration

}
