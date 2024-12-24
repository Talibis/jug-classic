package jugger.services

import jugger.interfaces.ChatRepository
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import com.fasterxml.jackson.databind.ObjectMapper
import jugger.interfaces.UserRepository
import jugger.models.ChatMessage
import jugger.models.MessageType
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

@Service
@Transactional
class ChatService(
    private val chatRepository: ChatRepository,
    private val userService: UserService,
    private val characterService: CharacterService, // Замените CharacterRepository на CharacterService
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository
) {
    private val locationSessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()
    private val logger = LoggerFactory.getLogger(ChatService::class.java)

    fun handleNewConnection(session: WebSocketSession, token: String) {
        try {
            val email = userService.extractEmailFromToken(token)

            // Используем CharacterService для поиска персонажа
            val character = characterService.findCharacterByEmail(email)
                ?: throw IllegalStateException("No character found for user")

            // Безопасное добавление сессии в locationSessions
            val locationId = character.locationId
                ?: throw IllegalStateException("Character has no location")

            locationSessions.computeIfAbsent(locationId) { ConcurrentHashMap.newKeySet() }
                .add(session)

            // Отправка последних сообщений при подключении
            val lastMessages = chatRepository.findByLocationIdOrderByTimestampDesc(
                locationId,
                PageRequest.of(0, 50)
            )
            sendHistoryToSession(session, lastMessages)

        } catch (e: Exception) {
            logger.error("Error handling new connection", e)
            session.close()
        }
    }

    fun processMessage(token: String, messageData: Map<String, String>): ChatMessage {
        return try {
            val email = userService.extractEmailFromToken(token)
            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalStateException("User not found") }

            val character = characterService.findCharacterByEmail(email)
                ?: throw IllegalStateException("No character found for user")

            // Используем ID пользователя из найденного пользователя
            val message = ChatMessage(
                type = MessageType.valueOf(
                    messageData["type"]
                        ?: throw IllegalArgumentException("Invalid message type")
                ),
                locationId = character.locationId
                    ?: throw IllegalStateException("Character has no location"),
                senderId = user, // Передаем полный объект User
                content = messageData["content"]
                    ?: throw IllegalArgumentException("Message content is required")
            )

            logger.info("Attempting to save message: $message")

            val savedMessage = chatRepository.save(message)

            logger.info("Message saved successfully: ${savedMessage.id}")

            broadcastToLocation(savedMessage)
            savedMessage

        } catch (e: Exception) {
            logger.error("Detailed error processing message", e)
            throw RuntimeException("Failed to process message", e)
        }
    }


    private fun broadcastToLocation(message: ChatMessage) {
        locationSessions[message.locationId]?.forEach { session ->
            try {
                session.sendMessage(convertMessageToJson(message))
            } catch (e: Exception) {
                logger.error("Error broadcasting message", e)
            }
        }
    }

    fun removeSession(session: WebSocketSession) {
        locationSessions.values.forEach { sessions ->
            sessions.remove(session)
        }
    }

    private fun convertMessageToJson(message: ChatMessage): TextMessage {
        val jsonMessage = objectMapper.writeValueAsString(message)
        return TextMessage(jsonMessage)
    }

    private fun sendHistoryToSession(session: WebSocketSession, messages: List<ChatMessage>) {
        messages.forEach { message ->
            try {
                val jsonMessage = objectMapper.writeValueAsString(message)
                session.sendMessage(TextMessage(jsonMessage))
            } catch (e: Exception) {
                logger.error("Error sending history message", e)
            }
        }
    }
}
