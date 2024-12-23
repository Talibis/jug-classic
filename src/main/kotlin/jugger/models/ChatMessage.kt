package jugger.models

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "chat_messages")
data class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    val type: MessageType,

    @Column(name = "location_id")
    val locationId: Long,

    @Column(name = "sender_id")
    val senderId: Long,

    @Column(name = "content", length = 1000)
    val content: String,

    @Column(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

enum class MessageType {
    TEXT, SYSTEM, JOIN, LEAVE
}
