package com.github.mauricio.async.db.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Log {

  fun get(): Logger {
    TODO()
  }

  fun getByName(name: String): Logger {
    return LoggerFactory.getLogger(name)
  }

}
