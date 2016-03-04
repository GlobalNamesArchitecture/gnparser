Global Names Parser
===================

.. image:: https://secure.travis-ci.org/GlobalNamesArchitecture/gnparser.svg
    :alt: Continuous Integration Status
    :target: https://travis-ci.org/GlobalNamesArchitecture/gnparser

Brief Intro
-----------

``gnparser`` splits scientific names into elements with meta information. For example,
``"Homo sapiens Linnaeus"`` is decomposed to human readable information as follows:

========  ================  ========
Element   Meaning           Position
========  ================  ========
Homo      genus             (0,4)
sapiens   specific_epithet  (5,12)
Linnaeus  author            (13,21)
========  ================  ========

Try it as a command line tool under Linux/Mac:

.. code:: bash

    wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.2.0/gnparser-0.2.0.zip
    unzip gnparser-0.2.0.zip
    sudo rm -rf /opt/gnparser
    sudo mv gnparser-0.2.0 /opt/gnparser
    sudo rm -f /usr/local/bin/gnparse
    sudo ln -s /opt/gnparser/bin/gnparse /usr/local/bin
    gnparse "Homo sapiens Linnaeus"
    gnparse -help

``gnparser`` is also `dockerized <https://github.com/gn-docker/gnparser>`_:

.. code:: bash

    docker pull gnames/gnparser
    # to run web-server
    docker run -d -p 80:9000 --name gnparser gnames/gnparser
    # to run socket server
    docker run -d -p 4334:4334 --name gnparser gnames/gnparser socket

Finally, run it right from your SBT console:

.. code:: bash

    $ mkdir -p project
    $ echo 'sbt.version=0.13.8' > project/build.properties
    $ sbt ';set libraryDependencies += "org.globalnames" %% "gnparser" % "0.2.0";console'
    scala> import org.globalnames.parser.ScientificNameParser.{instance => scientificNameParser}
    scala> scientificNameParser.fromString("Homo sapiens Linnaeus").renderCompactJson

.. contents:: Contents of this Document

Introduction
------------

Global Names Parser or ``gnparser`` is a Scala library for breaking
scientific names into meaningful elements. It is based on
`parboiled2 <http://parboiled2.org>`_ -- a Parsing Expression Grammar
(PEG) library. The ``gnparser`` project evolved from another PEG-based
scientific names parser --
`biodiversity <https://github.com/GlobalNamesArchitecture/biodiversity>`_
written in Ruby. Both projects were developed as a part of `Global Names
Architecture <http://globalnames.org>`_.

It is common to use regular expressions for parsing scientific names,
and this approach works well at extracting canonical forms in simple
cases. However for complex scientific names and for breaking names into
their semantic elements an approach using regular expressions often fails, unable to
overcome the recursive nature of data embedded in names. By contrast,
``gnparser`` is able to deal with the most complex scientific name
strings.

``gnparser`` takes a name string like
``Drosophila (Sophophora) melanogaster Meigen, 1830`` and returns back
parsed components in JSON format. We supply an informal `description of
the output fields </JSON_FIELDS.md>`_. Parser's behavior is defined in
its tests and the `test
file <https://raw.githubusercontent.com/GlobalNamesArchitecture/gnparser/master/parser/src/test/resources/test_data.txt>`_
is a good source of information about parser's capabilities, its input
and output.

Features
--------

-  Fast (~5x faster than `biodiversity
   gem <https://github.com/GlobalNamesArchitecture/biodiversity>`_),
   rock solid and elegant
-  Extracts all elements from a name, not only a canonical form
-  Works with very complex scientific names, including hybrids
-  Can be used directly in any language that can call Java -- Scala,
   Java, R, Python, Ruby etc.
-  Can run as a command line application
-  Can run as a socket server
-  Can run as a web server
-  Can be scaled to many CPUs and computers
-  Calculates a stable UUID version 5 ID from the content of a string

Use Cases
---------

Getting the simplest possible canonical form
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Canonical forms are great for matching names despite alternative
spellings. Use ``canonical_form`` field from parsing results for this use
case.

Getting canonical form with infraspecies ranks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In botany infraspecific ranks play an important role. Use
``canonical_extended`` field to preserve them.

Normalizing name strings
~~~~~~~~~~~~~~~~~~~~~~~~

There are many inconsistencies in writing of scientific names. Use
``normalized`` field to bring them all to a common form in spelling,
empty spaces, ranks.

Removing authorships in the middle of the name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Many data administrators store their name strings in two columns and
split them into "name part" and "authorship part". Such practice is not
very effective for names like "*Prosthechea cochleata* (L.) W.E.Higgins
*var. grandiflora* (Mutel) Christenson". Combination of
``canonical_extended`` with the ``authorship`` from the lowest taxon
will do the job better.

Figuring out if names are well-formed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If there are problems with parsing a name, parser generates
``quality_warning`` messages and lowers parsing ``quality`` of the name.
Quality means the following:

-  ``"quality": 1`` - No problems were detected
-  ``"quality": 2`` - There were small problems, normalized result
   should still be good
-  ``"quality": 3`` - There were serious problems with the name, and the
   final result is rather doubtful
-  ``"parse": false`` - A string could not be recognized as a scientific
   name

Creating stable GUIDs for name strings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Parser uses UUID version 5 to generate its ``id`` field. There is
algorithmic 1:1 relationship between the name string and the UUID.
Moreover the same algorithm can be used in any popular language to
generate the same UUID. Such IDs can be used to globally connect
information about name strings.

More information about UUID version 5 can be found in the `Global Names
blog <http://globalnames.org/news/2015/05/31/gn-uuid-0-5-0/>`_.

You can also use UUID calculation library in your code as it is shown in
`Scala example section <#scala>`_.

Assembling canonical forms etc. from original spelling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Parser tries to correct problems with spelling, but sometimes it is
important to keep original spellings in canonical forms or authorships.
The ``positions`` field attaches semantic meaning to every word in the
original name string and allows to create canonical forms or other
combinations using verbatim spelling of the words. Each element in
``positions`` contains 3 parts:

1. semantic meaning of a word
2. start position of the word
3. end position of the word

For example ``["species", 6, 11]`` means that a specific epithet starts
at 6th character and ends *before* 11th character of the string.

Dependency Declaration for Java or Scala
----------------------------------------

The artifacts for ``gnparser`` live on `Maven
Central <http://search.maven.org/#search%7Cga%7C1%7Cgnparser>`_ and can
be set as a dependency in following ways:

SBT:

.. code:: Scala

    libraryDependencies += "org.globalnames" %% "gnparser" % "0.2.0"

Maven:

.. code:: xml

    <dependency>
        <groupId>org.globalnames</groupId>
        <artifactId>gnparser_2.11</artifactId>
        <version>0.2.0</version>
    </dependency>

    <dependency>
        <groupId>org.globalnames</groupId>
        <artifactId>gnparser_2.10</artifactId>
        <version>0.2.0</version>
    </dependency>

Release Package
---------------

`Release
package <https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-0.2.0>`_
should be sufficient for all usages but development. It is not needed
for including parser into Java or Scala code -- `declare dependency
instead <#dependency-declaration-for-java-or-scala>`_.

Requirements
~~~~~~~~~~~~

Java Run Environment (JRE) version >= 1.6 (>= 1.8 for Web server)

Released Files
~~~~~~~~~~~~~~

===============================   ===============================================
File                              Description
===============================   ===============================================
``gnparser-assembly-0.2.0.jar``   `Fat Jar <#fat-jar>`_
``gnparser-0.2.0.zip``            `Command line tool and socket
                                  server <#command-line-tool-and-socket-server>`_
``gnparser-web-0.2.0.zip``        `Web service and REST API
                                  <#web-service-and-rest-api>`_
``release-0.2.0.zip``             Source code's zip file
``release-0.2.0.tar.gz``          Source code's tar file
===============================   ===============================================

Fat Jar
-------

Sometimes it is beneficial to have a jar that contains everything
necessary to run a program. Such jar would include Scala and all
required libraries.

`Fat
jar <https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.2.0/gnparser-assembly-0.2.0.jar>`_
for ``gnparser`` can be found in the `current
release <https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-0.2.0>`_.

Command Line Tool and Socket Server
-----------------------------------

Installation on Linux/Mac
~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: bash

    wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.2.0/gnparser-0.2.0.zip
    unzip gnparser-0.2.0.zip
    sudo rm -rf /opt/gnparser
    sudo mv gnparser-0.2.0 /opt/gnparser
    sudo rm -f /usr/local/bin/gnparse
    sudo ln -s /opt/gnparser/bin/gnparse /usr/local/bin

Installation on Windows
~~~~~~~~~~~~~~~~~~~~~~~

1. Download
   `gnparser-0.2.0.zip <https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.2.0/gnparser-0.2.0.zip>`_
2. Extract it to a place where you usually store program files
3. Update your `PATH <https://java.com/en/download/help/path.xml>`_ to
   point to bin subdirectory
4. Now you can use ``gnparse`` command provided by ``gnparse.bat``
   script from CMD

Usage of Executable
~~~~~~~~~~~~~~~~~~~

Note that ``gnparse`` loads Java run environment every time it is
called. As a result parsing one name at a time is **much** slower than
parsing many names from a file. When parsing large file expect rates of
3000-6000 name strings per second on one CPU.

To parse one name

::

    gnparse "Parus major Linnaeus, 1788"

To parse names from a file (one name per line).

::

    gnparse -input file_with_names.txt [-output output_file.json]

To see help

::

    gnparse -help

Usage as a Socket Server
~~~~~~~~~~~~~~~~~~~~~~~~

Use socket (TCP/IP) server when ``gnparser`` library cannot be imported
directly by a programming language. Setting ``-port`` is optional, 4334
is the default port.

::

    gnparse -server -port 1234

To test the socket connection use ``telnet localhost 1234``, enter a
name and press ``Enter``

Web Service and REST API
------------------------

Installation on Linux/Mac
~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: bash

    wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.2.0/gnparser-web-0.2.0.zip
    unzip gnparser-web-0.2.0.zip
    sudo rm -rf /opt/gnparser-web
    sudo mv gnparser-web-0.2.0 /opt/gnparser-web
    sudo rm -f /usr/local/bin/gnparser-web
    sudo ln -s /opt/gnparser-web/bin/gnparser-web /usr/local/bin

To start web server in production mode on http://0.0.0.0:9000

::

    gnparser-web

Installation on Windows
~~~~~~~~~~~~~~~~~~~~~~~

1. Download
   `gnparser-web-0.2.0.zip <https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.2.0/gnparser-web-0.2.0.zip>`_
2. Unzip it, and then launch CMD at that path
3. Run ``cd gnparser-web-0.2.0``
4. Run ``.\bin\gnparser-web.bat``

You can open it in a browser at
`http://localhost:9000 <http://localhost:9000>`_.

REST API Interface
~~~~~~~~~~~~~~~~~~

Make sure to CGI-escape name strings for GET requests. An '&' character
needs to be converted to '%26'

-  ``GET /api?names=["Aus bus", "Aus bus Linn. 1758"]``
-  ``POST /api`` with request body of JSON array of strings

Usage as a Library
------------------

Several languages are supported either natively or by running their
JVM-based versions. `Examples folder </examples>`_ provides scientific
name parsing code snippets for Scala, Java, Jython, JRuby and R
languages.

To avoid declaring multiple dependencies Jython, JRuby and R need a
`reference gnparser fat-jar <#fat-jar>`_.

If you decide to follow examples get the code from the
`release <https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-0.2.0>`_
or `clone it from GitHub <#getting-code-for-development>`_

Scala
~~~~~

`Scala
example </examples/java-scala/src/main/scala/org/globalnames/parser/examples/ParserScala.scala>`_
is an SBT subproject. To run it execute the command:

.. code:: bash

    sbt 'examples/runMain org.globalnames.parser.examples.ParserScala'

Calculation of UUID version 5 can be done in the following way:

.. code:: scala

    scala> val gen = org.globalnames.UuidGenerator()
    scala> gen.generate("Salinator solida")
    res0: java.util.UUID = da1a79e5-c16f-5ff7-a925-14c5c7ecdec5


Java
~~~~

`Java
example </examples/java-scala/src/main/java/org/globalnames/parser/examples/ParserJava.java>`_
is an SBT subproject. To run it execute the command:

.. code:: bash

    sbt 'examples/runMain org.globalnames.parser.examples.ParserJava'

Jython
~~~~~~

`Jython example </examples/jython/parser.py>`_ requires
`Jython <http://www.jython.org/>`_ -- a Python language implementation
for Java Virtual Machine. Jython distribution should be installed
locally `according to
instructions <https://wiki.python.org/jython/InstallationInstructions>`_.

To run it execute the command:

.. code:: bash

    java -jar $JYTHON_HOME/jython.jar \
      -Dpython.path=/path/to/gnparser-assembly-0.2.0.jar \
      examples/jython/parser.py

(JYTHON\_HOME needs to be defined or replaced by path to Jython jar)

R
~

`R example </examples/R/parser.R>`_ requires `rJava
package <https://cran.r-project.org/web/packages/rJava/index.html>`_ to
be installed. To run it execute the command:

::

    Rscript /opt/gnparser/examples/R/parser.R

JRuby
~~~~~

`JRuby example </examples/jruby/parser.rb>`_ requires
`JRuby <http://jruby.org/>`_ -- a Ruby language implementation for Java
Virtual Machine. JRuby distribution should be installed locally
`according to instructions <http://jruby.org/getting-started>`_.

To run it execute the command:

.. code:: bash

    jruby -J-classpath /path/to/gnparser-assembly-0.2.0.jar \
      examples/jruby/parser.rb

Getting Code for Development
----------------------------

Requirements
~~~~~~~~~~~~

-  `Git <https://git-scm.com/>`_
-  `Scala version >=
   2.10.6 <http://www.scala-lang.org/download/install.html>`_
-  Java SDK version >= 1.8.0
-  `SBT <http://www.scala-sbt.org/download.html>`_ >= 0.13.8

Installation
~~~~~~~~~~~~

.. code:: bash

    git clone https://github.com/GlobalNamesArchitecture/gnparser.git
    cd gnparser

If you decide to participate in ``gnparser`` development -- fork the
repository and submit pull requests of your work.

Project Structure
~~~~~~~~~~~~~~~~~

The project consists of four parts:

-  ``parser`` contains core routines for parsing input string
-  ``examples`` contains usage samples for some popular programming
   languages
-  ``runner`` contains code required to run ``parser`` from a command
   line as a standalone tool or to run it as a TCP/IP server
-  ``web`` contains a web app and a RESTful interface to ``parser``

Commands
~~~~~~~~

=====================   =======================================
Command                 Description
=====================   =======================================
``sbt test``            Runs all tests
``sbt ++2.10.6 test``   Runs all tests against Scala v2.10.6
``sbt assembly``        Creates `fat jars <#fat-jar>`_ for
                        command line and web
``sbt stage``           Creates executables for
                        command line and web
``sbt web/run``         Runs the web server in development mode
=====================   =======================================

Docker container
----------------

For usage with Docker containers read `gnparser container
instructions <https://github.com/gn-docker/gnparser>`_.

Contributors
------------

+ Alexander Myltsev `http://myltsev.name <http://myltsev.name>`_ `alexander-myltsev@github <https://github.com/alexander-myltsev>`_
+ Dmitry Mozzherin `dimus@github <https://github.com/dimus>`_

License
-------

Released under `MIT license </LICENSE>`_
