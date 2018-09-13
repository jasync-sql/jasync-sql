package com.github.aysnc.sql.db.postgresql.util

import com.github.jasync.sql.db.postgresql.util.ArrayStreamingParser
import com.github.jasync.sql.db.postgresql.util.ArrayStreamingParserDelegate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class ArrayStreamingParserSpec {

  val parser = ArrayStreamingParser


  @Test
  fun `"parser" should" generate the events correctly"`() {

    val content = "{{1,2,3},{4,5,6}}"

    val delegate = LoggingDelegate()
    parser.parse(content, delegate)

    assertThat(delegate.starts).isEqualTo(3)
    assertThat(delegate.ends).isEqualTo(3)
    assertThat(delegate.items).isEqualTo(listOf("{", "{", "1", "2", "3", "}", "{", "4", "5", "6", "}", "}"))
  }

  @Test
  fun `"parser" should"     "should parse a varchar array correctly"`() {
    val content = """{{"item","is here","but\"not there"},{"so","this is your last step"},{""}}"""

    val delegate = LoggingDelegate()
    parser.parse(content, delegate)

    assertThat(delegate.items).isEqualTo(listOf("{", "{", "item", "is here", "but\"not there", "}", "{", "so", "this is your last step", "}", "{", "", "}", "}"))
    assertThat(delegate.starts).isEqualTo(4)
    assertThat(delegate.ends).isEqualTo(4)
  }

  @Test
  fun `"parser" should"     "should parse a varchar array , nulls correctly"`() {
    val content = """{NULL,"first",NULL,"second","NULL",NULL}"""

    val delegate = LoggingDelegate()
    parser.parse(content, delegate)

    assertThat(delegate.items).isEqualTo(listOf("{", null, "first", null, "second", "NULL", null, "}"))
  }

}


class LoggingDelegate : ArrayStreamingParserDelegate {

  val items = mutableListOf<String?>()
  var starts = 0
  var ends = 0

  override fun arrayStarted() {
    items += "{"
    starts += 1
  }

  override fun arrayEnded() {
    items += "}"
    ends += 1
  }

  override fun elementFound(element: String) {
    items += element
  }

  override fun nullElementFound() {
    items.add(null)
  }
}
