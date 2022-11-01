package com.github.jasync.sql.db.exceptions

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "RedundantVisibilityModifier")
public class UnsupportedAuthenticationMethodException(val authenticationType: String) :
    DatabaseException("Unknown authentication method -> '$authenticationType'") {

    constructor(authType: Int) :
        this(authType.toString())
}
