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
package async
package maintenance

import scala.concurrent.duration.Duration
import scala.concurrent.Future

import gnieh.sohva.maintenance._

class DbPurger(val couch: CouchDB, val timeout: Duration) extends Task with gnieh.sohva.maintenance.DbPurger[Future] {

  import couch.ec

  def name = "purge"

  def purge(database: String, until: Option[String] = None): Future[Unit] = {
    val db = couch.database(database)
    for {
      now <- sanityChecks(db)
      uuid <- couch._uuid
      lock <- takeMaintenanceLock(db, uuid, now)
      now <- sanityChecks(db)
    } yield ()
  }

}
