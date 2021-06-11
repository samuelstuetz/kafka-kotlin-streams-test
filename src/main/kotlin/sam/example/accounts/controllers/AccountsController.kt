package sam.example.accounts.controllers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import sam.example.accounts.*
import sam.example.accounts.repositories.AccountStore

@RestController
class AccountsController(val store: AccountStore): Logging {

    @GetMapping(path = ["/accounts"])
    fun findAll(): Flow<AccountDto> {
        return store.findAll().map { it.asDto() }
    }

    @GetMapping(path = ["/accounts/{id}"])
    suspend fun findOne(@PathVariable id: Long): AccountDto? {
        return store.findOne(id)?.asDto()
    }

    @PostMapping(path = ["/accounts"])
    suspend fun createOne(@RequestBody createCommand: AccountCreateCommand): AccountDto {
        return store.create(createCommand).asDto()
    }

    @PatchMapping(path = ["/accounts/{id}"])
    suspend fun update(@PathVariable id: Long, @RequestBody updateCommand: AccountUpdateCommand): AccountDto {
        return store.update(id, updateCommand).asDto()
    }

    @DeleteMapping(path = ["/accounts/{id}"])
    suspend fun delete(@PathVariable id: Long) {
        return store.delete(id)
        //also tombstone the events topic probably
    }

    @ResponseStatus(
        value = HttpStatus.NOT_FOUND,
        reason = "no luck try again"
    )
    @ExceptionHandler(NoSuchKey::class)
    suspend fun respondSuchKey(exception: NoSuchKey) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body("no data for account with id <${exception.id}>")
}
