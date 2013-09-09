/*
 * Copyright 2013 Norman Maurer
 *
 * Norman Maurer, licenses this file to you under the Apache License,
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
package com.github.mauricio.async.db.mysql.codec

import io.netty.buffer.{CompositeByteBuf, UnpooledByteBufAllocator, ByteBuf, ByteBufAllocator}
import java.nio.ByteOrder

object LittleEndianByteBufAllocator {
  val INSTANCE = new LittleEndianByteBufAllocator
}

/**
 * Allocates ByteBuf which have LITTLE_ENDIAN order.
 */
class LittleEndianByteBufAllocator extends ByteBufAllocator {
  private val allocator = new UnpooledByteBufAllocator(false)

  def isDirectBufferPooled: Boolean = false

  def buffer() = littleEndian(allocator.buffer())

  def buffer(initialCapacity: Int) = littleEndian(allocator.buffer(initialCapacity))

  def buffer(initialCapacity: Int, maxCapacity: Int) = littleEndian(allocator.buffer(initialCapacity, maxCapacity))

  def ioBuffer() = littleEndian(allocator.ioBuffer())

  def ioBuffer(initialCapacity: Int) = littleEndian(allocator.ioBuffer(initialCapacity))

  def ioBuffer(initialCapacity: Int, maxCapacity: Int) = littleEndian(allocator.ioBuffer(initialCapacity, maxCapacity))

  def heapBuffer() = littleEndian(allocator.heapBuffer())

  def heapBuffer(initialCapacity: Int) = littleEndian(allocator.heapBuffer(initialCapacity))

  def heapBuffer(initialCapacity: Int, maxCapacity: Int) = littleEndian(allocator.heapBuffer(initialCapacity, maxCapacity))

  def directBuffer() = littleEndian(allocator.directBuffer())

  def directBuffer(initialCapacity: Int) = littleEndian(allocator.directBuffer(initialCapacity))

  def directBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf = littleEndian(allocator.directBuffer(initialCapacity, maxCapacity))

  def compositeBuffer() = allocator.compositeBuffer()

  def compositeBuffer(maxNumComponents: Int) = allocator.compositeBuffer(maxNumComponents)

  def compositeHeapBuffer() = allocator.compositeHeapBuffer()

  def compositeHeapBuffer(maxNumComponents: Int) = allocator.compositeHeapBuffer(maxNumComponents)

  def compositeDirectBuffer() = allocator.compositeDirectBuffer()

  def compositeDirectBuffer(maxNumComponents: Int): CompositeByteBuf = allocator.compositeDirectBuffer(maxNumComponents)

  private def littleEndian(b: ByteBuf) = b.order(ByteOrder.LITTLE_ENDIAN)

}
