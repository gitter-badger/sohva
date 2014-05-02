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
package lucene
package impl

import akka.actor._
import SupervisorStrategy._

import net.liftweb.json._

import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.Query
import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer

import java.io.File

/** An indexer manages indexes for all databases in a couchdb instance.
 *  Indexes are created and monitored by this actor.
 *
 *  @param baseDirectory the base directory where indexes are saved
 *
 *  @author Lucas Satabin
 */
class IndexManager(baseDirectory: File) extends Actor {

  def receive = indexing(Map())

  import IndexManager._

  override val supervisorStrategy = OneForOneStrategy() {
    case _: OutOfMemoryError => Stop
    case _                   => Restart
  }

  private def indexing(indexes: Map[String, ActorRef]): Receive = {
    case DocumentChanged(database, docs) if indexes.contains(database) =>
      // send the processing message to the index actor
      indexes(database) ! DatabaseIndex.Index(docs)

    case SubscribeDatabase(database, config, documenter) if !indexes.contains(database) =>
      val actor =
        context.actorOf(Props(new DatabaseIndex(directory(database), config, documenter)), name = database)
      // we monitor this actor, in case of a crash (for example, full disk when flushing)
      // so that we can react in accordance to this
      context.watch(actor)
      // enrich the known database indexes  with the newly created actor
      context.become(indexing(indexes + (database -> actor)))

    case UnsubscribeDatabase(database) if indexes.contains(database) =>
      // stop the corresponding actor
      indexes(database) ! PoisonPill

    case SearchDatabase(databases, query, n) =>

    case Terminated(index) =>
      // a watched actor was terminated, just remove it from known indexes
      context.become(indexing(indexes - index.path.name))

  }

  private def directory(db: String): File =
    new File(baseDirectory, db)

}

object IndexManager {
  case class DocumentChanged(database: String, docs: List[(String, Option[JObject])])
  case class SubscribeDatabase(
    database: String,
    config: IndexWriterConfig,
    documenter: JObject => Option[Document])
  case class UnsubscribeDatabase(database: String)
  case class SearchDatabase(databases: List[String], query: Query, n: Int)
  case class DisposeSearcher(database: List[String])
  case class SearchResult(results: List[ScoreDoc])

}
