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

import akka.actor.ActorRef

import scala.concurrent.Future

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.search.{
  Query,
  TopDocs
}

/** Interface for making full text search query to a database
 *
 *  @author Lucas Satabin
 */
class DatabaseSearcher(databases: List[String], indexer: ActorRef) {

  def search(query: Query, n: Int): Future[TopDocs] =
    ???

}
