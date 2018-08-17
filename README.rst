Global Names Parser
===================

.. image:: https://circleci.com/gh/GlobalNamesArchitecture/gnparser.svg?style=svg
    :target: https://circleci.com/gh/GlobalNamesArchitecture/gnparser

Important Changes
-----------------

**The change as follows might break your code!** ``gnparser 0.4.1`` optionally returned
``canonical_name.value_extended`` in output JSON. **Only** if input name contains ranks,
the field contains a canonical name with ranks. Otherwise the field is ``null``. ``gnparser 1.0.2``
renames the field to ``canonical_name.value_ranked``. Also it always returns it, even if input name
doesn't contain ranks.

Brief Intro
-----------

``gnparser`` splits scientific names into their component elements with associated meta information.
For example, ``"Homo sapiens Linnaeus"`` is parsed into human readable information as follows:

========  ================  ========
Element   Meaning           Position
========  ================  ========
Homo      genus             (0,4)
sapiens   specific_epithet  (5,12)
Linnaeus  author            (13,21)
========  ================  ========

Try it as a command line tool under Linux/Mac:

.. code:: bash

    wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-1.0.2/gnparser-1.0.2.zip
    unzip gnparser-1.0.2.zip
    sudo rm -rf /opt/gnparser
    sudo mv gnparser-1.0.2 /opt/gnparser
    sudo rm -f /usr/local/bin/gnparser
    sudo ln -s /opt/gnparser/bin/gnparser /usr/local/bin
    gnparser name "Homo sapiens Linnaeus"
    gnparser --help

``gnparser`` is also `dockerized <https://hub.docker.com/r/gnames/gnparser>`_:

.. code:: bash

    docker pull gnames/gnparser
    # to run web-server
    docker run -p 80:4334 --name gnparser gnames/gnparser web
    # to run socket server
    docker run -d -p 4334:4334 --name gnparser gnames/gnparser socket

Finally, run it right from your SBT console:

.. code:: bash

    $ mkdir -p project
    $ echo 'sbt.version=0.13.12' > project/build.properties
    $ sbt ';set libraryDependencies += "org.globalnames" %% "gnparser" % "1.0.2";console'
    scala> import org.globalnames.parser.ScientificNameParser.{instance => scientificNameParser}
    scala> scientificNameParser.fromString("Homo sapiens Linnaeus").renderCompactJson

.. contents:: Contents of this Document

Introduction
------------

Global Names Parser or ``gnparser`` is a Scala library for breaking up
scientific names into their different elements. The elements are classified.
It is based on `parboiled2 <http://parboiled2.org>`_ -- a Parsing Expression
Grammar (PEG) library. The ``gnparser`` project evolved from another PEG-based
scientific names parser --
`biodiversity <https://github.com/GlobalNamesArchitecture/biodiversity>`_
written in Ruby. Both projects were developed as a part of `Global Names
Architecture <http://globalnames.org>`_.

Many other parsing algorithms for scientific names use regular expressions.
This approach works well for extracting canonical forms in simple cases.
However, for complex scientific names and to parse scientific names into
all semantic elements regular expressions often fail, unable to overcome
the recursive nature of data embedded in names. By contrast, ``gnparser``
is able to deal with the most complex scientific name strings.

``gnparser`` takes a name string like
``Drosophila (Sophophora) melanogaster Meigen, 1830`` and returns parsed
components in JSON format. We supply a `description of
the output fields as JSON schema <http://globalnames.org/schemas/gnparser.json>`_.
This parser's behavior is defined in its tests and the `test
file <https://raw.githubusercontent.com/GlobalNamesArchitecture/gnparser/master/parser/src/test/resources/test_data.txt>`_
is a good source of information about parser's capabilities, its input and output.

Speed
-----

Millions of names parsed per hour on a i7-4930K CPU
(6 cores, 12 threads, at 3.4 GHz), parser v0.3.1

========  ================
Threads   Millions/hr
========  ================
1         29.44
2         50.85
4         90.45
8         120.75
12        130.9
========  ================

Features
--------

-  Fast (~8x faster than biodiversity
   `gem <https://github.com/GlobalNamesArchitecture/biodiversity>`_),
   rock solid and elegant
-  Extracts all elements from a name, not only canonical forms
-  Works with very complex scientific names, including hybrids
-  Can be used directly in any language that can call Java -- Scala,
   Java, R, Python, Ruby etc.
-  Can run as a command line application
-  Can run as a socket server
-  Can run as a web server
-  Can be integrated into Apache Spark-based projects
-  Can be scaled to many CPUs and computers
-  Calculates a stable UUID version 5 ID from the content of a string

Use Cases
---------

Getting the simplest possible canonical form
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Canonical forms of a scientific name are the latinized components without
annotations, authors or dates. They are great for matching names despite
alternative spellings. Use the ``canonical_form`` field from parsing
results for this use case.

Getting canonical form with infraspecies ranks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In botany infraspecific ranks play an important role. Use
``canonical_extended`` field to preserve them.

Normalizing name strings
~~~~~~~~~~~~~~~~~~~~~~~~

There are many inconsistencies in how scientific names may be written.
Use ``normalized`` field to bring them all to a common form (spelling, spacing,
ranks).

Removing authorships in the middle of the name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Many data administrators store name strings in two columns and split
them into "name part" and "authorship part". This practice misses some
information when dealing with names like
"*Prosthechea cochleata* (L.) W.E.Higgins *var. grandiflora*
(Mutel) Christenson". However, if this is the use case, a combination of
``canonical_extended`` with the authorship from the lowest taxon will do
the job. You can also use ``--format simple`` flag for ``gnparse`` command line tool.

Figuring out if names are well-formed
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If there are problems with parsing a name, parser generates
``quality_warning`` messages and lowers parsing ``quality`` of the name.
Quality values mean the following:

-  ``"quality": 1`` - No problems were detected
-  ``"quality": 2`` - There were small problems, normalized result
   should still be good
-  ``"quality": 3`` - There were serious problems with the name, and the
   final result is rather doubtful
-  ``"parse": false`` - A string could not be recognized as a scientific
   name

Creating stable GUIDs for name strings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``gnparser`` uses UUID version 5 to generate its ``id`` field.
There is algorithmic 1:1 relationship between the name string and the UUID.
Moreover the same algorithm can be used in any popular language to
generate the same UUID. Such IDs can be used to globally connect information
about name strings or information associated with name-strings.

More information about UUID version 5 can be found in the `Global Names
blog <http://globalnames.org/news/2015/05/31/gn-uuid-0-5-0/>`_.

You can also use UUID calculation library in your code as it is shown in
`Scala example section <#scala>`_.

Assembling canonical forms etc. from original spelling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``gnparser`` tries to correct problems with spelling, but sometimes it is
important to keep original spelling of the canonical forms or authorships.
The ``positions`` field attaches semantic meaning to every word in the
original name string and allows users to create canonical forms or other
combinations using the original verbatim spelling of the words. Each element
in ``positions`` contains 3 parts:

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

    libraryDependencies += "org.globalnames" %% "gnparser" % "1.0.2"

Maven:

.. code:: xml

    <dependency>
        <groupId>org.globalnames</groupId>
        <artifactId>gnparser_2.11</artifactId>
        <version>1.0.2</version>
    </dependency>

    <dependency>
        <groupId>org.globalnames</groupId>
        <artifactId>gnparser_2.10</artifactId>
        <version>1.0.2</version>
    </dependency>

Release Package
---------------

`Release
package <https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-1.0.2>`_
should be sufficient for all usages but development. It is not needed
for including ``gnparser`` into Java or Scala code -- `declare dependency
instead <#dependency-declaration-for-java-or-scala>`_.

Requirements
~~~~~~~~~~~~

Java Run Environment (JRE) version >= 1.6 (>= 1.8 for `runner` project)

Released Files
~~~~~~~~~~~~~~

===============================   ===============================================
File                              Description
===============================   ===============================================
``gnparser-assembly-1.0.2.jar``   `Fat Jar <#fat-jar>`_
``gnparser-1.0.2.zip``            `Command line tool, web and socket
                                  server <#command-line-tool-and-socket-server>`_
``release-1.0.2.zip``             Source code's zip file
``release-1.0.2.tar.gz``          Source code's tar file
===============================   ===============================================

Fat Jar
-------

Sometimes it is beneficial to have a jar that contains everything
necessary to run a program. Such a jar would include Scala and all
required libraries.

`Fat
jar <https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-1.0.2/gnparser-assembly-1.0.2.jar>`_
for ``gnparser`` can be found in the `current
release <https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-1.0.2>`_.

Command Line Tool and Socket Server
-----------------------------------

Installation on Linux/Mac
~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: bash

    wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-1.0.2/gnparser-1.0.2.zip
    unzip gnparser-1.0.2.zip
    sudo rm -rf /opt/gnparser
    sudo mv gnparser-1.0.2 /opt/gnparser
    sudo rm -f /usr/local/bin/gnparser
    sudo ln -s /opt/gnparser/bin/gnparser /usr/local/bin

Installation on Windows
~~~~~~~~~~~~~~~~~~~~~~~

1. Download
   `gnparser-1.0.2.zip <https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-1.0.2/gnparser-1.0.2.zip>`_
2. Extract it to a place where you usually store program files
3. Update your `PATH <https://java.com/en/download/help/path.xml>`_ to
   point to bin subdirectory
4. Now you can use ``gnparser`` command provided by ``gnparser.bat``
   script from CMD

Usage of Executable
~~~~~~~~~~~~~~~~~~~

Note that ``gnparser`` loads Java runtime environment every time it is
called. As a result parsing one name at a time is **much** slower than
parsing many names from a file. When parsing large file expect rates of
6000-9000 name strings per second on one CPU.

To parse one name

::

    gnparser name "Parus major Linnaeus, 1788"

To parse names from a file (one name per line)

::

    gnparser file --input file_with_names.txt [--output output_file.json --threads 8]

``file`` is the default command if no command is given. To parse names from STDIN to STDOUT:

::

    cat file_with_names.txt | gnparser > file_with_parsed_names.txt

``gnparser`` accepts the flag ``--format`` (or simply ``-f``) that determines
the output representation. The values are ``simple`` for simple tab-delimited format,
``json-pretty`` and ``json-compact`` for the JSON extended pretty form and the compact form
correspondingly

Finally, ``gnparser`` tool is not optimized for parsing huge input files.
`Speed`_ is measured by interacting with ``gnparser`` as the socket server. Using
it as file parser intends that it constructs JSON tree in memory and then saves
it to the disk. 1 million names might be hundreds of megabytes, while parsed
results JSON would be more than gigabyte. ``gnparser`` would consume a lot of RAM and
time to handle that.

::

    gnparser name "Parus major Linnaeus, 1788" --format simple

To see help

::

    gnparser --help

Usage as a Socket Server
~~~~~~~~~~~~~~~~~~~~~~~~

Use socket (TCP/IP) server when the ``gnparser`` library cannot be imported
directly by a programming language. Setting ``--port`` is optional, 4334
is the default port.

::

    gnparser socket --port 1234

To test the socket connection with command line:

::

    telnet localhost 1234

When you see your telnet prompt, enter a name and press ``Enter``.

To use (TCP/IP) socket server in your code find a ``socket`` library for
your language. There is a good chance it is included in the language's
core. For example in Ruby it would be:

.. code:: ruby

    require "socket"
    s = TCPSocket.open("0.0.0.0", 1234)
    s.write("Homo sapiens\n")
    s.gets

``gnparser`` TCP server can parse new-line delimited string in a single run.
For example if you have a file ``names.txt`` with many name-strings:

.. code:: text
    Dicalix glomeratus (King ex C.B.Clarke) Migo
    Mongoliolites Bondarenko & Minzhin 1977
    Spalacopsis macra Monn� & Giesbert 1994
    Scaphiodon Heckel 1843 sec. Eschmeyer 1998
    Cinnamomum calcareum Y.K.Li
    Eleala
    Haemanota abdominalis (Rothschild 1909)
    Astronidium anomalum Merr. & L.M. Perry
    Setaria chrysochaeta T. Durand & Schinz
    Metalimnobia flavobdominalis
    Clinopodium calamintha
    ...

it is more efficient to send several new-line delimited names at once through
the socket. ``gnparser`` server returns a string which contains new-line
delimited chunks, where each line is a JSON string for a corresponding input
name.

Example below also includes a safeguard for "back pressure" cases, where a
client application sends strings too fast. TCP server stores data temporarily
in buffers before processing, and buffers might get over-filled. At such
moment TCP server stops receiving new packets ("back pressure" situation) until
it empties its inner queue of messages. Because of that a client application
should monitor the count of sent bytes:

.. code:: ruby

    require "socket"
    require "json"

    socket = TCPSocket.open("0.0.0.0", 4334)

    open("names.txt").each_slice(100) do |slice|
      text = slice.join
      until socket.write(text) == text.bytes.size
        puts("Reading of a slice starting with #{slice[0]} failed. Retrying")
        str = socket.recv(10) until str.nil?
      end
      slice.each { puts(socket.gets) }
    end

Usage as a REST API Interface
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Use web-server as an HTTP alternative to TCP/IP server. Setting ``--port`` is
optional, 4334 is the default port. To start web server in production mode on
http://0.0.0.0:9000

::

    gnparser web --port 9000

Make sure to CGI-escape name strings for GET requests. An '&' character
needs to be converted to '%26'

-  ``GET /api?q=Aus+bus|Aus+bus+D.+%26+M.,+1870`
-  ``POST /api`` with request body of JSON array of strings

Usage as a Library
------------------

Several languages are supported either natively or by running their
JVM-based versions. The `examples folder </examples>`_ provides scientific
name parsing code snippets for Scala, Java, Jython, JRuby and R
languages.

To avoid declaring multiple dependencies Jython, JRuby and R need a
`reference gnparser fat-jar <#fat-jar>`_.

If you decide to follow examples get the code from the
`release <https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-1.0.2>`_
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


Apache Spark
~~~~~~~~~~~~

`examples/spark/README.rst </examples/spark/README.rst>`_
describes how to use ``gnparser`` with Scala or Python in Apache Spark
projects.

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

    GNPARSER_JAR_PATH=/path/to/gnparser-assembly-1.0.2.jar \
      jython examples/jython/parser.py

R
~

`R example </examples/R/parser.R>`_ requires `rJava
package <https://cran.r-project.org/web/packages/rJava/index.html>`_ to
be installed. To run it execute the command:

::

    Rscript examples/R/parser.R

JRuby
~~~~~

`JRuby example </examples/jruby/parser.rb>`_ requires
`JRuby <http://jruby.org/>`_ -- a Ruby language implementation for Java
Virtual Machine. JRuby distribution should be installed locally
`according to instructions <http://jruby.org/getting-started>`_.

To run it execute the command:

.. code:: bash

    jruby -J-classpath /path/to/gnparser-assembly-1.0.2.jar \
      examples/jruby/parser.rb

Getting Code for Development
----------------------------

Requirements
~~~~~~~~~~~~

-  `Git <https://git-scm.com/>`_
-  `Scala version >=
   2.10.6 <http://www.scala-lang.org/download/install.html>`_
-  Java SDK version >= 1.8.0
-  `SBT <http://www.scala-sbt.org/download.html>`_ >= 0.13.12

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

Prebuilt container image can be found on
`dockerhub <https://hub.docker.com/r/gnames/gnparser/>`_

Usage
-----

To install/update container

.. code:: bash

    docker pull gnames/gnparser

To run web server

.. code:: bash

    docker run -d -p 80:4334 --name gnparser gnames/gnparser web

To run socket server

.. code:: bash

    docker run -d -p 4334:4334 --name gnparser gnames/gnparser socket

Contributors
------------

+ Alexander Myltsev `http://myltsev.com <http://myltsev.com>`_ `alexander-myltsev@github <https://github.com/alexander-myltsev>`_
+ Dmitry Mozzherin `dimus@github <https://github.com/dimus>`_

License
-------

Released under `MIT license </LICENSE>`_
