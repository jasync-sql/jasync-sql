package com.github.jasync.sql.db.exceptions

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter", "RedundantVisibilityModifier")
public class UnsupportedAuthenticationMethodException(val authenticationType: String) :
    DatabaseException("Unknown authentication method -> '%s'".format(authenticationType)) {

    constructor(authType: Int) :
            this(authType.toString())


}
