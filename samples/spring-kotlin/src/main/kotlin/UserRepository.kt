package me.pgs

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface UserRepository : ReactiveCrudRepository<User, String> {
    fun findByUsernameAndPassword(username: String, password: String): Mono<User>
}
