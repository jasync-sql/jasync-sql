package com.github.jasync.sql.db.pool

class MaxTtlPassedException(id: String, age: Long, maxTtl: Long) :
    RuntimeException("Object $id passed max ttl with age $age over maxTtl $maxTtl")
