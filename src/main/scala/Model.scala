package riakka

import net.liftweb.json._
import JsonAST._
import JsonDSL._
import Extraction._

import java.util.Date

/* Domain objects that model the interaction with Riak. */
/* Scala 2.8 with named- and default-args, copy and such shall bring some serious goodness here. */

// class %, pronounced "metadata"
class %(val bucket: Symbol, val key: String, val links: List[Link], val vclock: Option[String], val vtag: Option[String], val lastmod: Option[Date]) {
  def id = bucket.name + "/" + key
  def link_+(link: Link*) = new %(bucket, key, link.toList ::: links, vclock, vtag, lastmod)
}

object % {
  def apply(id: (Symbol, String)): % = new %(id._1, id._2, List(), None, None, None)
  def apply(id: (Symbol, String), links: Seq[Link]): % = new %(id._1, id._2, links.toList, None, None, None)
}

case class Link(val bucket: Symbol, val key: String, val tag: String)

object Link {
  def apply(metadata: %) = new Link(metadata.bucket, metadata.key, "_")
}

// class ^^, pronounced "link-walking specification"
case class ^^(bucket: Symbol, tag: Option[String], accumulate: Option[Boolean]) {
  override def toString = bucket.name + "," + tag.getOrElse("_") + "," + accumulate.getOrElse("_")
}

object ^^ {
  def apply(bucket: Symbol) = new ^^(bucket, None, None)
}

private[riakka] trait Logging {
  val log = net.lag.logging.Logger.get
}

import dispatch._

private[riakka] trait WhenAware {
  def when[T](check: Int => Boolean)(handler: Handler[T]): T
}

/* This behaviour gives us fine-grained control on dealing with low-level exceptions */
private[riakka] trait RiakkaExceptionHandler extends WhenAware {
  abstract override def when[T](check: Int => Boolean)(handler: Handler[T]): T = {
    try {
      super.when(check)(handler)
    } catch {
      case StatusCode(304, _) => throw NotModified
      case StatusCode(404, _) => throw new NoSuchElementException
    }
  }
}

abstract class RiakkaException extends RuntimeException
object NotModified extends RiakkaException