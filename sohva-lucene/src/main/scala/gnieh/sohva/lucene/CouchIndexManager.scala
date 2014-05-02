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

import akka.actor._
import akka.util.Timeout

import scala.collection.{ mutable => mu }

import scala.concurrent.Future

import org.apache.lucene.index.{
  IndexWriter,
  IndexWriterConfig
}
import IndexWriterConfig.OpenMode
import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version

import net.liftweb.json._

import async._
import impl._

import java.io.File

import rx.lang.scala._

/** A CouchDB Indexer gives access to indexing of databases that are inside a CouchDB instance.
 *  Several databases of the CouchDB server may be indexed either separatly or in the same index.
 *  Different credentials may be used for different indexed databases.
 *
 *  @author Lucas Satabin
 */
class CouchIndexManager(
    val baseDirectory: File,
    val hostname: String = "localhost",
    val port: Int = 5984,
    val ssl: Boolean = false)(implicit val system: ActorSystem,
        timeout: Timeout) {

  // the client instance to communicate with the databases
  private val client = new CouchClient(hostname, port, ssl)

  import client.ec

  private val indexer = system.actorOf(Props(new IndexManager(baseDirectory)))

  /** Start indexing the database. The subscription returned when indexing started allows
   *  user to stop indexing of this database.
   *  @param database The database name for which one wants to build an index
   *  @param update Specify whether the index must be update or erased when opened. By default it updates an already existing index
   *  @param initialIndex Specify whether the index must be built when the index is created or if it is only
   *         built incrementally when documents are modified. By default it is built from scratch on creation
   *  @param analyzer The analyzer used to idnex texts
   *  @param credentials CouchDB credential if any
   */
  def indexer(database: String,
    update: Boolean = true,
    initialIndex: Boolean = true,
    analyzer: Analyzer = new StandardAnalyzer(Version.LUCENE_48),
    credentials: Option[CouchCredentials] = None)(documenter: JObject => Option[Document]): Future[Subscription] =
      for {
        client <- authenticated(credentials)
      } yield {
        val db = client.database(database)
        val changes = db.changes()

        val config = new IndexWriterConfig(Version.LUCENE_48, analyzer)
        if(update)
          config.setOpenMode(OpenMode.CREATE_OR_APPEND)
        else
          config.setOpenMode(OpenMode.CREATE)

        // subscribe the database to the actor
        indexer ! IndexManager.SubscribeDatabase(database, config, documenter)

        // if requested, Start indexing all documents
        if(initialIndex)
          for {
            ids <- db._all_docs()
            docs <- db.getDocsById(ids)
          } indexer ! IndexManager.DocumentChanged(database, ids zip docs)

        // subscribe to the change stream
        val subs = changes.stream.subscribe(new IndexObserver(database))

        Subscription {
          changes.close()
          subs.unsubscribe()
        }
      }

  /** Returns an interface to search for documents in one or several databases */
  def searcher(databases: List[String]): DatabaseSearcher =
    new DatabaseSearcher(databases, indexer)

  private def authenticated(credentials: Option[CouchCredentials]) = credentials match {
    case Some(credentials) => client.withCredentials(credentials)
    case None              => Future.successful(client)
  }

  private class IndexObserver(database: String) extends Observer[(String, Option[JObject])] {

    override def onNext(v: (String, Option[JObject])): Unit = {
      indexer ! IndexManager.DocumentChanged(database, List(v))
    }

    override def onCompleted(): Unit = {
      indexer ! IndexManager.UnsubscribeDatabase(database)
    }
  }

}

