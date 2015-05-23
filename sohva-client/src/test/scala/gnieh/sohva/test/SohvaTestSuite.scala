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
package test

import sync._

import gnieh.sohva.strategy._

import org.scalatest._

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._

import spray.json._

abstract class SohvaTestSpec(retry: Int = 0, strategy: Strategy = BarneyStinsonStrategy) extends FlatSpec with ShouldMatchers with BeforeAndAfterAll with SohvaProtocol {

  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5.seconds)

  implicit val testDocFormat = couchFormat[TestDoc]
  implicit val testDoc2Format = couchFormat[TestDoc2]

  val couch = new CouchClient
  val session = couch.startCookieSession
  val db = session.database("sohva-tests", credit = retry, strategy = strategy)

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

}

case class TestDoc(_id: String, toto: Int) extends IdRev
case class TestDoc2(_id: String, toto: Int) extends IdRev
