package jugger.services

import jugger.interfaces.JwtTokenProvider
import jugger.models.CharacterCreateRequest
import jugger.models.Character
import jugger.interfaces.UserRepository
import jugger.interfaces.CharacterRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CharacterService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val characterRepository: CharacterRepository
) {
    private val logger = LoggerFactory.getLogger(CharacterService::class.java)

    @Transactional
    fun createCharacter(token: String, request: CharacterCreateRequest): Character? {
        try {
            // Извлечение username из токена
            val email = jwtTokenProvider.extractEmail(token.substringAfter("Bearer "))
            logger.info("Attempting to create character for user: $email")

            // Найти пользователя
            val user = userRepository.findByEmail(email)
                .orElseThrow { RuntimeException("User not found: $email") }

            // Проверить, что у пользователя еще нет персонажа
            if (characterRepository.existsByEmail(email)) {
                logger.warn("User already has a character: $email")
                return null
            }

            // Создание нового персонажа
            val newCharacter = Character(
                email = email,
                characterName = request.characterName,
                characterClass = request.characterClass,
                locationId = request.initialLocation,
                user = user, // Используем извлеченного пользователя
                level = 1,
                health = 100,
                mana = 50,
                experience = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                banned = false
            )

            // Обновить флаг наличия персонажа у пользователя
            user.haveCharacter = true
            userRepository.save(user)

            // Сохранение персонажа
            val savedCharacter = characterRepository.save(newCharacter)

            logger.info("Character created successfully for user: $email")
            return savedCharacter

        } catch (e: Exception) {
            logger.error("Error creating character", e)
            throw RuntimeException("Failed to create character: ${e.message}", e)
        }
    }

    fun findCharacterByEmail(email: String): Character? {
        return characterRepository.findByEmail(email)
    }
}
