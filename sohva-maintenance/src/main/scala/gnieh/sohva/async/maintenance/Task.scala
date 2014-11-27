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

import scala.concurrent.Future

import gnieh.sohva.maintenance.{
  MaintenanceDoc,
  MaintenanceException
}

abstract class Task extends gnieh.sohva.maintenance.Task[Future] {

  val couch: CouchDB

  import couch.ec

  private def allCompleted[T](seq: Seq[Future[T]]): Future[Unit] =
    Future.fold(seq)(()) { (_, _) => () }

  protected def sanityChecks(db: Database): Future[Long] =
    db.info.flatMap {
      case Some(InfoResult(true, _, _, _, _, _, _, _, _, _)) =>
        Future.failed(new MaintenanceException(f"Database ${db.name} is being compacted"))
      case Some(InfoResult(false, _, _, _, _, _, _, purge_seq, _, update_seq)) =>
        couch.serverDate.flatMap { now =>
          // to make it "zero downtime", query a view in each design document
          // to rebuild indexes if needed
          // we first need to get all design documents
          db.builtInView("_all_docs").query[String, String, DesignDoc](
            startkey = Some("_design/"),
            endkey = Some("_design0"),
            include_docs = true) flatMap { designs =>
              allCompleted(for {
                (_, design) <- designs.docs
                // and query the first view found
                (view, _) <- design.views.headOption
              } yield db.design(design.name).view(view).query(reduce = false, limit = 1)).flatMap { case () =>
                db.info.map {
                  case Some(InfoResult(_, _, _, _, _, _, _, new_purge_seq, _, _)) if new_purge_seq != purge_seq =>
                    throw new MaintenanceException("Concurrent database purging occurred concurrently")
                  case None =>
                    throw new MaintenanceException(f"Database ${db.name} does not exist")
                  case  _ =>
                    now
                }
              }
            }
        }
      case None =>
        Future.failed(new MaintenanceException(f"Database ${db.name} does not exist"))
    }

  protected def takeMaintenanceLock(db: Database, uuid: String, now: Long): Future[MaintenanceDoc] =
    db.getDocById[MaintenanceDoc](f"_local/$name").flatMap {
      case Some(MaintenanceDoc(_, _, _, _, expires_at)) if expires_at > now =>
        Future.failed(new MaintenanceException("Another maintenance task is already running"))
      case Some(doc) =>
        db.saveDoc(MaintenanceDoc("_local/maintenance", uuid, "purge", now, now + timeout.toMillis).withRev(doc._rev))
      case None =>
        db.saveDoc(MaintenanceDoc("_local/maintenance", uuid, "purge", now, now + timeout.toMillis))
    }

}
