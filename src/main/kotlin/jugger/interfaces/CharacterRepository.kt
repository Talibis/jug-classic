package jugger.interfaces

import jugger.models.Character
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CharacterRepository : JpaRepository<Character, Long> {
    fun findByUsername(username: String): Character?
    fun existsByUsername(username: String): Boolean
}
