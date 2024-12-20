package jugger.controllers

import jugger.interfaces.JwtTokenProvider
import jugger.interfaces.UserRepository
import jugger.interfaces.CharacterRepository
import jugger.models.CharacterCreateRequest
import jugger.services.UserService
import jugger.services.CharacterService
import jugger.models.ErrorResponse

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/character")
class CharacterController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val characterService: CharacterService
) {
    private val logger: Logger = LoggerFactory.getLogger(CharacterController::class.java)

    @GetMapping("/check-character")
    fun checkUserCharacter(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<*> {
        // Извлечь username из токена
        val username = jwtTokenProvider.extractUsername(token.replace("Bearer ", ""))

        // Найти пользователя
        val user = userRepository.findByUsername(username)
            .orElseThrow { UserService.UserNotFoundException("User not found") }

        // Найти персонажа
        val character = characterService.findCharacterByUsername(username)
            ?: return ResponseEntity.ok(mapOf("hasCharacter" to false))

        // Создаем DTO для возврата всех данных кроме времени создания и обновления
        val characterResponse = mapOf(
            "id" to character.id,
            "username" to character.username,
            "characterClass" to character.characterClass,
            "level" to character.level,
            "health" to character.health,
            "mana" to character.mana,
            "experience" to character.experience,
            "locationId" to character.locationId,
            "banned" to character.banned
        )

        return ResponseEntity.ok(characterResponse)
    }


    @PostMapping("/create")
    fun createCharacter(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: CharacterCreateRequest
    ): ResponseEntity<*> {
        logger.info("Character creation request: $request")

        return try {
            val character = characterService.createCharacter(token, request)
            when {
                character != null -> ResponseEntity.ok(character)
                else -> ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                        ErrorResponse(
                            status = HttpStatus.CONFLICT.value(), // Используем числовое значение статус-кода
                            message = "Character creation failed",
                            errors = listOf("User already has a character")
                        )
                    )
            }
        } catch (e: Exception) {
            logger.error("Character creation error", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    ErrorResponse(
                        status = HttpStatus.INTERNAL_SERVER_ERROR.value(), // Числовое значение статус-кода
                        message = "Internal error",
                        errors = listOf(e.message ?: "Unknown error")
                    )
                )
        }
    }
}