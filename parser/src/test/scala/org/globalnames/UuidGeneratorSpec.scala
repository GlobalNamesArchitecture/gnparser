package org.globalnames

import java.util.UUID

import org.specs2.mutable.Specification

class UuidGeneratorSpec extends Specification {
  "Generates UUID correctly" >> {
    UuidGenerator.generate("Pseudocercospora Speg.") ===
      UUID.fromString("ccc7780b-c68b-53c6-9166-6b2d4902923e")
  }
}
