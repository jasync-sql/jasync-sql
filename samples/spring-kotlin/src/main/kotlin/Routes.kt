package me.pgs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_STREAM_JSON
import org.springframework.web.reactive.function.server.router

@Configuration
class Routes(val userHandler: UserHandler) {
    @Bean
    fun Router() = router {
        GET("/test").nest {
            accept(APPLICATION_STREAM_JSON, userHandler::getUsers)
        }
    }

}