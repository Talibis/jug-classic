package jugger.interfaces

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jugger.models.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration}") private val jwtExpiration: Long
) {
    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    fun generateToken(user: User): String {
        val claims = Jwts.claims().setSubject(user.email)
        claims["userId"] = user.id
        claims["email"] = user.email

        val now = Date()
        val validity = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token)
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun extractEmail(token: String): String {
        return try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .body
                .subject
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid token")
        }
    }

    // Новый метод для получения Authentication
    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .body

        val username = claims.subject

        // Создаем список authorities (здесь простой пример, можно расширить)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

        // Создаем объект UserDetails или используйте вашу собственную реализацию
        return UsernamePasswordAuthenticationToken(
            username,
            null,
            authorities
        )
    }

    fun validateAndGetAuthentication(token: String): Authentication {
        try {
            // Проверяем токен
            if (!validateToken(token)) {
                throw IllegalArgumentException("Invalid token")
            }

            // Извлекаем claims
            val claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .body

            // БЕЗОПАСНОЕ извлечение userId
            val userId = when (val id = claims["userId"]) {
                is Long -> id
                is Int -> id.toLong()
                is String -> id.toLongOrNull()
                else -> null
            } ?: throw IllegalArgumentException("User ID not found or invalid in token")

            val email = claims.subject
                ?: throw IllegalArgumentException("Email not found in token")

            // Создаем список authorities
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

            // Создаем Authentication объект
            return UsernamePasswordAuthenticationToken(
                email, // Используем email как principal
                userId, // Передаем userId как credentials
                authorities
            )
        } catch (ex: Exception) {
            logger.error("Token validation error", ex)
            throw IllegalArgumentException("Invalid token: ${ex.message}")
        }
    }}
