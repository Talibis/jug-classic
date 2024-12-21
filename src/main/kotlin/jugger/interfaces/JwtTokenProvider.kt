package jugger.interfaces

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jugger.models.User
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
}
