package org.globalnames

import java.util.UUID

import com.fasterxml.uuid.{Generators, StringArgGenerator}

object UuidGenerator {
  private val uuidGenerator: StringArgGenerator = {
    val namespace = UUID.fromString("90181196-fecf-5082-a4c1-411d4f314cda")
    Generators.nameBasedGenerator(namespace)
  }

  def generate(name: String): UUID = uuidGenerator.generate(name)

  val EmptyUuid: UUID = generate("")
}
