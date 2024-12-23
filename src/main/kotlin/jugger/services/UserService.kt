package jugger.services

import jugger.interfaces.JwtTokenProvider
import jugger.interfaces.UserRepository
import jugger.models.*
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.socket.WebSocketSession
import javax.validation.ValidationException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,  // Добавлен JwtTokenProvider
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)


    @Transactional
    fun registerNewUser(dto: UserRegistrationDto): UserRegistrationResponseDto {
        // Преобразуем email в lowercase перед валидацией и дальнейшей обработкой
        val lowercaseEmail = dto.email.lowercase()
        val dtoWithLowercaseEmail = dto.copy(email = lowercaseEmail)

        validateEmail(dtoWithLowercaseEmail.email)
        checkUserUniqueness(dtoWithLowercaseEmail)

        val user = createNewUser(dtoWithLowercaseEmail)
        val savedUser = userRepository.save(user)

        logger.info("User registered successfully: email={}", dtoWithLowercaseEmail.email)

        return mapToUserRegistrationResponseDto(savedUser)
    }

    private fun validateEmail(email: String) {
        val trimmedEmail = email.trim().lowercase()

        when {
            !isValidEmail(trimmedEmail) -> {
                logger.warn("Email validation failed: invalid format")
                throw ValidationException("Invalid email format")
            }
        }
    }

    private fun checkUserUniqueness(dto: UserRegistrationDto) {
        val errors = mutableListOf<String>()

        if (userRepository.existsByEmail(dto.email)) {
            logger.warn("Email already exists: {}", dto.email)
            errors.add("Email already exists")
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors.joinToString("; "))
        }
    }

    private fun createNewUser(dto: UserRegistrationDto): User {
        logger.debug("Creating new user: email={}", dto.email)
        return User(
            email = dto.email.lowercase().trim(),
            password = passwordEncoder.encode(dto.password)
        )
    }

    private fun mapToUserAuthResponseDto(user: User, token: String? = null): UserAuthResponseDto {
        logger.debug("Mapping user to response DTO: email={}", user.email)
        return UserAuthResponseDto(
            id = user.id ?: throw IllegalStateException("User ID cannot be null"),
            email = user.email,
            token = token,
            haveCharacter = user.haveCharacter
        )
    }

    private fun mapToUserRegistrationResponseDto(user: User): UserRegistrationResponseDto {
        logger.debug("Mapping user to registration response DTO: email={}", user.email)
        return UserRegistrationResponseDto(
            id = user.id ?: throw IllegalStateException("User ID cannot be null"),
            email = user.email,
            haveCharacter = user.haveCharacter
        )
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$""".toRegex()
        return email.matches(emailRegex)
    }

    fun extractTokenFromSession(session: WebSocketSession): String {
        // Вариант 1: Извлечение из URL-параметров
        val uri = session.uri ?: throw IllegalArgumentException("No URI in session")
        return uri.query?.split("&")
            ?.find { it.startsWith("token=") }
            ?.substringAfter("token=")
            ?: throw IllegalArgumentException("Token not found in session")
    }

    fun authenticateUser(dto: UserLoginDto): UserAuthResponseDto {
        // Преобразование email в lowercase перед поиском
        val lowercaseEmail = dto.email.lowercase()

        // Используйте .orElseThrow() или .get() для Optional
        val user = userRepository.findByEmail(lowercaseEmail)
            .orElseThrow { UserNotFoundException("User not found") }

        // Проверка пароля
        if (!passwordEncoder.matches(dto.password, user.password)) {
            logger.warn("Invalid password for user: $lowercaseEmail")
            throw ValidationException("Invalid credentials")
        }

        // Генерация JWT токена при аутентификации
        val token = jwtTokenProvider.generateToken(user)

        logger.info("User authenticated successfully: $lowercaseEmail")
        return mapToUserAuthResponseDto(user, token)
    }

    fun getUserFromToken(token: String): User {
        // Расширенная валидация токена
        require(token.isNotBlank()) { "Token cannot be blank" }

        if (!jwtTokenProvider.validateToken(token)) {
            logger.warn("Invalid token attempted: {}", token)
            throw IllegalArgumentException("Invalid or expired token")
        }

        val email = jwtTokenProvider.extractEmail(token)
            ?: throw IllegalArgumentException("Cannot extract email from token")

        return userRepository.findByEmail(email.lowercase())
            .orElseThrow {
                logger.error("User not found for email: {}", email)
                UserNotFoundException("User not found for email: $email")
            }
    }

    fun getUserFromSession(session: WebSocketSession): User {
        return try {
            val token = extractTokenFromSession(session)
            getUserFromToken(token)
        } catch (e: Exception) {
            logger.error("Error getting user from session", e)
            throw e
        }
    }

    fun extractEmailFromToken(token: String): String {
        return jwtTokenProvider.extractEmail(token.substringAfter("Bearer "))
    }

    // Исключения остаются прежними
    class UserNotFoundException(message: String) : RuntimeException(message)
}
