package jugger.handlers

import jugger.interfaces.JwtTokenProvider
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

@Component
class WebSocketAuthInterceptor(
    private val jwtTokenProvider: JwtTokenProvider
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        accessor?.let {
            if (it.command == StompCommand.CONNECT) {
                val authToken = it.getFirstNativeHeader("Authorization")

                if (authToken != null && authToken.startsWith("Bearer ")) {
                    val token = authToken.substring(7)
                    try {
                        // Проверка и получение Authentication
                        val authentication = jwtTokenProvider.validateAndGetAuthentication(token)
                        accessor.user = authentication
                    } catch (e: Exception) {
                        // Логирование ошибки
                        println("WebSocket Authentication Error: ${e.message}")
                        throw MessagingException("Invalid token", e)
                    }
                } else {
                    throw MessagingException("No token provided")
                }
            }
        }

        return message
    }
}
