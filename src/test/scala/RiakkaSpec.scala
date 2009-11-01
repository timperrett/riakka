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
  val db = Riakka()
  val random_key = java.util.UUID.randomUUID.toString
  val default_bucket = "test"
	
  describe("A given JSON structure") {

    it("should be persisted and retrieved") {

      //val post = Post(new Date, "id", "title", "body")
	  //Riakka.fromJson(m, obj)
	  //Riakka(m, post)

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
	  
	  val metadata = RiakMetadata(default_bucket, random_key, List())
	  
	  db save (metadata, obj)
	  val obj2 = db get metadata
	  assert(obj == obj2)
	  println("Under key " + random_key + " the following JSON was retrieved:")
	  println(pretty(render(obj2)))
	
    }

  	describe("The datastore") {

	    it("should give back a set of stored keys") {
		  // hmmm... right now here only for demo purposes
		  db findAll default_bucket foreach (println(_))
		}
		
		it("should be able to delete a key") {
		  	db delete RiakMetadata(default_bucket, random_key, List())
		}
	}

  }
	
}