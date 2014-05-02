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
import akka.util._

import scala.concurrent.duration._

import org.scalatest._

import sync._

import java.nio.file.Files

import org.apache.lucene.document.Document
import org.apache.lucene.document.{
  Field,
  IntField,
  StringField,
  TextField
}

class BasicIndexTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAll {

  implicit val system = ActorSystem()
  implicit val timeout = Timeout(20.seconds)

  val couch = new CouchClient
  val session = couch.startCookieSession
  val db =  session.database("sohva-lucene-tests")

  import couch.serializer.formats
  import system.dispatcher

  override def beforeAll() {
    // login
    session.login("admin", "admin")
    // create database
    db.create
  }

  override def afterAll() {
    // cleanup database
    db.delete
    // logout
    session.logout
    couch.shutdown()
    system.shutdown()
  }

  "Indexing a database" should "work" in {

    val manager = new CouchIndexManager(Files.createTempDirectory("sohva-lucene-tests-").toFile)

    val subs = manager.indexer("sohva-lucene-tests") {
      case o =>
        for(TestDoc(_id, toto, tata) <- o.extractOpt[TestDoc])
          yield {
            val doc = new Document
            doc.add(new IntField("toto", toto, Field.Store.YES))
            doc.add(new TextField("tata", tata, Field.Store.YES))
            doc.add(new StringField("_id", _id, Field.Store.YES))
            doc
          }
    }

    for(i <- 1 to 10)
      db.saveDoc(TestDoc(s"doc$i", i, "This is my super text"))

    Thread.sleep(3000)

    subs.map(_.unsubscribe())

  }

}

case class TestDoc(_id: String, toto: Int, tata: String) extends IdRev

