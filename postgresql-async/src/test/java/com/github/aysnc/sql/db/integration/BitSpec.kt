package com.github.aysnc.sql.db.integration

import org.junit.Test


class BitSpec {//: DatabaseTestHelper() {

    @Test
    fun `"when processing bit columns" should "result in binary data"`() {


//        withHandler { handler ->
//            val create = """CREATE TEMP TABLE binary_test
//                         (
//                           id bigserial NOT NULL,
//                           some_bit BYTEA NOT NULL,
//                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id)
//                         )"""
//
//            executeDdl(handler, create)
//            executePreparedStatement(handler,
//                    "INSERT INTO binary_test (some_bit) VALUES (E'\\\\000'),(E'\\\\001')")
//
//            val rows = executePreparedStatement(handler, "select * from binary_test")!!.rows!!
//
//            val bit0 = rows[0]("some_bit")
//            val bit1 = rows[1]("some_bit")
//
//            assertThat(bit0).isEqualTo(byteArrayOf(0))
//            assertThat(bit1).isEqualTo(byteArrayOf(1))
//        }

    }

//    @Test
//    fun `"when processing bit columns" should "result in binary data in BIT(2) column"`() {
//
//        withHandler { handler ->
//            val create = """CREATE TEMP TABLE binary_test
//                         (
//                           id bigserial NOT NULL,
//                           some_bit BYTEA NOT NULL,
//                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id)
//                         )"""
//
//            executeDdl(handler, create)
//            executePreparedStatement(handler,
//                    "INSERT INTO binary_test (some_bit) VALUES (E'\\\\000'),(E'\\\\001'),(E'\\\\002'),(E'\\\\003')")
//
//            val rows = executePreparedStatement(handler, "select * from binary_test")!!.rows!!
//
//            val bit0 = rows[0]("some_bit")
//            val bit1 = rows[1]("some_bit")
//            val bit2 = rows[2]("some_bit")
//            val bit3 = rows[3]("some_bit")
//
//            assertThat(bit0).isEqualTo(byteArrayOf(0))
//            assertThat(bit1).isEqualTo(byteArrayOf(1))
//            assertThat(bit2).isEqualTo(byteArrayOf(2))
//            assertThat(bit3).isEqualTo(byteArrayOf(3))
//        }
//
//    }

}


