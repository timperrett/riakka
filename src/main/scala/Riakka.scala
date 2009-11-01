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

// missing: vclock, vtag, lastmod
case class RiakMetadata(bucket: String, key: String, links: Seq[Seq[String]])

object Riakka {	
  def apply() = new Riakka("localhost", 8098)
}

class Riakka(val hostname: String, val port: Int) {
	
  import dispatch._
	
  private val http = new Http
  private implicit val db = :/(hostname, port) / "jiak"

  import net.lag.logging.Logger
  private val log = Logger.get
	
  /** Find all keys of a given bucket and return in a Seq. */
  def findAll(bucket: String): Seq[String] = {
	val response = http(db / bucket as_str)
	for { JString(key) <- parse(response) \\ "keys" } yield key
  }
  // Would this be feasible? e.g. val list = db.find[BlogPost] 
  //def find[A](implicit m: scala.reflect.Manifest[A]): Seq[A] = 

  // should be guarded and wrapped in an Option?
  def get(metadata: RiakMetadata): JValue = {
    var req = db / metadata.bucket / metadata.key
	val resp = http(req as_str)
	val Some(JField(_, obj)) = parse(resp) find {
	     case JField("object", _) => true
	     case _ => false
	   }
	return obj
  }
  // support entities in the future => get[A] : (RiakMetadata, A)

  def save(metadata: RiakMetadata, obj: JObject): Unit = {
	http((db / metadata.bucket / metadata.key <:< Map("Content-Type" -> "application/json") <<< getJson(metadata, obj)) >|)
  }
  
  def delete(metadata: RiakMetadata): Unit = http(db / metadata.bucket / metadata.key <--() >|) // <<? Map("vclock" -> ...)

  private def getJson(metadata: RiakMetadata, obj: JValue): String = {
    implicit val formats = Serialization.formats(NoTypeHints) // ShortTypeHints(classOf[Post] :: Nil)
    val m = decompose(metadata)
    val riak_obj = m merge JObject(JField("object", obj) :: Nil)
    compact(render(riak_obj))
  }
  // support entities, getJson[A]

}