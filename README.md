Global Names Parser
===================

[![Continuous Integration Status][ci-svg]][ci-link]
[![Stories in Ready][waffle-ready-svg]][waffle]
[![Stories in Progress][waffle-progress-svg]][waffle]

Introduction
------------

Global Names Parser or `gnparser` is a Scala library for breaking scientific
names into meaningful elements. It is based on [parboiled2] -- a Parsing
Expression Grammar (PEG) library. The `gnparser` project evolved from another
PEG-based scientific names parser -- [biodiversity][biodiversity-parser]
written in Ruby. Both projects were developed as a part of [Global Names
Architecture][gna].

It is common to use regular expressions for parsing scientific names,
and such approach works well for extraction of canonical forms in simple cases.
However for complex scientific names and for breaking names to their semantic
elements regular expression approach fails, unable to overcome recursive nature
of data embedded in the names. By contrast, `gnparser` is able to deal with the
most complex scientific name strings.

`gnparser` takes a name string like `Drosophila (Sophophora) melanogaster
Meigen, 1830` and returns back parsed components in JSON format. We supply an
informal [description of the output fields][json-fields]. Parser's behavior is
defined in its tests and the [test file][test-file] is a good source of
information about parser's capabilities, its input and output.

Features
--------

* Fast (~5x faster than [biodiversity gem][biodiversity-parser]),
  rock solid and elegant
* Extracts all elements from a name, not only a canonical form
* Works with very complex scientific names, including hybrids
* Can be used directly in any language that can call Java -- Scala,
  Java, R, Python, Ruby etc.
* Can run as a command line application
* Can run as a socket server
* Can run as a web server
* Can be scaled to many CPUs and computers

Use Cases
---------

### Getting the simplest possible canonical form

Canonical forms are great for matching names in spite of alternative spellings.
Use `canonical_form` field from parsing results for such case.

### Getting canonical form with infraspecies ranks

In botany infraspecific ranks play important role. Use `canonical_extended`
field to preserve them.

### Normalizing name strings

There are many inconsistencies in writing of scientific names. Use `normalized`
field to bring them all to a common form in spelling, empty spaces, ranks.

### Removing authorships in the middle of the name

Many data administrators store their name strings in two columns and split them
into "name part" and "authorship part". Such practice is not very effective for
names like "*Prosthechea cochleata* (L.) W.E.Higgins *var. grandiflora*
(Mutel) Christenson".  Combination of `canonical_extended` with the
`authorship` from the lowest taxon will do the job better.

### Figuring out if names are well-formed

If there are problems with parsing a name, parser generates `quality_warning`
messages and lowers parsing `quality` of the name.  Quality means the
following:

* `"quality": 1` - No problems were detected
* `"quality": 2` - There were small problems, normalized result should
                   still be good
* `"quality": 3` - There were serious problems with the name, and the final
                   result is rather doubtful
* `"parse": false` - A string could not be recognized as a scientific name

### Creating stable GUIDs for name strings

Parser uses UUID version 5 to generate its `id` field. There is algorithmic 1:1
relationship between the name string and the UUID. Moreover the same algorithm
can be used in any popular language to generate the same UUID. Such IDs can be
used to globally connect information about name strings.

More information about UUID version 5 can be found in the [Global Names
blog][uuid].

### Assembling canonical forms etc. from original spelling

Parser tries to correct problems with spelling, but sometimes it is important
to keep original spellings in canonical forms or authorships. The `positions`
field attaches semantic meaning to every word in the original name string and
allows to create canonical forms or other combinations using verbatim spelling
of the words. Each element in `positions` contains 3 parts:

1. semantic meaning of a word
2. start position of the word
3. end position of the word

For example `["species", 6, 11]` means that a specific epithet starts at 6th
character and ends *before* 11th character of the string.

Dependency Declaration for Java or Scala
----------------------------------------

The artifacts for `gnparser` live on [Maven Central][maven] and can be set as a
dependency in following ways:

SBT:

```scala
libraryDependencies += "org.globalnames" %% "gnparser" % "0.1.0"
```

Maven:

```xml
<dependency>
    <groupId>org.globalnames</groupId>
    <artifactId>gnparser_2.11</artifactId>
    <version>0.1.0</version>
</dependency>
```

From version 0.1.1 `gnparser` will be available for Scala 2.10.3+.

Release Package
---------------

[Release package][release] should be sufficient for all usages but development.
It is not needed for including parser into Java or Scala code -- [declare
dependency instead][declare-dependency].

### Requirements:

Java Run Environment (JRE) version >= 1.6 (>= 1.8 for Web server)

### Released Files

| File                          | Description                                |
|-------------------------------|--------------------------------------------|
| `gnparser-assembly-0.1.0.jar` | [Fat Jar][fat-jar]                         |
| `gnparser-0.1.0.zip`          | [Command line tool and socket server][cli] |
| `gnparser-web-0.1.0.zip`      | [Web service and REST API][web]            |
| `release-0.1.0.zip`           | Source code's zip file                     |
| `release-0.1.0.tar.gz`        | Source code's tar file                     |

Fat Jar
-------

Sometimes it is beneficial to have a jar that contains everything necessary to
run a program. Such jar would include Scala and all required libraries.

[Fat jar][gnparser-fatjar] for `gnparser` can be found in the [current
release][release].

Command Line Tool and Socket Server
-----------------------------------

### Installation on Linux/Mac

```bash
wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.1.0/gnparser-0.1.0.zip
unzip gnparser-0.1.0.zip
sudo rm -rf /opt/gnparser
sudo mv gnparser-0.1.0 /opt/gnparser
sudo rm /usr/local/bin/gnparse
sudo ln -s /opt/gnparser/bin/gnparse /usr/local/bin
```

### Installation on Windows

1. Download [gnparser-0.1.0.zip][gnparser-zip]
2. Extract it to a place where you usually store program files
3. Update your [PATH][windows-set-path] to point to bin subdirectory
4. Now you can use `gnparse` command provided by `gnparse.bat` script from CMD

### Usage of Executable

Note that `gnparse` loads Java run environment every time it is called. As a
result parsing one name at a time is **much** slower than parsing many names
from a file. When parsing large file expect rates of 3000-6000 name strings per
second on one CPU.

To parse one name

```
gnparse "Parus major Linnaeus, 1788"
```

To parse names from a file (one name per line).

```
gnparse -input file_with_names.txt [-output output_file.json]
```

To see help

```
gnparse -help
```

### Usage as a Socket Server

Use socket (TCP/IP) server when `gnparser` library cannot be imported directly
by a programming language. Setting `-port` is optional, 4334 is the default
port.

```
gnparse -server -port 1234
```

To test the socket connection use `telnet localhost 1234`, enter a name and
press `Enter`

Web Service and REST API
------------------------

### Installation on Linux/Mac

```bash
wget https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.1.0/gnparser-web-0.1.0.zip
unzip gnparser-web-0.1.0.zip
sudo rm -rf /opt/gnparser-web
sudo mv gnparser-web-0.1.0 /opt/gnparser-web
sudo rm /usr/local/bin/gnparser-web
sudo ln -s /opt/gnparser-web/bin/gnparser-web /usr/local/bin
```
To start web server in production mode on http://0.0.0.0:9000

```
gnparser-web
```

### Installation on Windows

1. Download [gnparser-web-0.1.0.zip][gnparser-web-zip]
2. Unzip it, and then launch CMD at that path
3. Run `cd gnparser-web-0.1.0`
4. Run `.\bin\gnparser-web.bat`

You can open it in a browser at [http://localhost:9000][localhost].

### REST API Interface

Make sure to CGI-escape name strings for GET requests. An '&' character needs
to be converted to '%26'

* `GET /api?names=["Aus bus", "Aus bus Linn. 1758"]`
* `POST /api` with request body of JSON array of strings for `names` parameter

Usage as a Library
------------------

Several languages are supported either natively or by running their JVM-based
versions. [Examples folder][examples-folder] provides scientific name parsing
code snippets for Scala, Java, Jython, JRuby and R languages.

To avoid declaring multiple dependencies Jython, JRuby and R need a [reference
 gnparser fat-jar][fat-jar].

If you decide to follow examples get the code from the [release] or [clone it
from GitHub][gnparser-git]

### Scala

[Scala example][example-scala] is an SBT subproject. To run it execute the command:

```
sbt 'examples/runMain org.globalnames.parser.examples.ParserScala'
```

### Java

[Java example][example-java] is an SBT subproject. To run it execute the command:

```
sbt 'examples/runMain org.globalnames.parser.examples.ParserJava'
```

### Jython

[Jython example][example-jython] requires [Jython][jython] -- a Python language
implementation for Java Virtual Machine.  Jython distribution should be
installed locally [according to instructions][jython-installation].

To run it execute the command:

```bash
java -jar $JYTHON_HOME/jython.jar \
  -Dpython.path=/path/to/gnparser-assembly-0.1.0.jar \
  examples/jython/parser.py
```

(JYTHON_HOME needs to be defined or replaced by path to Jython jar)

### R

[R example][example-r] requires [rJava package][rjava]
to be installed. To run it execute the command:

```
Rscript /opt/gnparser/examples/R/parser.R
```

### JRuby

[JRuby example][example-jruby] requires [JRuby][jruby] -- a Ruby language
implementation for Java Virtual Machine.  JRuby distribution should be
installed locally [according to instructions][jruby-installation].

To run it execute the command:

```bash
jruby -J-classpath /path/to/gnparser-assembly-0.1.0.jar \
  examples/jruby/parser.rb
```

Getting Code for Development
----------------------------

### Requirements

* [Git][git-install]
* [Scala version >= 2.10.3][scala-install]
* Java SDK version >= 1.8.0
* [SBT][sbt-install] >= 0.13.8

### Installation

```
git clone https://github.com/GlobalNamesArchitecture/gnparser.git
cd gnparser
```

If you decide to participate in `gnparser` development -- fork the repository
and submit pull requests of your work.

### Project Structure

The project consists of four parts:

* `parser` contains core routines for parsing input string
* `examples` contains usage samples for some popular programming languages
* `runner` contains code required to run `parser` from a command line as a
  standalone tool or to run it as a TCP/IP server
* `web` contains a web app and a RESTful interface to `parser`

### Commands

| Command             | Description                                          |
|---------------------|------------------------------------------------------|
| `sbt test`          | Runs all tests                                       |
| `sbt ++2.10.3 test` | Runs all tests against Scala v2.10.3                 |
| `sbt assembly`      | Creates [fat jars][fat-jar] for command line and web |
| `sbt stage`         | Creates executables for command line and web         |
| `sbt web/run`       | Runs the web server in development mode              |

Docker container
----------------

For usage with Docker containers read [gnparser container
instructions][gnparser-docker].

Authors
-------

[Alexander Myltsev][alexander-myltsev], [Dmitry Mozzherin][dimus]

License
---------

Released under [MIT license][license]

[alexander-myltsev]: http://myltsev.name
[biodiversity-parser]: https://github.com/GlobalNamesArchitecture/biodiversity
[ci-link]: http://travis-ci.org/GlobalNamesArchitecture/gnparser
[ci-svg]: https://secure.travis-ci.org/GlobalNamesArchitecture/gnparser.svg
[cli]: #command-line-tool-and-socket-server
[declare-dependency]: #dependency-declaration-for-java-or-scala
[dimus]: https://github.com/dimus
[example-java]: /examples/java-scala/src/main/java/org/globalnames/parser/examples/ParserJava.java
[example-jruby]: /examples/jruby/parser.rb
[example-jython]: /examples/jython/parser.py
[example-r]: /examples/R/parser.R
[example-scala]: /examples/java-scala/src/main/scala/org/globalnames/parser/examples/ParserScala.scala
[examples-folder]: /examples
[fat-jar]: #fat-jar
[git-install]: https://git-scm.com/
[gna]: http://globalnames.org
[gnparser-docker]: https://github.com/gn-docker/gnparser
[gnparser-git]: #getting-code-for-development
[gnparser-fatjar]: https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.1.0/gnparser-assembly-0.1.0.jar
[gnparser-web-zip]: https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.1.0/gnparser-web-0.1.0.zip
[gnparser-zip]: https://github.com/GlobalNamesArchitecture/gnparser/releases/download/release-0.1.0/gnparser-0.1.0.zip
[jruby-installation]: http://jruby.org/getting-started
[jruby]: http://jruby.org/
[json-fields]: /JSON_FIELDS.md
[jython-installation]: https://wiki.python.org/jython/InstallationInstructions
[jython]: http://www.jython.org/
[license]: /LICENSE
[localhost]: http://localhost:9000
[maven]: http://search.maven.org/#search%7Cga%7C1%7Cgnparser
[package-install]: #installation-from-a-release-package
[parboiled2]: http://parboiled2.org
[r-env]: https://www.r-project.org/about.html
[release-package]: #release-package
[release]: https://github.com/GlobalNamesArchitecture/gnparser/releases/tag/release-0.1.0
[rjava]: https://cran.r-project.org/web/packages/rJava/index.html
[sbt-install]: http://www.scala-sbt.org/download.html
[scala-install]: http://www.scala-lang.org/download/install.html
[test-file]: https://raw.githubusercontent.com/GlobalNamesArchitecture/gnparser/master/parser/src/test/resources/test_data.txt
[uuid]: http://globalnames.org/news/2015/05/31/gn-uuid-0-5-0/
[waffle-progress-svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=in%20progress&title=In%20Progress
[waffle-ready-svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=ready&title=Issues%20To%20Do
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
[web]: #web-service-and-rest-api
[windows-set-path]: https://java.com/en/download/help/path.xml
