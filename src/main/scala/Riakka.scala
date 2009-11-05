package riakka

import net.liftweb.json._
import JsonAST._
import JsonDSL._
import JsonParser._
import Extraction._
import java.io._

object Jiak {
  def init = new Jiak("localhost", 8098, "jiak")
}

class Jiak(val hostname: String, val port: Int, val jiak_base: String) extends Logging {

  import dispatch._	
  private val http = new Http with RiakkaExceptionHandler
  private val db = :/(hostname, port) / jiak_base

  /** Find all keys of a given bucket and return in a Seq. */
  def find_all(bucket: Symbol): Seq[String] = {
	val response = http(db / bucket.name as_str)
	for { JString(key) <- parse(response) \\ "keys" } yield key
  }
  // later on support: def find_all[A](implicit m: scala.reflect.Manifest[A]): Seq[A]

  def get(metadata: %): (%, JObject) = {
    val request = db / metadata.id
    val response = http(request as_str)
	parse(response)
  }
  // later on support: def get[A](metadata: %)(implicit m: scala.reflect.Manifest[A]): A
  // as well as plain structs made up of Lists, Tuples, etc => all things convertable to JObject

  def conditional_get(metadata: %): (%, Option[JObject]) = {
    try {
      val request0 = db / metadata.id
      val request = metadata.vtag match {
        case Some(vtag) => request0 <:< Map("If-None-Match" -> ("\"" + vtag + "\""))
        case None => request0
      }
      (metadata, Some(parse(http(request as_str))._2))
    } catch {
      case NotModified => (metadata, None)
    }
  }

  /** Gets an attachment from raw -- WARNING: only works in Riak trunk **/ 
  def get_attachment(metadata: %, out: OutputStream): Unit = {
	http(:/(hostname, port) / "raw" / metadata.id >>> out)
  }

  def save(metadata: %, obj: JObject): Unit = {
	http((db / metadata.id <:< Map("Content-Type" -> "application/json") <<< tuple_to_json(metadata, obj) >|))
  }

  def save_with_response(metadata: %, obj: JObject): (%, JObject) = {
    val response = http((db / metadata.id <:< Map("Content-Type" -> "application/json") <<? Map("returnbody" -> true) <<< tuple_to_json(metadata, obj) as_str))
    parse(response)
  }

  /** Saves an attachment to raw -- WARNING: only works in Riak trunk **/
  def save_attachment(metadata: %, file: File, content_type: String) = {
	implicit def r2r(request: Request) = new PutFileRequest(request)
    http(:/(hostname, port) / "raw" / metadata.id put_file (file, content_type) >|)
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

  private class PutFileRequest(request: Request) {
    def put_file(file: File, content_type: String) = request next {
      import org.apache.http.client.methods.HttpPut
      import org.apache.http.entity.FileEntity
      val put_method = new HttpPut
      put_method setEntity new FileEntity(file, content_type)
      Request.mimic(put_method) _
    }
  }

}