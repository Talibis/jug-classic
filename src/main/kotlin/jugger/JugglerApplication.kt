package jugger

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class JugglerApplication

fun main(args: Array<String>) {
    runApplication<JugglerApplication>(*args)
}
