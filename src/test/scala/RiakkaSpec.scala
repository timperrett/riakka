package riakka

import org.scalatest.{Spec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import net.liftweb.json._
import JsonAST._
import JsonDSL._
import JsonParser._
import Extraction._

class BaseSpec extends Spec with ShouldMatchers with BeforeAndAfter with Logging {

  // override def beforeAll
  val db = Jiak.init

  def rand() = java.util.UUID.randomUUID.toString
  val random_key = rand()
  val random_key_2 = rand()
  val random_key_3 = rand()
  val default_bucket = Symbol("riakka-" + rand())
  val metadata = %(default_bucket -> random_key)

  describe("A given JSON object") {

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

    it("should return None when providing back the same vtag or Some if they don't match") {
      val (metadata_with_vtag, original_object) = db get metadata
      val (_, obj) = db conditional_get metadata_with_vtag
      assert(obj == None)
      val (_, obj2) = db conditional_get metadata
      assert(obj2 == Some(original_object))
    }

    it("should be updated, twice in a row") {
      val object_1: JObject = ("answer" -> 42)
      val object_2: JObject = ("answer" -> 46)

      val (m_other, _) = db save_with_response (%(default_bucket, "object"), object_1)
      db save (m_other, object_2)
      assert((db get m_other)._2 == object_2)
      db delete m_other
    }

    it("should set and walk links") { // make this more clear
      val (first_metadata, first_object) = db get metadata
      val linked_object: JObject = ("am_i_being_linked?" -> true)
      val linked_object_2: JObject = ("am_i_being_linked?" -> "also true")
      val (linked_object_metadata, _) = db save_with_response (%(default_bucket -> random_key_2), linked_object)
      db save (%(default_bucket -> random_key_3), linked_object_2)
      first_metadata.link_+(Link(default_bucket, random_key_2, "_"))
      first_metadata.link_+(Link(default_bucket, random_key_3, "_"))
      // until we fix the Metadata/Links models - and things can look like:
      // val first_metadata_with_link = %(first_metadata, Link(default_bucket, random_key_2, "_"))
      db save (first_metadata, first_object)

      val linked_objects = db walk (metadata, WalkSpec(default_bucket, None, None))
      log.info("Date of last modification ===> " + linked_object_metadata.lastmod.getOrElse("Sorry dude, no date here"))
      assert(linked_objects exists { _._2 == linked_object })
    }

    it("should be deleted") {
      db delete metadata
      intercept[NoSuchElementException] { db get metadata }
    }

  }

  describe("The datastore") {

    it("should return a set of stored keys for a given bucket") {
      // hmmm... right now here only for demo purposes
      val all = db find_all default_bucket mkString(", ")
      log.info(all)
    }

  }

}