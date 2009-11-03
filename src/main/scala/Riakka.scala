package riakka

import net.liftweb.json._
import net.liftweb.json.Serialization
import net.liftweb.json.Serialization._
import net.liftweb.json.JsonParser._
import net.liftweb.json.DateFormat
import JsonAST._
import JsonDSL._
import JsonParser._
import Extraction._
import java.util.Date

object % {
  def apply(id: (Symbol, String)): % = %(id._1.name, id._2, List(), None, None, None)
}

case class %(bucket: String, key: String, var links: List[List[String]], var vclock: Option[String], var vtag: Option[String], var lastmod: Option[String]) {
  def id = bucket + "/" + key
}
// ask Symbol#toString to be patched in Scala 2.8 - otherwise ask Joni to support it in lift-json
// case class RiakLink(bucket: String, key: String, tag: String) // make buckets Symbols and lastmods Dates

class WalkSpec(bucket: Symbol, tag: Option[String], accumulate: Option[Boolean]) {
  override def toString = bucket.name + "," + tag.getOrElse("_") + "," + accumulate.getOrElse("_")
}

object Jiak {
  def init = new Jiak("localhost", 8098, "jiak")
}

class Jiak(val hostname: String, val port: Int, val jiak_base: String) {
	
  import dispatch._
	
  private val http = new Http
  private implicit val db = :/(hostname, port) / jiak_base
  private implicit val formats = Serialization.formats(NoTypeHints)

  import net.lag.logging.Logger
  private val log = Logger.get
	
  /** Find all keys of a given bucket and return in a Seq. */
  def find_all(bucket: Symbol): Seq[String] = {
	val response = http(db / bucket.name as_str)
	for { JString(key) <- parse(response) \\ "keys" } yield key
  }
  // later on support: def find_all[A](implicit m: scala.reflect.Manifest[A]): Seq[A]

  def get(metadata: %): (%, JObject) = {
    var request = db / metadata.id
    val response = try {
	  http(request as_str)
	} catch {
	  case StatusCode(404, _) => throw new NoSuchElementException
	}
	parse(response)
  }
  // later on support: def get[A](metadata: %)(implicit m: scala.reflect.Manifest[A]): A
  // as well as plain structs made up of Lists, Tuples, etc => all things convertable to JObject

  def save(metadata: %, obj: JObject): Unit = {
	http((db / metadata.id <:< Map("Content-Type" -> "application/json") <<< tuple_to_json(metadata, obj) >|))
  }

  def save_with_response(metadata: %, obj: JObject): (%, JObject) = {
    val response = http((db / metadata.id <:< Map("Content-Type" -> "application/json") <<? Map("returnbody" -> true) <<< (metadata, obj) as_str))
    parse(response)
  }
  
  def delete(metadata: %): Unit = {
    http(db / metadata.id <--() >|) // <<? Map("vclock" -> ...)
  }

  def walk(metadata: %, spec: WalkSpec*): Seq[(%, JObject)] = List()

  private implicit def tuple_to_json(metadata: %, obj: JObject): String = {
    val m = decompose(metadata)
    val riak_obj = m merge JObject(JField("object", obj) :: Nil)
    println("Sending to server\n" + pretty(render(riak_obj)))
    compact(render(riak_obj))
  }

  private implicit def json_to_tuple(json: JValue): (%, JObject) = {
	println("Receiving from server\n" + pretty(render(json)))
	val metadata = json.extract[%]
	val JField(_, obj) = json \ "object"
	return (metadata, obj.asInstanceOf[JObject]) // it has to be a JSON object
  }

}