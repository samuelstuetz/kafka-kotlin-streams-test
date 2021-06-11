package sam.example.accounts.controllers

import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.*
import sam.example.accounts.*
import java.time.Instant
import java.time.ZoneOffset

@RestController
class EventsController(val store: EventStore){

    @GetMapping(path = ["/accounts/{account_id}/events"])
    fun stream(@PathVariable account_id: Long): Flow<Event> = store.findAll(account_id)

    @PostMapping(path=["/accounts/{account_id}/events"])
    suspend fun publish(@PathVariable account_id: Long, @RequestBody createData: EventCreate): AccountScopedEventDto {
        return store.create(account_id, createData).asDto()
    }

    @GetMapping(path = ["/accounts/{account_id}/statistics"])
    suspend fun summary(@PathVariable account_id:Long,
    ): List<EventStatisticDto> {
        // no idea how to get that would have to serialize or aggregate differently
        // this should be improved obviously.
        val magicallyRecreateTheWindow = 0L
        return store.statistics(account_id)?.types?.map {
            EventStatisticDto(
                Instant.now().atZone(ZoneOffset.UTC).toLocalDate().minusDays(magicallyRecreateTheWindow),
                it.key,
                it.value
            )
        } ?: throw NoSuchKey(account_id)
    }

}