Riakka
======

Scala library for talking to Riak.

Getting started
---------------

You need to have SBT installed. Please follow [setup instructions](http://code.google.com/p/simple-build-tool/wiki/Setup).

    git clone git://github.com/frank06/riakka.git
    cd riakka
    sbt
    > update
    > test

It assumes you're running Riak in `localhost` at port `8098`, and will use a random bucket starting with `riakka` to run the suite.

Riak
----

First off, you should be familiar with Riak. There is information available [here](http://riak.basho.com).
Some other resources of interest:

 - [Riak Presentation at NYC NoSQL](http://riak.basho.com/nyc-nosql/)
 - [Intro to Riak screencast](http://videocodechat.com/post/219711761/intro-to-riak-with-bryan-fink)
 - [Riak Demo: Stickynotes](http://blog.beerriot.com/2009/08/17/riak-demo-stickynotes/)
 - [Module jiak_resource Edoc](http://riak.basho.com/edoc/jiak_resource.html)
 - ...and we're all looking forward to a wiki :)

Quick overview
--------------

Riak (through its HTTP interface, Jiak) exposes documents with some associated metadata (all in JSON format). For example:

    {"bucket":"bucket", "key":"foo", "object":{"bar":"baz"}, "links":[["bucket","key","tag"], ["b2","key2","tag2"]]}"

Riakka will consistently return this structure splitted up in two: A tuple consisting of

 - a metadata object (of type `%`) that holds important data such as `bucket`, `key`, `vclock`, `links`.
 - the actual object (of type `JObject`) => see [lift-json](http://github.com/dpp/liftweb/blob/master/lift-base/lift-json)

Most query methods will also require a `%` object. There is a convenience method for creating those, as you will see in the following examples.

Features by example
-------------------

Fire a Scala console (within `sbt`):

        > console

 - first off, all the ceremony (wish it was more succint)
        scala> import riakka._
        scala> import Jiak._
        scala> import net.liftweb.json._
        scala> import JsonParser._
        scala> import JsonAST._
        scala> import JsonDSL._

 - let's initialize the client and save an object

        scala> val db = Jiak.init
        scala> val json = parse(""" {"my":"json", "list": [1, 2, 3]} """)
        scala> val metadata = %('test -> "mykey") // here we prepare a metadata object, with bucket test and key mykey
        scala> db save (metadata, json)

 - create a link (note the `save_with_response` method, that returns updated metadata!)

        scala> val linked_json = ("am_i_being_linked?" -> true) ~ ("a", "b")  // a neat DSL to create JSON
        scala> val (linked_json_metadata, _) = db save_with_response (%('test -> "linked"), linked_json)  // we persist that one
        scala> val link = Link('test, linked_json_metadata.key, "_")  // we create the link
        scala> db save (metadata.link_+(link), json) // we pass-in a new metadata object, which contains the link

 - walk the original object and retrieve the linked one

        scala> val linked_objects = db walk (metadata, ^^('test)) // check the docs for ^^ / link-walking spec
        scala> val (_, first_linked_object) = linked_objects first
        scala> first_linked_object.to_json // right?

 - now clean up

        scala> db delete metadata // will use the metadata to locate the document and delete it
        scala> db delete linked_json_metadata
        scala> db get metadata // does the result make sense?

There is also support for `If-None-Match`, attachments and more. Have a look at `RiakkaSpec.scala`.

To be done
----------

 - Much better support of the (known) HTTP API, including schemas (read/write masks), allow_mult, specifying R/W values and datastore info.
 - Support for entities (as well as plain structs made up of Lists, Tuples, etc => all things convertable to JObject), transparent serialization/deserialization to/from JSON. Should be not a big deal, as lift-json supports it wonderfully.
 - Make concepts more straightforward, improve documentation and test coverage, as usual.

Needless to say, fork and send pull requests. And make use of Github's issue tracker, too.

Citing [Paul from Ruby's RiakRest](http://github.com/wcpr/riakrest): "Go forth and Riak!"