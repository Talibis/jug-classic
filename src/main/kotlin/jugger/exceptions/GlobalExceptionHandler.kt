package jugger.exceptions

import jugger.models.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.sql.SQLException
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

data class ValidationErrorResponse(
    val violations: List<Violation>
)

data class Violation(
    val fieldName: String,
    val message: String
)

@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Обработчик для Bean Validation (@Valid) ошибок
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val violations = ex.bindingResult.fieldErrors.map { error ->
            val detailedMessage = when (error.field) {
                "username" -> when {
                    error.defaultMessage?.contains("size", ignoreCase = true) == true ->
                        "Username must be between 3 and 50 characters long"
                    error.defaultMessage?.contains("blank", ignoreCase = true) == true ->
                        "Username cannot be empty"
                    else -> error.defaultMessage ?: "Invalid username"
                }
                "email" -> when {
                    error.defaultMessage?.contains("email", ignoreCase = true) == true ->
                        "Please provide a valid email address"
                    error.defaultMessage?.contains("blank", ignoreCase = true) == true ->
                        "Email cannot be empty"
                    else -> error.defaultMessage ?: "Invalid email"
                }
                "password" -> when {
                    error.defaultMessage?.contains("size", ignoreCase = true) == true ->
                        "Password must be at least 8 characters long"
                    error.defaultMessage?.contains("blank", ignoreCase = true) == true ->
                        "Password cannot be empty"
                    else -> error.defaultMessage ?: "Invalid password"
                }
                else -> error.defaultMessage ?: "Invalid value"
            }

            Violation(
                fieldName = error.field,
                message = detailedMessage
            )
        }

        logger.warn("Validation errors occurred: $violations")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ValidationErrorResponse(violations))
    }

    // Обработчик для ConstraintViolation исключений
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ValidationErrorResponse> {
        val violations = ex.constraintViolations.map { violation ->
            val detailedMessage = when {
                violation.propertyPath.toString().contains("username") -> when {
                    violation.message.contains("size") -> "Username must be between 3 and 50 characters long"
                    violation.message.contains("blank") -> "Username cannot be empty"
                    else -> violation.message
                }
                violation.propertyPath.toString().contains("email") -> when {
                    violation.message.contains("email") -> "Please provide a valid email address"
                    violation.message.contains("blank") -> "Email cannot be empty"
                    else -> violation.message
                }
                violation.propertyPath.toString().contains("password") -> when {
                    violation.message.contains("size") -> "Password must be at least 8 characters long"
                    violation.message.contains("blank") -> "Password cannot be empty"
                    else -> violation.message
                }
                else -> violation.message
            }

            Violation(
                fieldName = violation.propertyPath.toString(),
                message = detailedMessage
            )
        }

        logger.warn("Constraint violations: $violations")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ValidationErrorResponse(violations))
    }

    // Обработчик для общих ValidationException
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException): ResponseEntity<ErrorResponse> {
        logger.warn("Validation exception: ${ex.message}")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    message = "Validation error",
                    errors = listOf(ex.message ?: "Validation failed")
                )
            )
    }

    // Обработчик для IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${ex.message}")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    message = "Invalid argument",
                    errors = listOf(ex.message ?: "Invalid argument provided")
                )
            )
    }

    // Общий обработчик неожиданных исключений
    @ExceptionHandler(Exception::class)
    fun handleGeneralExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = "An unexpected error occurred",
                    errors = listOf(ex.message ?: "Unknown error")
                )
            )
    }

    // Обработчик для HttpMessageNotReadableException
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.error("JSON parsing error", ex)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    message = "Invalid request format",
                    errors = listOf(ex.message ?: "Could not parse JSON")
                )
            )
    }

    // Обработчик для DataIntegrityViolationException и SQLException
    @ExceptionHandler(DataIntegrityViolationException::class, SQLException::class)
    fun handleDataIntegrityViolationException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Data integrity violation", ex)

        val errorMessage = when {
            ex.message?.contains("duplicate key value", ignoreCase = true) == true -> {
                when {
                    ex.message?.contains("email", ignoreCase = true) == true -> "Email уже используется"
                    ex.message?.contains("username", ignoreCase = true) == true -> "Никнейм уже занят"
                    else -> "Пользователь с такими данными уже существует"
                }
            }
            else -> "Ошибка при регистрации"
        }

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    status = HttpStatus.CONFLICT.value(),
                    message = "Ошибка регистрации",
                    errors = listOf(errorMessage)
                )
            )
    }
}
