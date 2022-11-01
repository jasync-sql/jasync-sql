package me.pgs

import org.springframework.data.annotation.Id

data class User(
    @Id
    val username: String,
    val password: String
)
