package jugger.handlers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jugger.models.MessageType
import jugger.services.ChatService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class WebSocketHandler(
    private val chatService: ChatService,
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        try {
            val token = extractToken(session)
            chatService.handleNewConnection(session, token)
            sessions[session.id] = session
            logger.info("New WebSocket connection: ${session.id}")
        } catch (e: Exception) {
            logger.error("Connection error", e)
            session.close()
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val token = extractToken(session)

            // Всегда преобразуем сообщение в JSON
            val messageData = try {
                // Попытка распарсить как JSON
                objectMapper.readValue(
                    message.payload,
                    object : TypeReference<Map<String, Any>>() {}
                ).mapValues { it.value.toString() }
            } catch (e: Exception) {
                // Если не JSON - принудительно создаем JSON
                mapOf(
                    "content" to message.payload,
                    "type" to "TEXT",
                    "timestamp" to System.currentTimeMillis().toString(),
                    "originalPayload" to message.payload
                )
            }

            // Гарантированно имеем валидный JSON
            val processableMessage = when {
                messageData.isEmpty() -> mapOf(
                    "content" to message.payload,
                    "type" to "TEXT",
                    "timestamp" to System.currentTimeMillis().toString()
                )
                !messageData.containsKey("content") -> messageData + ("content" to message.payload)
                !messageData.containsKey("type") -> messageData + ("type" to "TEXT")
                !messageData.containsKey("timestamp") -> messageData + ("timestamp" to System.currentTimeMillis().toString())
                else -> messageData
            }

            chatService.processMessage(token, processableMessage)
        } catch (e: Exception) {
            logger.error("Message processing error: ${e.message}", e)
            sendErrorToClient(session, "Failed to process message: ${e.message}")
        }
    }

    // Дополнительный метод для отправки ошибки клиенту
    private fun sendErrorToClient(session: WebSocketSession, errorMessage: String) {
        if (session.isOpen) {
            try {
                val errorJson = objectMapper.writeValueAsString(
                    mapOf(
                        "type" to "ERROR",
                        "message" to errorMessage
                    )
                )
                session.sendMessage(TextMessage(errorJson))
            } catch (e: Exception) {
                logger.error("Failed to send error to client", e)
            }
        }
    }

    // Можно добавить метод очистки и нормализации входящего сообщения
    private fun normalizeMessage(payload: String): Map<String, String> {
        return mapOf(
            "content" to payload.trim(),
            "type" to MessageType.TEXT.name,
            "timestamp" to System.currentTimeMillis().toString()
        )
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        if (session.isOpen) {
            session.close()
        }
        chatService.removeSession(session)
        sessions.remove(session.id)
        logger.info("WebSocket connection closed: ${session.id}, status: $status")
    }

    private fun extractToken(session: WebSocketSession): String {
        // Извлечение токена из параметров запроса
        val token = session.uri?.query
            ?.split("&")
            ?.find { it.startsWith("token=") }
            ?.removePrefix("token=")
            ?: throw IllegalArgumentException("No token provided")

        return token
    }
}
