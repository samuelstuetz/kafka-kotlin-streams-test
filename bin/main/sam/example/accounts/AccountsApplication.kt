package sam.example.accounts

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AccountsApplication

fun main(args: Array<String>) {
	runApplication<AccountsApplication>(*args)
}
