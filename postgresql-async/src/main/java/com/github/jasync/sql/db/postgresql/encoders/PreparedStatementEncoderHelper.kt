package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.length
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}


interface PreparedStatementEncoderHelper {

    companion object {
        const val Portal = 'P'
        const val Statement = 'S'
    }

    fun writeExecutePortal(
        statementIdBytes: ByteArray,
        query: String,
        values: List<Any?>,
        encoder: ColumnEncoderRegistry,
        charset: Charset,
        writeDescribe: Boolean = false
    ): ByteBuf {

        logger.debug("Preparing execute portal to statement ($query) - values (${values.joinToString(", ")}) - $charset")

        val bindBuffer = Unpooled.buffer(1024)

        bindBuffer.writeByte(ServerMessage.Bind)
        bindBuffer.writeInt(0)

        bindBuffer.writeBytes(statementIdBytes)
        bindBuffer.writeByte(0)
        bindBuffer.writeBytes(statementIdBytes)
        bindBuffer.writeByte(0)

        bindBuffer.writeShort(0)

        bindBuffer.writeShort(values.length)

        val decodedValues = if (logger.isDebugEnabled) {
            mutableListOf<String?>()
        } else {
            null
        }

        for (value in values) {
            if (value == null) {
                bindBuffer.writeInt(-1)

                if (logger.isDebugEnabled) {
                    decodedValues?.add(null)
                }
            } else {
                val encodedValue = encoder.encode(value)

                if (logger.isDebugEnabled) {
                    decodedValues?.add(encodedValue)
                }

                if (encodedValue == null) {
                    bindBuffer.writeInt(-1)
                } else {
                    val content = encodedValue.toByteArray(charset)
                    bindBuffer.writeInt(content.length)
                    bindBuffer.writeBytes(content)
                }

            }
        }

        if (logger.isDebugEnabled) {
            logger.debug(
                "Executing portal - statement id " +
                        "(${statementIdBytes.joinToString("-")}) - statement ($query) - " +
                        "encoded values (${decodedValues?.joinToString(", ")}) - original values (${values.joinToString(
                            ","
                        )})"
            )
        }

        bindBuffer.writeShort(0)

        ByteBufferUtils.writeLength(bindBuffer)

        if (writeDescribe) {
            val describeLength = 1 + 4 + 1 + statementIdBytes.length + 1
            @Suppress("UnnecessaryVariable")
            val describeBuffer = bindBuffer
            describeBuffer.writeByte(ServerMessage.Describe)
            describeBuffer.writeInt(describeLength - 1)
            describeBuffer.writeByte('P'.toInt())
            describeBuffer.writeBytes(statementIdBytes)
            describeBuffer.writeByte(0)
        }

        val executeLength = 1 + 4 + statementIdBytes.length + 1 + 4
        val executeBuffer = Unpooled.buffer(executeLength)
        executeBuffer.writeByte(ServerMessage.Execute)
        executeBuffer.writeInt(executeLength - 1)
        executeBuffer.writeBytes(statementIdBytes)
        executeBuffer.writeByte(0)
        executeBuffer.writeInt(0)

        val closeAndSync = closeAndSyncBuffer(statementIdBytes, Portal)

        return Unpooled.wrappedBuffer(bindBuffer, executeBuffer, closeAndSync)
    }

    fun closeAndSyncBuffer(statementIdBytes: ByteArray, closeType: Char): ByteBuf {
        val closeLength = 1 + 4 + 1 + statementIdBytes.length + 1
        val closeBuffer = Unpooled.buffer(closeLength)
        closeBuffer.writeByte(ServerMessage.CloseStatementOrPortal)
        closeBuffer.writeInt(closeLength - 1)
        closeBuffer.writeByte(closeType.toInt())
        closeBuffer.writeBytes(statementIdBytes)
        closeBuffer.writeByte(0)

        val syncBuffer = Unpooled.buffer(5)
        syncBuffer.writeByte(ServerMessage.Sync)
        syncBuffer.writeInt(4)

        return Unpooled.wrappedBuffer(closeBuffer, syncBuffer)

    }

}
