package jugger

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableFeignClients
@EntityScan("jugger.models")  // Указывает пакет с JPA сущностями
@EnableJpaRepositories("jugger.interfaces")  // Указывает пакет с репозиториями
class JugglerApplication

fun main(args: Array<String>) {
    runApplication<JugglerApplication>(*args)
}
