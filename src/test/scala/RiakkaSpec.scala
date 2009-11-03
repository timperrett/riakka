package riakka

import org.scalatest.{Spec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers
import java.util.Date
import net.liftweb.json._
import net.liftweb.json.Serialization
import net.liftweb.json.Serialization._
import net.liftweb.json.JsonParser._
import net.liftweb.json.DateFormat
import JsonAST._
import JsonDSL._
import JsonParser._
import Extraction._

case class Post(date: Date, id: String, title: String, body: String) // extends Riak

class BaseSpec extends Spec with ShouldMatchers with BeforeAndAfter {
	
  // override def beforeAll
  val db = Jiak.init
  
  val random_key = java.util.UUID.randomUUID.toString
  val random_key_2 = java.util.UUID.randomUUID.toString
  val default_bucket = 'test // '
  val metadata = %(default_bucket -> random_key)

  describe("A given object") {

    it("should be persisted and retrieved") {

	  val obj: JObject = 
	  ("person" ->
	    ("name" -> "Joe") ~
	    ("age" -> 35) ~
	    ("spouse" -> 
	      ("person" -> 
	        ("name" -> "Marilyn") ~
	        ("age" -> 33)
	      )
	    )
	  )
	  
	  db save (metadata, obj)
	  val (metadata2, obj2) = db get metadata
	  assert(obj == obj2)
	
    }

    it("should be updated") {
	   val (first_metadata, first_object) = db get metadata
	   val second_object: JObject = ("answer" -> 42)
	   val linked_object: JObject = ("am_i_being_linked?" -> "yes")
	   db save (%(default_bucket -> random_key_2), linked_object)
	   first_metadata.links_=(List(List(default_bucket.name, random_key_2, "_")))
	   db save (first_metadata, second_object)
	   val (third_metadata, third_object) = db get first_metadata
	   assert(second_object == third_object)
	}
	
	it("should walk links") {
	   val linked_objects = db walk (metadata, WalkSpec(default_bucket, None, None))
	   val linked_object = db get (%(default_bucket -> random_key_2))
	   assert(linked_objects.first == linked_object)
	}
	
	it("should be deleted") {
  	  db delete metadata
      intercept[NoSuchElementException] { db get metadata }
    }

  }

  describe("The datastore") {
   
    it("should return a set of stored keys for a given bucket") {
      // hmmm... right now here only for demo purposes
	  db find_all default_bucket foreach (println(_))
    }

  }

}