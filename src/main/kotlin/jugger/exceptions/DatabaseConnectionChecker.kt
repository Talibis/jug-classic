package jugger.exceptions

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.sql.SQLException
import javax.sql.DataSource

@Component
class DatabaseConnectionChecker {
    @Autowired
    private lateinit var dataSource: DataSource

    @PostConstruct
    fun checkDatabaseConnection() {
        try {
            dataSource.connection.use { _ ->
                println("Успешное подключение к базе данных!")
            }
        } catch (e: SQLException) {
            System.err.println("Ошибка подключения к базе данных: ${e.message}")
        }
    }
}
