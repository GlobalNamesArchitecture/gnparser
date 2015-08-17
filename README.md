Global Names Parser
===================

[![Continuous Integration Status][ci_svg]][ci_link]
[![Stories in Ready][waffle_svg]][waffle]

Port of Biodiversity gem (a scientific names parser written in ruby)

Please note that gnparser is at the very early stage of development

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
Use on a local machine
----------------------

First run

```
sbt publish-local
```
It will create package at `$HOME/.ivy2/local/org.globalnames`

You can use the package on the same machine by adding to your sbt file

```
libraryDependencies += "org.globalnames" %% "gnparser" % "0.1.0-SNAPSHOT"
```

Create Executable jar
---------------------

```
sbt assembly
```

If all goes well sbt output will tell the path to the jar file

Usage of executable
-------------------

### To parse one name

```
java -jar GnParser-assembly-x.y.z.jar "Homo sapiens L."
```

### To parse names from a file

File should have one name per line

```
java -jar GnParser-assembly-x.y.z.jar --input path_to_file --output out.txt
```

### To start socket server

```
java -jar GnParser-assembly-x.y.z.jar --server --port 5555
```

Usage of the library
--------------------

```scala
import org.globalnames.parser.SciName

val parsed: SciName = SciName.fromString("Homo sapiens L.")

val parsedJson = parsed.toJson
```

Copyright
---------

Released under [MIT license][license]

[license]: /LICENSE

[ci_svg]: https://secure.travis-ci.org/GlobalNamesArchitecture/gnparser.svg
[ci_link]: http://travis-ci.org/GlobalNamesArchitecture/gnparser
[waffle_svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=ready&title=Issues
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
