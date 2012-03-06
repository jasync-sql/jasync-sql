package com.github.mauricio.postgresql.column

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:34 AM
 */

class ColumnDecoderNotFoundException( kind : Int )
  extends IllegalArgumentException( "There is no decoder available for kind %s".format(kind) )

object ColumnDecoder {

  val Bigserial = 20
  val Smallint = 21
  val Integer = 23
  val Numeric = 1700 // Decimal is the same as Numeric on PostgreSQL
  val Real = 700
  val Double = 701
  val Serial = 23
  val Varchar = 1043 // Char is the same as Varchar on PostgreSQL
  val Text = 25
  val Timestamp = 1114
  val Date = 1082
  val Time = 1083
  val Boolean = 16

  def decoderFor(kind: Int): ColumnDecoder = {
    kind match {
      case Boolean => BooleanDecoder
      case Bigserial => LongDecoder
      case Smallint => IntegerDecoder
      case Integer => IntegerDecoder
      case Text => StringDecoder
      case Real => FloatDecoder
      case Double => DoubleDecoder
      case Varchar => StringDecoder
      case Numeric => BigDecimalDecoder
      case Timestamp => TimestampDecoder
      case Date => DateDecoder
      case Time => TimeDecoder
      case _ => throw new ColumnDecoderNotFoundException(kind)
    }
  }

  def decode( kind : Int, value : String ) : Any = {
    decoderFor(kind).decode(value)
  }

}

trait ColumnDecoder {

  def decode(value: String): Any

}
