package jugger.interfaces

import jugger.models.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable

@Repository
interface ChatRepository : JpaRepository<ChatMessage, Long> {
    // Метод с правильными типами параметров
    fun findByLocationIdAndTimestampBetween(
        locationId: Long,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<ChatMessage>

    fun findByLocationIdOrderByTimestampDesc(
        locationId: Long,
        pageable: Pageable
    ): List<ChatMessage>
}
