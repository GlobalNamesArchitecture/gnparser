Apache Spark
------------

To run these examples `download <http://spark.apache.org/downloads.html>`_
and install Apache Spark version ``1.6.1``

Scala
~~~~~

`Apache Spark example
</spark/src/main/scala/org/globalnames/parser/examples/ParserSpark.scala>`_
is an SBT subproject. To run it complete steps as follows:

1. build a fat-jar of the example with ``sbt ";++2.10.6;exampleSpark/assembly"``

2. run it with Spark by executing ``$SPARK_HOME/bin/spark-submit ./examples/spark/target/scala-2.10/gnparser-example-spark-assembly-0.3.0-SNAPSHOT.jar``

Python
~~~~~~

1. build a fat-jar of the ``gnparser's spark-python`` project with ``sbt ";++2.10.6;sparkPython/assembly"``. The project provides a thin wrapper for allowing transformation of input (`RDD[String]` scientific names) to the output (`RDD[String]` parsed results in compact JSON format).

2. run ``pyspark`` with command:

.. code:: bash

    $SPARK_HOME/bin/pyspark \
      --jars "`pwd`/spark-python/target/scala-2.10/gnparser-spark-python-assembly-0.3.0-SNAPSHOT.jar" \
      --driver-class-path="`pwd`/spark-python/target/scala-2.10/gnparser-spark-python-assembly-0.3.0-SNAPSHOT.jar"`

3. add Python snippet to call the wrapper:

.. code:: python

    def parse(names):
        from pyspark.mllib.common import _py2java, _java2py
        parser = sc._jvm.org.globalnames.parser.spark.Parser()
        result = parser.parse(_py2java(sc, names))
        return _java2py(sc, result)

4. now scientific name strings can be parsed in your program as follows:

.. code:: python

    names = sc.parallelize(["Homo sapiens Linnaeus 1758",
                            "Salinator solida (Martens, 1878)",
                            "Taraxacum officinale F. H. Wigg."])

    import json
    canonical_names = parse(names) \
      .map(lambda r: json.loads(r)) \
      .map(lambda j: (j["name_string_id"], j["canonical_name"]["value"])) \
      .collect()

    println canonical_names

    # [(u'208eb0ea-40e3-5894-9b7d-664721bd24e6', u'Homo sapiens'),
    #  (u'b0f8459f-8b73-514c-b6f3-568d54d99ded', u'Salinator solida'),
    #  (u'c2ab9908-ea25-57e1-835a-06b9d1ade53b', u'Taraxacum officinale')]
