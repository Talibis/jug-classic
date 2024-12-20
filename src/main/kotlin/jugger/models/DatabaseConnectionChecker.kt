package jugger.models

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.SQLException
import javax.sql.DataSource


@Component
class DatabaseConnectionChecker {
    @Autowired
    private val dataSource: DataSource? = null

    @PostConstruct
    fun checkDatabaseConnection() {
        try {
            dataSource?.getConnection().use { connection ->
                println("Успешное подключение к базе данных!")
            }
        } catch (e: SQLException) {
            System.err.println("Ошибка подключения к базе данных: " + e.message)
        }
    }
}