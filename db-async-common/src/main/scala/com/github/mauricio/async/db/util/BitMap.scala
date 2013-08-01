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

import io.netty.buffer.ByteBuf

object BitMap {
  final val Bytes = Array(128, 64, 32, 16, 8, 4, 2, 1)

  def apply(bytes: Byte*): BitMap = new BitMap(bytes.toArray)

  def forSize( totalBits : Int ) : BitMap = {
    val quotient = totalBits / 8
    val remainder = totalBits % 8
    val finalSize = if ( remainder == 0 ) quotient else quotient + 1
    new BitMap( new Array[Byte](finalSize) )
  }

  def fromBuffer( totalBits : Int, buffer : ByteBuf ) : BitMap = {
    val quotient = totalBits / 8
    val remainder = totalBits % 8

    val bitMapSource = new Array[Byte](if (remainder == 0) quotient else quotient + 1)
    buffer.readBytes(bitMapSource)

    new BitMap(bitMapSource)
  }

}

/**
 *
 * Implements a bit map where you can check which bits are set and which are not.
 *
 * @param bytes
 */

class BitMap(bytes: Array[Byte]) extends IndexedSeq[(Int, Boolean)] {

  val length = bytes.length * 8

  /**
   *
   * Returns true if the bit at the given index is set, false if it is not.
   *
   * @param index the bit position, starts at 0
   * @return
   */

  def isSet(index: Int): Boolean = {
    val quotient = index / 8
    val remainder = index % 8

    (bytes(quotient) & BitMap.Bytes(remainder)) != 0
  }

  def set(index: Int) {
    val quotient = index / 8
    val remainder = index % 8

    bytes(quotient) = (bytes(quotient) | BitMap.Bytes(remainder)).asInstanceOf[Byte]
  }

  override def foreach[U](f: ((Int, Boolean)) => U) {
    var currentIndex = 0
    for (byte <- bytes) {
      var x = 0
      while (x < BitMap.Bytes.length) {
        f(currentIndex, (byte & BitMap.Bytes(x)) != 0)
        x += 1
        currentIndex += 1
      }
    }
  }

  def foreachWithLimit[U](startIndex: Int, length: Int, f: ((Int, Boolean)) => U) {
    var currentIndex = startIndex
    var start = startIndex / 8
    var x = startIndex % 8
    val limit = length + startIndex
    while (start < bytes.length) {
      val byte = this.bytes(start)
      while (x < BitMap.Bytes.length) {
        f(currentIndex, (byte & BitMap.Bytes(x)) != 0)
        x += 1
        currentIndex += 1

        if (currentIndex >= limit) {
          return
        }
      }
      x = 0
      start += 1
    }
  }

  def apply(idx: Int): (Int, Boolean) = (idx, this.isSet(idx))

  override def toString: String = this.map(entry => if (entry._2) '1' else '0').mkString("")

  def write( buffer : ByteBuf ) {
    buffer.writeBytes(bytes)
  }

}
