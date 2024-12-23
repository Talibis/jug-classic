package jugger.config

import com.fasterxml.jackson.databind.ObjectMapper
import jugger.handlers.WebSocketAuthHandler
import jugger.handlers.WebSocketHandler
import jugger.services.ChatService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val webSocketHandler: WebSocketHandler,
    private val webSocketAuthHandler: WebSocketAuthHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler, "/chat")
            .addInterceptors(webSocketAuthHandler)
            .setAllowedOrigins("*")
    }
}

