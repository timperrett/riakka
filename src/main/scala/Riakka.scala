package riakka

import net.liftweb.json._
import JsonAST._
import JsonDSL._
import JsonParser._
import Extraction._

object Jiak {
  def init = new Jiak("localhost", 8098, "jiak")
}

class Jiak(val hostname: String, val port: Int, val jiak_base: String) {

  private val log = net.lag.logging.Logger.get	

  import dispatch._
  private val http = new Http
  private implicit val db = :/(hostname, port) / jiak_base
	
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
    val response = http((db / metadata.id <:< Map("Content-Type" -> "application/json") <<? Map("returnbody" -> true) <<< tuple_to_json(metadata, obj) as_str))
    parse(response)
  }
  
  def delete(metadata: %): Unit = {
    http((db / metadata.id DELETE) >|)
  }

  def walk(metadata: %, specs: WalkSpec*): Seq[(%, JObject)] = {
    val response = http(db / metadata.id / specs.mkString("/") as_str)
    val json = parse(response)
    val JField(_, JArray(riak_objects)) = (json \ "results")
    for (riak_object <- riak_objects) yield riak_object
  }

  /** Local implicit functions */

  private implicit def tuple_to_json(metadata: %, obj: JObject): String = {
	implicit val formats = Serialization.formats(NoTypeHints)
    val m = decompose(metadata)
    val riak_object = m merge JObject(JField("object", obj) :: Nil)
    log.info("Sending to server\n" + pretty(render(riak_object)))
    compact(render(riak_object))
  }

  private implicit def jvalue_to_tuple(json: JValue): (%, JObject) = {
	log.info("Receiving from server\n" + pretty(render(json)))
	implicit val formats = new DefaultFormats {
        override def dateFormatter = new java.text.SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ZZZ")
      }
	val metadata = json.extract[%]
	val JField(_, JObject(obj)) = json \ "object"
	return (metadata, obj)
  }

}