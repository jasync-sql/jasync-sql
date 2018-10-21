package me.pgs

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import org.springframework.web.reactive.function.server.body

@Component
class UserHandler(val db: DB) {

    fun getUsers(req: ServerRequest) =
        ok().body(Mono.fromFuture(db.connectionPool.sendPreparedStatement("select * from user;")).map { it.rows.orEmpty() })
}