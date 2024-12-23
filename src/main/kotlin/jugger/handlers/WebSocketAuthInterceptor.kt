package jugger.handlers

import jugger.interfaces.JwtTokenProvider
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(WebSocketAuthInterceptor::class.java)

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        logger.info("Intercepting message:")
        logger.info("Message headers: ${message.headers}")

        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        accessor?.let {
            logger.info("STOMP Command: ${it.command}")

            if (it.command == StompCommand.CONNECT) {
                val authToken = it.getFirstNativeHeader("Authorization")
                logger.info("Authorization header: $authToken")

                if (authToken != null && authToken.startsWith("Bearer ")) {
                    val token = authToken.substring(7)
                    try {
                        logger.info("Validating token: $token")
                        val authentication = jwtTokenProvider.validateAndGetAuthentication(token)

                        logger.info("Authentication successful:")
                        logger.info("Principal: ${authentication.principal}")
                        logger.info("Authorities: ${authentication.authorities}")

                        accessor.user = authentication
                    } catch (e: Exception) {
                        logger.error("WebSocket Authentication Error", e)
                        throw MessagingException("Invalid token", e)
                    }
                } else {
                    logger.error("No valid token provided")
                    throw MessagingException("No token provided")
                }
            }
        }

        return message
    }
}
