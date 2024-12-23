package jugger.handlers

import jugger.interfaces.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest

@Component
class WebSocketAuthHandler(
    private val jwtTokenProvider: JwtTokenProvider
) : HandshakeInterceptor {

    private val logger = LoggerFactory.getLogger(WebSocketAuthHandler::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            val servletRequest = request.servletRequest
            val authHeader = servletRequest.getHeader("Authorization")

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)
                try {
                    // Валидация токена и получение аутентификации
                    val authentication = jwtTokenProvider.validateAndGetAuthentication(token)

                    // Установка контекста безопасности
                    SecurityContextHolder.getContext().authentication = authentication

                    logger.info("WebSocket authentication successful")
                    return true
                } catch (e: Exception) {
                    logger.error("WebSocket authentication failed", e)
                    return false
                }
            }
        }
        return false
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        ex: Exception?
    ) {
        ex?.let {
            logger.error("WebSocket handshake error", it)
        }
    }
}
