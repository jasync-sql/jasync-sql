package com.github.mauricio.postgresql.util

import org.specs2.mutable.Specification

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 1:32 PM
 */

class FutureResultSpec extends Specification {

  "future" should {

    "return the value when it is a success" in {

      val success = FutureSuccess("ME!")
      success() === "ME!"
    }

    "raise an exception when trying to get an error a success" in {

      try {
        val result = FutureSuccess("ME!")
        result.getFailure
      } catch {
        case e : IllegalStateException => success
      }
    }

    "grab an exception from the failure" in {
      val exception = new RuntimeException()
      val failure = FutureFailure(exception)
      failure.getFailure === exception
    }

    "raise an exception when trying to grab a value from a failure" in {
      val exception = new RuntimeException()
      val failure = FutureFailure(exception)

      try {
        failure()
      } catch {
        case e : IllegalStateException => success
      }
    }

  }

}
