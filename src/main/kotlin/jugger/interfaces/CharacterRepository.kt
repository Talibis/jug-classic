package jugger.interfaces

import jugger.models.Character
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CharacterRepository : JpaRepository<Character, Long> {
    fun findByEmail(email: String): Character?
    fun existsByEmail(email: String): Boolean
}
