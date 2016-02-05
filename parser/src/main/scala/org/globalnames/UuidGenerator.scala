package org.globalnames

import java.util.UUID

import com.fasterxml.uuid.{Generators, StringArgGenerator}

class UuidGenerator private[UuidGenerator]() {
  private val uuidGenerator: StringArgGenerator = {
    val namespace = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")
    Generators.nameBasedGenerator(namespace)
  }

  def generate(name: String): UUID = uuidGenerator.generate(name)
}

object UuidGenerator {
  def apply() = new UuidGenerator
}
