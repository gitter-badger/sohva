package gnieh.sohva
package async
package caching
package impl

import akka.actor.Actor

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import scala.collection.immutable.Queue

import net.liftweb.json.JValue

/** The concrete thread-safe cache implementation as an actor.
 *  Entries can be retrieved or added as well as invalidated. This happens e.g.
 *  when another client modified the document revision. An invalidated entry is a candidate for
 *  eviction on next cleanup.
 *
 *  @author Lucas Satabin
 */
class CacheActor(retrieve: String => Future[Option[JValue]], size: Int, ttl: Duration) extends Actor {

  private implicit def ec = context.system.dispatcher

  private val hasTtl =
    ttl.isFinite

  private val ttlNanos =
    if(hasTtl)
      ttl.toNanos
    else
      0l


  def receive = running(Map.empty, Queue.empty)

  private def running(cache: Map[String, (Long, Option[JValue])], ages: Queue[String]): Receive = {
    case get @ GetDoc(id) =>
      cache.get(id) match {
        case Some((expiration, value)) if stillValid(expiration) =>
          sender ! value
        case Some(_) =>
          // expired document, reload it
          val s = sender
          for(doc <- retrieve(id)) {
            // caches the newly retrieved document
            cacheDoc(cache, ages, id, doc)
            s ! doc
          }
        case None =>
          // document not in cache, load it
          val s = sender
          for(doc <- retrieve(id)) {
            // caches the newly retrieved document
            cacheDoc(cache, ages, id, doc)
            s ! doc
          }
      }

    case InvalidateDoc(id) =>
      // we explicitely request the document to be invalidated, i.e.
      // it is immediately removed from the cache
      for(doc <- cache.get(id))
        context.become(running(cache - id, ages.filter(_ != id)))

    case Clear =>
      // clear all cached documents
      context.become(running(Map.empty, Queue.empty))

  }

  @inline
  private def stillValid(expiration: Long): Boolean =
    !hasTtl || System.nanoTime > expiration

  private def cacheDoc[T](cache: Map[String, (Long, Option[JValue])], ages: Queue[String], id: String, doc: Option[JValue]): Unit = {
    val (newCache, newAges) = if(cache.size >= size) {
      // the cache is full, make some room before doing anything else
      // this is an LRU cache, so the oldest entry is removed first
      // if no entry expired
      val cache1 = cache.filter {
        case (_, (expiration, _)) => stillValid(expiration)
      }
      val ages1 = ages.filter(id => cache1.contains(id))
      val (cache2, ages2) =
        if(cache1.size >= size) {
          // there were no expired keys, removed the oldest one
          val (oldest, rest) = ages1.dequeue
          (cache1 - oldest, rest)
        } else {
          (cache1, ages1)
        }
    } else {
      (cache, ages)
    }

    context.become(running(cache.updated(id, (timestamp, doc)), ages.enqueue(id)))
  }

  private def timestamp =
    if(hasTtl)
      System.nanoTime + ttlNanos
    else
      0l

}

case class GetDoc(id: String)
case class GetDocs(ids: List[String])
case class InvalidateDoc(id: String)
case object Clear
