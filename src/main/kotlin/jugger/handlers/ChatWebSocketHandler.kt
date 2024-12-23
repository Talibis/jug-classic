package jugger.handlers

import jugger.services.ChatService
import jugger.services.UserService
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory  // Правильный импорт
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.CloseStatus

@Component
class ChatWebSocketHandler(
    private val chatService: ChatService,
    private val userService: UserService,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)

    @Throws(Exception::class)
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val token = session.attributes["TOKEN"] as? String
                ?: throw IllegalArgumentException("No token in session")

            // Более детальная проверка типов
            val rawPayload = objectMapper.readValue(message.payload, Map::class.java)

            val payload = rawPayload.mapKeys { it.key.toString() }
                .mapValues { it.value?.toString() }

            val messageData = mapOf(
                "type" to (payload["type"]
                    ?: throw IllegalArgumentException("Message type is required")),
                "content" to (payload["content"]
                    ?: throw IllegalArgumentException("Message content is required"))
            )

            val chatMessage = chatService.processMessage(token, messageData)
            logger.info("Message processed: ${chatMessage.id}")
        } catch (e: Exception) {
            logger.error("Error processing message", e)
            session.sendMessage(TextMessage("Error: ${e.message}"))
        }
    }


    @Throws(Exception::class)
    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            // Извлечение токена из заголовков при установлении соединения
            val token = session.handshakeHeaders.getFirst("Authorization")
                ?.removePrefix("Bearer ")
                ?: throw IllegalArgumentException("No authorization token")

            // Сохранение токена в атрибутах сессии для последующего использования
            session.attributes["TOKEN"] = token

            chatService.handleNewConnection(session, token)
            logger.info("New WebSocket connection established")
        } catch (e: Exception) {
            logger.error("Error establishing connection", e)
            session.close()
        }
    }

    @Throws(Exception::class)
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        try {
            val user = userService.getUserFromSession(session)
            chatService.removeSession(session)
            logger.info("WebSocket connection closed for user: ${user.email}")
        } catch (e: Exception) {
            logger.error("Error closing connection", e)
        }
    }
}

