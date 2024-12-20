package jugger.models

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<String>
)
