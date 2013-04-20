package com.github.mauricio.async.db.postgresql.util

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.util.{ArrayStreamingParser, ArrayStreamingParserDelegate}
import scala.collection.mutable.ArrayBuffer

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 1:47 PM
 */
class ArrayStreamingParserSpec extends Specification {

  val parser = ArrayStreamingParser

  "parser" should {

    "generate the events correctly" in {

      val content = "{{1,2,3},{4,5,6}}"

      val delegate = new LoggingDelegate()
      parser.parse(content, delegate)

      delegate.starts === 3
      delegate.ends === 3
      delegate.items === ArrayBuffer("{", "{", "1", "2", "3", "}", "{", "4", "5", "6","}","}")
    }

    "should parse a varchar array correctly" in {
      val content = """{{"item","is here","but\"not there"},{"so","this is your last step"},{""}}"""

      val delegate = new LoggingDelegate()
      parser.parse(content, delegate)

      delegate.items === ArrayBuffer( "{", "{", "item", "is here", "but\"not there", "}", "{", "so", "this is your last step", "}", "{", "", "}","}" )
      delegate.starts === 4
      delegate.ends === 4
    }

    "should parse a varchar array with nulls correctly" in {
      val content = """{NULL,"first",NULL,"second","NULL",NULL}"""

      val delegate = new LoggingDelegate()
      parser.parse(content, delegate)

      delegate.items === ArrayBuffer( "{", null, "first", null, "second", "NULL", null, "}" )
    }

  }

}

class LoggingDelegate extends ArrayStreamingParserDelegate {

  val items = new ArrayBuffer[String]()
  var starts = 0
  var ends = 0

  override def arrayStarted {
    items += "{"
    starts += 1
  }

  override def arrayEnded {
    items += "}"
    ends += 1
  }

  override def elementFound(element: String) {
    items += element
  }

  override def nullElementFound {
    items += null
  }
}
