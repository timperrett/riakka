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

It assumes you're running Riak in `localhost` at port `8098`, and will use the bucket `test` to run the suite. 