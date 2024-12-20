package jugger.services

import jugger.interfaces.JwtTokenProvider
import jugger.interfaces.UserRepository
import jugger.models.*
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.validation.ValidationException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,  // Добавлен JwtTokenProvider
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)


    @Transactional
    fun registerNewUser(dto: UserRegistrationDto): UserResponseDto {
        validateUsername(dto.username)
        validateEmail(dto.email)
        checkUserUniqueness(dto)

        val user = createNewUser(dto)
        val savedUser = userRepository.save(user)

        // Генерация токена при регистрации
        val token = jwtTokenProvider.generateToken(savedUser)

        logger.info("User registered successfully: username={}", dto.username)

        return mapToUserResponseDto(savedUser, token)
    }

    private fun validateUsername(username: String) {
        val trimmedUsername = username.trim()

        when {
            trimmedUsername.length < 3 -> {
                logger.warn("Username validation failed: too short ({})", trimmedUsername.length)
                throw ValidationException("Username must be at least 3 characters long")
            }

            trimmedUsername.length > 20 -> {
                logger.warn("Username validation failed: too long ({})", trimmedUsername.length)
                throw ValidationException("Username must not exceed 20 characters")
            }

            !trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                logger.warn("Username validation failed: invalid characters")
                throw ValidationException("Username can only contain letters, numbers, and underscores")
            }
        }
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

        if (userRepository.existsByUsername(dto.username)) {
            logger.warn("Username already exists: {}", dto.username)
            errors.add("Username already exists")
        }
        if (userRepository.existsByEmail(dto.email)) {
            logger.warn("Email already exists: {}", dto.email)
            errors.add("Email already exists")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors.joinToString("; "))
        }
    }

    private fun createNewUser(dto: UserRegistrationDto): User {
        logger.debug("Creating new user: username={}", dto.username)
        return User(
            username = dto.username.trim(),
            email = dto.email.lowercase().trim(),
            password = passwordEncoder.encode(dto.password)
        )
    }

    private fun mapToUserResponseDto(user: User, token: String? = null): UserResponseDto {
        logger.debug("Mapping user to response DTO: username={}", user.username)
        return UserResponseDto(
            id = user.id ?: throw IllegalStateException("User ID cannot be null"),
            username = user.username,
            email = user.email,
            token = token,
            haveCharacter = user.haveCharacter
        )
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$""".toRegex()
        return email.matches(emailRegex)
    }

    fun authenticateUser(dto: UserLoginDto): UserResponseDto {
        // Используйте .orElseThrow() или .get() для Optional
        val user = userRepository.findByUsername(dto.username)
            .orElseThrow { UserNotFoundException("User not found") }

        // Проверка пароля
        if (!passwordEncoder.matches(dto.password, user.password)) {
            logger.warn("Invalid password for user: ${dto.username}")
            throw ValidationException("Invalid credentials")
        }

        // Генерация JWT токена при аутентификации
        val token = jwtTokenProvider.generateToken(user)

        logger.info("User authenticated successfully: ${dto.username}")
        return mapToUserResponseDto(user, token)
    }

    // Исключения остаются прежними
    class UserNotFoundException(message: String) : RuntimeException(message)
    class UserAlreadyExistsException(message: String) : RuntimeException(message)
}
