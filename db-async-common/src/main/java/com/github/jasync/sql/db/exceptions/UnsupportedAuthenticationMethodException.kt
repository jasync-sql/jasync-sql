package com.github.jasync.sql.db.exceptions

class UnsupportedAuthenticationMethodException(val authenticationType: String) :
    DatabaseException("Unknown authentication method -> '%s'".format(authenticationType)) {

    constructor(authType: Int) :
            this(authType.toString())


}
