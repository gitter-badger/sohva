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

import org.apache.lucene.index.{
  IndexWriter,
  IndexWriterConfig,
  DirectoryReader,
  Term
}
import org.apache.lucene.search.{
  IndexSearcher,
  Query
}
import org.apache.lucene.document.{
  Document,
  Field,
  StringField
}
import org.apache.lucene.store.FSDirectory

import net.liftweb.json._

import akka.actor.Actor

import java.io.File

import scala.collection.JavaConverters._

/** Actor responsible for handling index and query messages for a specific database.
 *  It manages the `IndexWriter` and `IndexReader` used, commiting to the store, ...
 *
 *  '''Note:''' Do not instantiate directly this actor but use the [[gnieh.sohva.lucene.CouchIndexer]]
 *  class instead which will create instances of this actor when needed and will monitor them.
 *
 *  @author Lucas Satabin
 */
class DatabaseIndex(
  dbIndexDir: File,
  config: IndexWriterConfig,
  documenter: JObject => Option[Document]) extends Actor {

  import DatabaseIndex._

  def receive = receiving(FSDirectory.open(dbIndexDir), None, None, None)

  private def receiving(directory: FSDirectory, writer: Option[IndexWriter], reader: Option[DirectoryReader], searcher: Option[IndexSearcher]): Receive = {
    case Index(docs) =>
      // create the writer if none exists
      val w = writer match {
        case Some(w) => w
        case None    => new IndexWriter(directory, config)
      }
      // process the given documents if needed by updating
      // field for this document identifier for this index
      for((id, obj) <- docs)
        obj match {
          case Some(o) =>
            for(doc <- documenter(o)) {
              // ensure the document have an `_id' field
              doc.add(new StringField("_id", id, Field.Store.YES))
              w.updateDocument(term(id), doc)
            }
          case None =>
            // delete the document with the given identifier from the index
            w.deleteDocuments(term(id))
        }
      w.commit()

      // invalidate the reader if any existed
      reader.foreach(_.close())
      // we now have the latest writer, and no valid reader/searcher
      context.become(receiving(directory, Some(w), None, None))

    case Search(query, n) =>
      // do we have a valid reader?
      val (r, s) = (reader, searcher) match {
        case (Some(r), Some(s)) if r.isCurrent =>
          (r, s)
        case (Some(r), _) =>
          // open a new reader to make it up-to-date
          val newReader = DirectoryReader.openIfChanged(r)
          // close the old reader
          r.close()
          // and return the newly created one
          (newReader, new IndexSearcher(newReader))
        case (None, _) =>
          val r = DirectoryReader.open(directory)
          (r, new IndexSearcher(r))
      }

      // execute the search query
      val results =
        for(scoreDoc <- s.search(query, n).scoreDocs)
          yield {
            val doc = s.doc(scoreDoc.doc, Set("_id").asJava)
            ScoreDoc(doc.getField("_id").stringValue, self.path.name, scoreDoc.score)
          }

      // reply to the sender
      sender ! Found(results)

      context.become(receiving(directory, writer, Some(r), Some(s)))

  }

  @inline
  private def term(id: String) =
    new Term("_id", id)

}

object DatabaseIndex {
  case object Stop
  case class Index(docs: List[(String, Option[JObject])])
  case class Search(query: Query, n: Int)
  case class Found(results: Seq[ScoreDoc])
}

