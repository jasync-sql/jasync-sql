/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.util

/**
 *
 * Simplifies the represenation of a collection of flags. You should subclass this
 * class and give it a default collection of flags.
 *
 * @param mask
 * @param possibleValues
 */

abstract class Flag[T](mask: Int, possibleValues: Map[String, Int]) {

  def +(flag: Int) : T = create(mask | flag)

  def -(flag: Int) : T = create(mask & ~flag)

  def has(flag: Int) : Boolean = hasAll(flag)

  protected def create( value : Int ) : T

  def hasAll(flags: Int*) : Boolean = flags.map {
    f => (f & mask) > 0
  }.reduceLeft {
    _ && _
  }

  override def toString() : String = {
    val cs = possibleValues.filter {
      case (key, value) => has(value)
    }.map {
      case (key,value) => key
    }.mkString (", ")
    s"${this.getClass.getSimpleName}(" + mask + ": " + cs + ")"
  }
}
