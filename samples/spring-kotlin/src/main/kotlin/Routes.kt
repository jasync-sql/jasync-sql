package me.pgs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.router

@Configuration
class Routes(private val userHandler: UserHandler) {
    @Bean
    fun routerFunction() = router {
        GET("/test").nest {
            accept(APPLICATION_JSON, userHandler::getUsers)
        }
    }
}
