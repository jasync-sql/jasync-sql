package com.github.jasync.sql.db.pool

class ObjectAgedOutException(obj: PooledObject, age: Long, maxTtl: Long) :
    RuntimeException("Object ${obj.id} aged out of pool with age $age over maxTtl $maxTtl") {}