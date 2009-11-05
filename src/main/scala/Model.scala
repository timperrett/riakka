package riakka

import net.liftweb.json._
import JsonAST._
import JsonDSL._
import Extraction._

import java.util.Date

/* Domain objects that model the interaction with Riak. */
/* Scala 2.8 with named- and default-args, copy and such shall bring some goodness here. */

object % {
  def apply(id: (Symbol, String)): % = new %(id._1.name, id._2, None, None, None)
  //def apply(m: %, links: Link*): % = new %(m.bucket, m.key, links.toList, m.vclock, m.vtag, m.lastmod)
}

class %(val bucket: String, val key: String, val vclock: Option[String], val vtag: Option[String], val lastmod: Option[Date]) {
  def id = bucket + "/" + key
  private var links: List[List[String]] = List() // this mutable thingy to be replaced
  def link_+(link: Link) = links ::= List(link.bucket.name, link.key, link.tag)
}
// make bucket a Symbol, when upgrading to cutting-edge lift-json (thanks Joni!)

case class Link(val bucket: Symbol, val key: String, val tag: String)

case class WalkSpec(bucket: Symbol, tag: Option[String], accumulate: Option[Boolean]) {
  override def toString = bucket.name + "," + tag.getOrElse("_") + "," + accumulate.getOrElse("_")
}

class RichJValue(value: JValue) {
  def to_json = pretty(render(value)) 
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