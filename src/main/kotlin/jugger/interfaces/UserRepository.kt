package jugger.interfaces

import jugger.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>

    // Методы для быстрой проверки существования
    fun existsByEmail(email: String): Boolean
}


