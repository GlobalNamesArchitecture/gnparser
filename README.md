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

It is common to use [regular expressions][regex] for parsing scientific names,
and such approach works well for extraction of canonical forms in simple cases.
However for complex scientific names and for breaking names to their semantic
elements regular expression approach fails, unable to overcome recursive nature
of data embedded in the names. By contrast, `gnparser` is able to deal with the
most complex scientific name strings.

`gnparser` takes a name string like `Drosophila (Sophophora) melanogaster Meigen,
1830` and returns back parsed components in JSON format. We supply an informal
[description of the output fields][json-fields]. Parser's behavior is defined
in its tests and the [test file][test-file] is a good source of information
about parser's capabilities, its input and output.

Features
--------

* Fast, rock solid and elegant
* Extracts all elements from a name, not only a canonical form
* Works with very complex scientific names, including hybrids
* Can be used directly in many languages -- Scala, Java, R, Python, Ruby etc.
* Can run as a command line application
* Can run as a socket server
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
names like "*Prosthechea cochleata* (L.) W.E.Higgins *var.  grandiflora*
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
relationship between the name string and the name. Moreover the same algorithm
can be used in any popular language to generate the same UUID. Such IDs can be
used to globally connect information about name strings.

More information about these UUIDs can be found in [Global Names blog][uuid].

### Assembling canonical forms etc. from original spelling

Parser tries to correct problems with spelling, but sometimes it is important
to keep original spellings in canonical forms or authorships. The `positions`
field attaches semantic meaning to every word in the original name string and
allows to create canonical forms or other combinations using verbatim spelling
of the words. Each member of positions collection contains fields indicating
semantic meaning of a word, start of the word and the end of the word
correspondingly.  For example `["species", 6, 11]` means that a specific
epithet starts at 6th character and ends before 11th character of the string.

Dependency Declaration for Java or Scala
----------------------------------------

The artifacts for `gnparser` live on [Maven Central][maven] and can be set as a
dependency in following ways:

SBT:

```scala
libraryDependencies += "org.globalnames" %% "parser" % "0.1.0"
```

Maven:

```xml
<dependency>
    <groupId>org.globalnames</groupId>
    <artifactId>parser</artifactId>
    <version>0.1.0</version>
</dependency>
```

`gnparser` is available for Scala 2.11.0+.

Installation from a Release Package
-----------------------------------

Release package should be sufficient for all usages but the development. It is
not needed for including parser into Java or Scala code -- [declare dependency
instead][declare-dependency]. For most cases the only requirement is Java
Run Environment (JRE) version >= 1.6, however you will also need [Scala Build
Tool][sbt-install] to run web service.

### Docker

```
docker pull gnames/gnparser
```

### Linux/Mac

```
wget https://github.com/GlobalNamesArchitecture/gnparser/archive/v0.1.0.tar.gz
tar xvf gnparser-0.1.0.tar.gz
sudo rm -rf /opt/gnparser
sudo mv gnparser-0.1.0 /opt/gnparser
ln -s /opt/gnparser/runner/target/universal/stage/bin/gnparse /usr/local/bin
```

### Windows

1. Download [gnparse-0.1.0-SNAPSHOT.zip][gnparse-zip]
2. Extract it to a place where you usually store program files
3. Update your [PATH][windows-set-path] to point to bin subdirectory
4. Now you can use `gnparse` command provided by `gnparse.bat` script from CMD

Installation from GitHub
------------------------

Only needed for development. For all other usages [declare
dependency][declare-dependency] or download a [release
package][package-install]

Requirements:

* [Git][git-install]
* [Scala version >= 2.11.0][scala-install]
* Java SDK version >= 1.6.0
* [SBT][sbt-install]

```
git clone https://github.com/GlobalNamesArchitecture/gnparser.git
cd gnparser
sbt test
```

Run `sbt stage` to create `gnparse` and `gnparse.bat` executing scripts at
`{PROJECT_ROOT}/runner/target/universal/stage/bin/`.

### Project Structure

The project consists of four parts:

* `parser` contains core routines for parsing input string
* `examples` contains usage samples for some popular programming languages
* `runner` contains code required to run `parser` from a command line as a
  standalone tool or to run it as a TCP/IP server
* `web` contains a web app and a RESTful interface to `parser`

## Fat Jar

Sometimes it is beneficial to have a jar that contains everything necessary to run
a program. Such jar would include Scala and all required libraries. Run `sbt assembly`
to put it to release packages at

```
{PROJECT_ROOT}/runner/target/scala-2.11/global-names-parser-runner-assembly-0.1.0.jar
```

Usage as a Library
------------------

Several languages are supported either natively or by running their JVM-based
versions. [Examples folder][examples-folder] provides scientific name parsing
code snippets for Scala, Java, Jython, JRuby and R languages.

To avoid declaring multiple dependencies Jython, JRuby and R need a [reference
 GnParser fat-jar][fat-jar].

### Scala

TODO: Make Scala example

Scala example is an SBT subproject and is stored at [scala
subfolder][examples-folder/scala]. To run it execute the command:

TODO: Put Scala sbt command

```
sbt ";project examples;run"
```

### Java

[Java example][example-java] is an SBT subproject. To run it execute the
command:

```
sbt ";project examples;run"
```

### Jython

[Jython][jython] is a Python language implementation for Java Virtual Machine.
Jython distribution should be installed locally [according to
instruction][jython-installation].

Jython needs [a reference GnParser fat-jar][fat-jar]. Run the command to
execute [Jython example][example-jython] (`$JYTHON_HOME` should be
defined):

```bash
java -jar $JYTHON_HOME/jython.jar \
  -Dpython.path=/opt/gnparser/runner/target/scala-2.11/global-names-parser-runner-assembly-0.1.0.jar \
  examples/jython/parser.py
```

### R

[R is a language and environment][r-env] for statistical computing and
graphics. [R example][example-r] requires [rJava package][rjava]
to be installed. Example script can be run with the command:

```
Rscript /opt/gnparser/examples/R/parser.R
```

### JRuby

[JRuby][jruby] is an implementation of Ruby for Java Virtual Machine. A
distribution should be installed locally [according to
instruction][jruby-installation]. [JRuby example][example-jruby] needs a
[reference GnParser fat jar][fat-jar]. Example script can be run with the
command:

```bash
jruby -J-classpath \
  /opt/gnparser/runner/target/scala-2.11/global-names-parser-runner-assembly-0.1.0.jar \
  examples/jruby/parser.rb
```

Command Line Usage
---------

### Usage of Executable

To see help

```gnparse -help```

To parse one name

```gnparse "Parus major Linnaeus, 1788"```

To parse names from a file (one name per line).

```
gnparse -input file_with_names.txt [-output output_file.json]
```

### Usage as a Socket Server

Use socket (TCP/IP) server when `gnparser` library cannot be imported directly
by a programming language. Setting `-port` is optional, 4334 is the default
port.

```
gnparse -server -port 1234
```

To test socket connection use

```
telnet localhost 1234
```

Usage as a Web Service
----------------------

No artifact is shipped with wib service. `gnparser` source code is required for launch.

Start web server with

```
sbt web/run
```

You can open it in a browser at [http://localhost:9000][localhost]

Web application has also REST API interface as follows:

* `GET /api?names=["Aus bus", "Aus bus 1700"]`
* `POST /api` with request body of JSON array of strings

In case of using [docker's container for gnparser][gnparser-docker] the
following command will download the container and set web service on port 80 of
the host machine.

```
docker run -d -p 80:9000 --name gnparser gnames/gnparser
```

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
[declare-dependency]: #dependency-declaration-for-java-or-scala
[dimus]: https://github.com/dimus
[gnparser-docker]: https://github.com/gn-docker/gnparser
[example-r]: /examples/R/parser.R
[example-java]: /examples/java/src/main/java/Parser.java
[example-jruby]: /examples/jruby/parser.rb
[example-jython]: /examples/jython/parser.py
[examples-folder]: /examples
[fat-jar]: #fat-jar
[git-install]: https://git-scm.com/
[gna]: http://globalnames.org
[gnparse-zip]: https://github.com/GlobalNamesArchitecture/gnparser/archive/gnparse-0.1.0-SNAPSHOT.zip
[jruby]: http://jruby.org/
[jruby-installation]: http://jruby.org/getting-started
[json-fields]: /JSON_FIELDS.md
[jython-installation]: https://wiki.python.org/jython/InstallationInstructions
[jython]: http://www.jython.org/
[license]: /LICENSE
[localhost]: http://localhost:9000
[maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.globalnames%22
[package-install]: #installation-from-a-release-package
[parboiled2]: http://parboiled2.org
[r-env]: https://www.r-project.org/about.html
[regex]: https://en.wikipedia.org/wiki/Regular_expression
[releases]: https://github.com/GlobalNamesArchitecture/gnparser/releases
[rjava]: https://cran.r-project.org/web/packages/rJava/index.html
[sbt-install]: http://www.scala-sbt.org/download.html
[scala-install]: http://www.scala-lang.org/download/install.html
[test-file]: https://raw.githubusercontent.com/GlobalNamesArchitecture/gnparser/master/parser/src/test/resources/test_data.txt
[uuid]: http://globalnames.org/news/2015/05/31/gn-uuid-0-5-0/
[waffle-progress-svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=in%20progress&title=In%20Progress
[waffle-ready-svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=ready&title=Issues%20To%20Do
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
[windows-set-path]: https://java.com/en/download/help/path.xml
