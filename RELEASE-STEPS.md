1. sbt ";parser/assembly;runner/universal:packageBin;web/dist"
2. mkdir release-{VERSION}
3. cp runner/target/universal/gnparser-{VERSION}.zip ./release-{VERSION}
4. cp parser/target/scala-2.11/gnparser-assembly-{VERSION}.jar ./release-{VERSION}
5. cp web/target/universal/gnparser-web-{VERSION}.zip ./release-{VERSION}
6. upload release-{VERSION} contents to github
