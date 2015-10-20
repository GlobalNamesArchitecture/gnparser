Global Names Parser
===================

[![Continuous Integration Status][ci-svg]][ci-link]
[![Stories in Ready][waffle-ready-svg]][waffle]
[![Stories in Progress][waffle-progress-svg]][waffle]

Port of Biodiversity gem (a scientific names parser written in ruby) to Scala

Global Names Parser is approaching the first public release

Requirements
------------

Java SDK, Scala >= 2.11.6, SBT

Install and Run Tests
---------------------

There is no official package yet.

```
git clone https://github.com/GlobalNamesArchitecture/gnparser.git
cd gnparser
sbt test
```

Local Usage
-----------

The project consist of three parts:
* `parser` contains core routines for parsing input string
* `examples` contains usage samples for some popular programming languages
* `runner` contains code required to run `parser` from command line as
standalone util or TCP/IP server

`sbt publish-local` puts `parser` artifact (`global-names-parser-0.1.0-SNAPSHOT.jar`)
to local ivy cache (by default it is located at `$HOME/.ivy2/local/org.globalnames`).
After that you can use `parser` facilities on the same machine by adding
library dependency:

```
libraryDependencies += "org.globalnames" %% "global-names-parser" % "0.1.0-SNAPSHOT"
```

Using CLI
---------

CLI stands for Command Line Interface. To create one for `runner` project run `sbt stage`.
It then can be found at `runner/target/universal/stage/bin/gnparse`.

## Usage of Executable

`gnparse -help` gives detailed help on how to use `gnparse` cli.

### To parse one name

```
gnparse "Homo sapiens L."
```

### To parse names from a file

File should have one name per line

```
gnparse --input path_to_file --output out.txt
```

### To start socket server

```
gnparse --server --port 5555
```

Usage of the library
--------------------

```scala
import org.globalnames.parser.SciName

val parsed: SciName = SciName.fromString("Homo sapiens L.")

val parsedJson = parsed.toJson
```

Running the Examples
--------------------

All examples are stored at [examples folder][examples-folder]. There are
currently examples for Java, Jython, JRuby and R languages.

Jython, JRuby and R need reference GnParser jar. Run `sbt "parser/assembly"`
command to create fat-jar at
`parser/target/scala-2.11/global-names-parser-assembly-0.1.0-SNAPSHOT.jar`.

### Java

Java examples is an SBT subproject that is stored at [java subfolder][examples-folder/java].
To run it execute the command:

```
sbt ";project examples;run"
```

### Jython

[Jython][jython] is a Python language implementation for Java Virtual Machine.
There is no SBT subproject for Jython as there is for [Java](#Java). A distribution
should be installed locally [according to instruction][jython-installation].
Jython needs reference GnParser fat-jar. Run the
command to execute [Jython script][examples-folder/jython] (`$Jython_HOME` should be
defined):

```bash
java -jar $Jython_HOME/jython.jar \
  -Dpython.path=parser/target/scala-2.11/global-names-parser-assembly-{VERSION}.jar \
  examples/jython/parser.py
```

### R

[R is a language and environment][r-env] for statistical computing and graphics.
[Usage example][examples-folder/R] requires [rJava package][rjava] to be installed.
Example script can be run with the command:

```
Rscript examples/R/parser.R
```

### JRuby

[JRuby][jruby] is an implementation of Ruby for Java Virtual Machine. A distribution
should be installed locally [according to instruction][jruby-installation]. Jython
needs reference GnParser fat-jar. Use the following command from the project root
directory to run the example code:

```bash
jruby -J-classpath \
  parser/target/scala-2.11/global-names-parser-assembly-{VERSION}.jar \
  examples/jruby/parser.rb
```

Copyright
---------

Released under [MIT license][license]

[ci-link]: http://travis-ci.org/GlobalNamesArchitecture/gnparser
[ci-svg]: https://secure.travis-ci.org/GlobalNamesArchitecture/gnparser.svg
[examples-folder/R]: /examples/R
[examples-folder/java]: /examples/java
[examples-folder/jython]: /examples/jython/parser.py
[examples-folder]: /examples
[jruby-installation]: http://jruby.org/getting-started
[jython-installation]: https://wiki.python.org/jython/InstallationInstructions
[jython]: http://www.jython.org/
[license]: /LICENSE
[r-env]: https://www.r-project.org/about.html
[rjava]: https://cran.r-project.org/web/packages/rJava/index.html
[waffle-progress-svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=in%20progress&title=In%20Progress
[waffle-ready-svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=ready&title=Issues%20To%20Do
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
