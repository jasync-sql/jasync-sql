package com.github.mauricio.async.db.mysql.codec

import io.netty.buffer.CompositeByteBuf
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.nio.ByteOrder


/**
 * Allocates ByteBuf which have LITTLE_ENDIAN order.
 */
class LittleEndianByteBufAllocator : ByteBufAllocator {

  companion object {
    val INSTANCE = LittleEndianByteBufAllocator
  }
  private val allocator = UnpooledByteBufAllocator(false)

  override fun isDirectBufferPooled(): Boolean = false

  override fun buffer() = littleEndian(allocator.buffer())

  override fun buffer(initialCapacity: Int) = littleEndian(allocator.buffer(initialCapacity))

  override fun buffer(initialCapacity: Int, maxCapacity: Int) = littleEndian(allocator.buffer(initialCapacity, maxCapacity))

  override fun ioBuffer() = littleEndian(allocator.ioBuffer())

  override fun ioBuffer(initialCapacity: Int) = littleEndian(allocator.ioBuffer(initialCapacity))

  override fun ioBuffer(initialCapacity: Int, maxCapacity: Int) = littleEndian(allocator.ioBuffer(initialCapacity, maxCapacity))

  override fun heapBuffer() = littleEndian(allocator.heapBuffer())

  override fun heapBuffer(initialCapacity: Int) = littleEndian(allocator.heapBuffer(initialCapacity))

  override fun heapBuffer(initialCapacity: Int, maxCapacity: Int) = littleEndian(allocator.heapBuffer(initialCapacity, maxCapacity))

  override fun directBuffer() = littleEndian(allocator.directBuffer())

  override fun directBuffer(initialCapacity: Int) = littleEndian(allocator.directBuffer(initialCapacity))

  override fun directBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf = littleEndian(allocator.directBuffer(initialCapacity, maxCapacity))

  override fun compositeBuffer(): CompositeByteBuf = allocator.compositeBuffer()

  override fun compositeBuffer(maxNumComponents: Int): CompositeByteBuf = allocator.compositeBuffer(maxNumComponents)

  override fun compositeHeapBuffer(): CompositeByteBuf = allocator.compositeHeapBuffer()

  override fun compositeHeapBuffer(maxNumComponents: Int): CompositeByteBuf = allocator.compositeHeapBuffer(maxNumComponents)

  override fun compositeDirectBuffer(): CompositeByteBuf = allocator.compositeDirectBuffer()

  override fun compositeDirectBuffer(maxNumComponents: Int): CompositeByteBuf = allocator.compositeDirectBuffer(maxNumComponents)

  override fun calculateNewCapacity(minNewCapacity: Int, maxCapacity: Int): Int = allocator.calculateNewCapacity(minNewCapacity, maxCapacity)

  private fun littleEndian(b: ByteBuf): ByteBuf = b.order(ByteOrder.LITTLE_ENDIAN)

}
