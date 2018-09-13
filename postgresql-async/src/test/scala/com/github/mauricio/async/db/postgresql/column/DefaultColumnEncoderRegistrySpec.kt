
package com.github.mauricio.async.db.postgresql.column

import org.specs2.mutable.Specification

class DefaultColumnEncoderRegistrySpec : Specification {

  val registry = PostgreSQLColumnEncoderRegistry()

  "registry" should {

    "correctly render an array of strings , nulls" in {
      val items = Array( "some", """text \ hoes " here to be seen""", null, "all, right" )
      registry.encode( items ) === """{"some","text \\ hoes \" here to be seen",NULL,"all, right"}"""
    }

    "correctly render an array of numbers" in {
      val items = Array(Array(1,2,3),Array(4,5,6),Array(7,null,8))
      registry.encode( items ) === "{{1,2,3},{4,5,6},{7,NULL,8}}"
    }

  }

}