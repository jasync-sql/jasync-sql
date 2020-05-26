package com.github.jasync.sql.db.mysql.message.server

import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.general.ColumnData
import com.github.jasync.sql.db.mysql.binary.decoder.BinaryDecoder
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.util.CharsetMapper

data class ColumnDefinitionMessage(
    val catalog: String,
    val schema: String,
    val table: String,
    val originalTable: String,
    override val name: String,
    val originalName: String,
    val characterSet: Int,
    val columnLength: Long,
    val columnType: Int,
    val flags: Short,
    val decimals: Byte,
    val binaryDecoder: BinaryDecoder,
    val textDecoder: ColumnDecoder
) : ServerMessage(ServerMessage.ColumnDefinition), ColumnData {

    override fun dataType(): Int = this.columnType
    override fun dataTypeSize(): Long = this.columnLength

    override fun toString(): String {
        val columnTypeName = ColumnTypes.Mapping.getOrElse(columnType) { columnType }
        val charsetName = CharsetMapper.DefaultCharsetsById.getOrElse(characterSet) { characterSet }

        return "${this::class.java.simpleName}(name=$name,columnType=$columnTypeName,table=$table,charset=$charsetName,decimals=$decimals})"
    }
}
