DFScala
=======

What is this?
-------------

This is a library for using the dataflow model of parallelism in the Scala
programming language [1].

Who wrote this?
---------------

DFScala was written primarily by Salman Khan and Daniel Goodman at the
University of Manchester. It is research involving Chris Seaton, Behram Khan,
Yegor Guskov, Mikel Luján and Ian Watson.

The corresponding researcher is Daniel Goodman, goodmand@cs.man.ac.uk.

How do I use this?
------------------

See the published paper [1]. There is a separate package of benchmarks
demonstrating how to use the library.

Licence
-------

See licence.txt.

Dependencies
------------

*   Scala 2.9.1
*   SBT 0.11

Compiling
---------

    # produces target/scala-2.9.1/dfscala_2.9.1-0.1.jar
    sbt package

Documentation
-------------

    # produces target/scala-2.9.1/api
    sbt doc

Acknowledgements
----------------

The Teraflux project is funded by the European Commission Seventh Framework
Programme. Chris Seaton is an EPSRC funded student. Mikel Luján is a Royal
Society University Research Fellow.

References
----------

[1] D. Goodman, S. Khan, C. Seaton, Y. Guskov, B. Khan, M. Luján, and I.
Watson. DFScala: High level dataflow support for Scala. In Proceedings of the
Second International Workshop on Data-Flow Models For Extreme Scale Computing
(DFM), 2012.
