package me.pgs

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class UserHandler(private val userRepository: UserRepository) {
    fun getUsers(serverRequest: ServerRequest): Mono<ServerResponse> =
        ServerResponse.ok().body(userRepository.findAll(), User::class.java)
}
