package jugger.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import java.time.LocalDateTime

enum class CharacterClass(private val value: String) {
    PERUN("PERUN"),
    SWAROG("SWAROG"),
    STRIBOG("STRIBOG"),
    VELES("VELES");

    @JsonValue
    override fun toString(): String = value

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): CharacterClass {
            return values().first { it.value.equals(value, ignoreCase = true) }
        }
    }
}

@Entity
@Table(name = "characters")
data class Character(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true)
    val username: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "character_class")
    val characterClass: CharacterClass, // Новое поле класса персонажа

    @Column
    var level: Int = 1, // Новое поле уровня, по умолчанию 1

    @Column
    var health: Int = 100,

    @Column
    var mana: Int = 50,

    @Column
    var experience: Long = 0,

    @Column(name = "location_id")
    var locationId: Long? = null,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var banned: Boolean = false,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User
)
