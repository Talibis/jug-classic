package jugger.models

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "auth", url = "\${AUTH_SERVICE_URL:http://localhost:8080}", fallback = AuthClientFallback::class)
interface AuthClient {
    @PostMapping("/api/auth/register")
    fun registerUser(@RequestBody registrationDto: UserRegistrationDto): ResponseEntity<RegisterResponse>

    @PostMapping("/api/auth/login")
    fun loginUser(@RequestBody loginDto: UserLoginDto): ResponseEntity<LoginResponse>
}

class AuthClientFallback : AuthClient {
    override fun registerUser(registrationDto: UserRegistrationDto): ResponseEntity<RegisterResponse> {
        return ResponseEntity.status(503).body(
            RegisterResponse(
                message = "Registration service unavailable",
                userId = null
            )
        )
    }

    override fun loginUser(loginDto: UserLoginDto): ResponseEntity<LoginResponse> {
        return ResponseEntity.status(503).body(
            LoginResponse(
                message = "Authentication service unavailable",
                token = null,
                userId = null
            )
        )
    }
}

data class LoginResponse(
    val message: String,
    val token: String?,
    val userId: Long?
)
