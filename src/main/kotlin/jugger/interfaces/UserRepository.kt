package jugger.interfaces

import jugger.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun findByEmail(email: String): Optional<User>

    // Методы для быстрой проверки существования
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
}


