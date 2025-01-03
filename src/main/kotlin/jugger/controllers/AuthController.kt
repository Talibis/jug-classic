package jugger.controllers

import jugger.models.*
import javax.validation.Valid
import jugger.services.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
) {
    private val logger: Logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/register")
    fun registerUser(
        @Valid @RequestBody registrationDto: UserRegistrationDto
    ): ResponseEntity<UserRegistrationResponseDto> {
        logger.info("Attempting to register user: ${registrationDto.email}")
        val userResponse = userService.registerNewUser(registrationDto)

        return ResponseEntity.ok().body(userResponse)
    }

    @PostMapping("/login")
    fun loginUser(
        @Valid @RequestBody loginDto: UserLoginDto
    ): ResponseEntity<UserAuthResponseDto> {
        logger.info("Attempting to login user: ${loginDto.email}")
        val userResponse = userService.authenticateUser(loginDto)
        return ResponseEntity.ok().body(userResponse)
    }
}
