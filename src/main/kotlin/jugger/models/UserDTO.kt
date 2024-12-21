package jugger.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.*

data class UserRegistrationDto @JsonCreator constructor(

    @field:NotBlank(message = "Email cannot be blank")
    @field:Email(message = "Invalid email format")
    @JsonProperty("email")
    val email: String,

    @field:NotBlank(message = "Password cannot be blank")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    @JsonProperty("password")
    val password: String
)

data class CharacterCreateRequest @JsonCreator constructor(
    @field:NotNull(message = "Character class is required")
    @JsonProperty("characterClass")
    val characterClass: CharacterClass,

    @JsonProperty("characterName")
    val characterName: String,

    @JsonProperty("initialLocation")
    val initialLocation: Long? = null
)

data class UserLoginDto @JsonCreator constructor(
    @JsonProperty("email")
    val email: String,

    @JsonProperty("password")
    val password: String
)

data class UserResponseDto(
    val id: Long,
    val email: String,
    val token: String? = null,
    val haveCharacter: Boolean
)
