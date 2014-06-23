package gnieh.sohva
package async
package caching

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import net.liftweb.json.JValue

import gnieh.diffson.JsonPatch

import java.io.{
  File,
  InputStream
}

import strategy.Strategy

import impl.{
  CacheActor,
  GetDoc,
  GetDocs,
  Clear,
  InvalidateDoc
}

import akka.actor.{
  Props,
  PoisonPill
}
import akka.pattern.ask

/** A database caching the results to limit connections to the database when possible.
 *  The cache has a given maximum size that cannot be exceeded.
 *  It is implemented as an expiring LRU cache.
 *  This is useful when you have a lot of read access to the same documents with few writes.
 *
 *  @author Lucas Satabin
 */
class CachedDatabase private[caching] (
  name: String,
  couch: CouchDB,
  serializer: JsonSerializer,
  credit: Int,
  strategy: Strategy,
  val cacheSize: Int,
  val ttl: Duration,
  val realTimeInvalidation: Boolean)
  extends Database(name, couch, serializer, credit, strategy) {

  private val cache =
    couch.system.actorOf(Props(new CacheActor(super.getRawDocById(_, None), cacheSize, ttl)))

  import couch.timeout

  private val subscription =
    if(realTimeInvalidation)
      Some(changes().subscribe {
        case(id, _) => cache ! InvalidateDoc(id)
      })
    else
      None

  /** Clears the cache from all documents */
  def clearChache: Unit =
    cache ! Clear

  /** Stops the cache. It is necessary to call this method when you are done using this database to avoid memory leaks */
  def stopCache: Unit = {
    subscription.foreach(_.unsubscribe())
    cache ! PoisonPill
  }

  /** Returns the document identified by the given id if it exists.
   *  Documents are cached when using this method, so that furether retrievals should
   *  be quicker if the document is still valid and alive.
   *  If a specific revision is required, no cache is used.
   */
  override def getDocById[T: Manifest](id: String, revision: Option[String] = None): Future[Option[T]] =
    revision match {
      case Some(_) =>
        super.getDocById[T](id, revision)
      case None =>
        (cache ? GetDoc(id)).mapTo[Option[JValue]].map(cached => cached.map(serializer.fromJson[T]))
    }

  /** Returns the raw repsentation of the document identified by the given id if it exists.
   *  Documents are cached when using this method, so that furether retrievals should
   *  be quicker if the document is still valid and alive.
   *  If a specific revision is required, no cache is used.
   */
  override def getRawDocById(id: String, revision: Option[String] = None): Future[Option[JValue]] =
    revision match {
      case Some(_) =>
        super.getRawDocById(id, revision)
      case None =>
        (cache ? GetDoc(id)).mapTo[Option[JValue]]
    }

  /** Returns all the documents with given identifiers and of the given type.
   *  If the document with an identifier exists in the database but has not the
   *  required type, it is not added to the result.
   *  Documents are cached when using this method, so that furether retrievals should
   *  be quicker if the document is still valid and alive.
   */
  override def getDocsById[T: Manifest](ids: List[String]): Future[List[T]] =
    (cache ? GetDocs(ids)).mapTo[List[JValue]].map(_.map(serializer.fromJson[T]))

}
