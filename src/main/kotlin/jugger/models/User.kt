package jugger.models

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    val email: String,

    @Column(nullable = false)
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    var password: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "have_character")
    var haveCharacter: Boolean = false
)
