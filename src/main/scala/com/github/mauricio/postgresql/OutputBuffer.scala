package com.github.mauricio.postgresql

import java.io.ByteArrayOutputStream
import org.jboss.netty.buffer.ChannelBuffers
import java.io.IOException
import org.jboss.netty.buffer.ChannelBuffer

class OutputBuffer {

  class WrappedByteArrayOutputStream extends ByteArrayOutputStream  {
    
    def buffer : Array[Byte] = {
      this.buf
    }    
    
  }
  
  val output = new WrappedByteArrayOutputStream()
  var bytesWritten = 0
  private val int4Buffer = new Array[Byte](4)
  private val int2Buffer = new Array[Byte](2)

  this.writeInteger4(0)
  
  def writeByte( value : Byte ) : Unit = {
    output.write(value)
    this.bytesWritten += 1
  }
  

  def writeCString( value : String ) : Unit = {
    this.write( value.getBytes("UTF-8") )
    this.writeByte(0)
  }
  
  def writeInteger4(value: Int): Unit = {
    this.int4Buffer(0) = (value >>> 24).asInstanceOf[Byte]
    this.int4Buffer(1) = (value >>> 16).asInstanceOf[Byte]
    this.int4Buffer(2) = (value >>> 8).asInstanceOf[Byte]
    this.int4Buffer(3) = (value).asInstanceOf[Byte]
    this.write(this.int4Buffer)
    this.bytesWritten += 4
  }
  
  def write( bytes : Array[Byte] ) : Unit = {
    this.output.write( bytes )
    this.bytesWritten += bytes.length
  }
  
  def writeInteger2( value : Int ) : Unit = {
    
    if ( value < Short.MinValue || value > Short.MaxValue ) {
      throw new IOException("Tried to send an out-of-range integer as a 2-byte value: " + value);
    }
    
    this.int2Buffer(0) = ( value >>> 8 ).asInstanceOf[Byte]
    this.int2Buffer(1) = value.asInstanceOf[Byte]
    
    this.write(this.int2Buffer)    
  }
  
  def toBuffer : ChannelBuffer = {    
    val buffer = this.output.buffer
   
    val value = this.output.size()
    buffer(0) = (value >>> 24).asInstanceOf[Byte]
    buffer(1) = (value >>> 16).asInstanceOf[Byte]
    buffer(2) = (value >>> 8).asInstanceOf[Byte]
    buffer(3) = (value).asInstanceOf[Byte]
    
    ChannelBuffers.wrappedBuffer( buffer, 0, this.output.size() )
  }
  
}