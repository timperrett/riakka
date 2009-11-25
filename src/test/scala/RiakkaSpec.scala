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

  // TODO this really needs to be refactored with a real-world example! (and do something with the proliferation of metadatas)

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

      val (m_other, _) = db save_with_response (%(default_bucket -> "object"), object_1)
      db save (m_other, object_2)
      assert((db get m_other)._2 == object_2)
      db delete m_other
    }

    it("should set and walk links") { // make this more clear
      val (m_obj, obj) = db get metadata
      val child1 = ("am_i_being_linked?" -> true) ~ ("a" -> 1)
      val child2 = ("am_i_being_linked?" -> "also true") ~ ("b" -> 2)

      val m2 = %(default_bucket -> random_key_2)
      val m3 = %(default_bucket -> random_key_3)

      val (m_child1, _) = db save_with_response (m2, child1)
      db save (m3, child2)

      val link1 = Link(m2)
      val link2 = Link(m3)

      db save (m_obj.link_+(link1, link2), obj)

      val children = db walk (metadata, ^^(default_bucket))
      assert(children exists { _._2 == child1 })
      assert(children exists { _._2 == child2 })

      log.info("Date of last modification ===> " + m_child1.lastmod.getOrElse("Sorry dude, no date here"))
    }

    it("should be deleted") {
      db delete metadata
      intercept[NoSuchElementException] { db get metadata }
    }

  }

  describe("The datastore") {

    it ("should clean up the whole mess created by the test suite") {
      val all = db find_all default_bucket
      log.info("These are all the docs in " + default_bucket.name + ": " + all.mkString(", "))

      all foreach { key =>
        db delete %(default_bucket -> key)
      }

    }

  }

}