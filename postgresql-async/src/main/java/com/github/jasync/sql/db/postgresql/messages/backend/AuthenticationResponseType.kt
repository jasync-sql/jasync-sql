package com.github.jasync.sql.db.postgresql.messages.backend

//object AuthenticationResponseType : Enumeration {
//  type AuthenticationResponseType = Value
//  val MD5, Cleartext, Ok = Value
//}

enum class AuthenticationResponseType {
    MD5,
    Cleartext,
    Ok
}
