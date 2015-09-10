Global Names Parser
===================

[![Continuous Integration Status][ci_svg]][ci_link]
[![Stories in Ready][waffle_ready_svg]][waffle]
[![Stories in Progress][waffle_progress_svg]][waffle]

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
### Windows OS and UUID v5 Encoding Issue

Strings with characters outside of the ASCII range generate erroneous UUIDs v5
on systems with non-UTF-8 encodings.

The reason is that calculation of UUID v5 converts a string to bytes first. 
The bytes are created using the system's default encoding. On Windows OS 
default encoding is usually Cp1251 (but not UTF-8). 

On Unix-like systems the default encoding more often then not is UTF-8 and ID
is calculated correctly.

To enforce UTF-8 encoding launch `sbt` with

```
JAVA_OPTS="-Dfile.encoding=UTF8" sbt
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
[waffle_ready_svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=ready&title=Issues%20To%20Do
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
[waffle_progress_svg]: https://badge.waffle.io/GlobalNamesArchitecture/gnparser.svg?label=in%20progress&title=In%20Progress
[waffle]: https://waffle.io/GlobalNamesArchitecture/gnparser
