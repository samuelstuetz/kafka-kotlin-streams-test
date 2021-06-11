package sam.example.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.streams.KeyValue
import java.time.Instant
import java.time.LocalDate

/**
 * enums to keep track of what is used and what exists.
 * Can/Should be externalised later into yaml but at the start is easier
 * in conceptual phase makes it much easier to code if the IDE can track it
 */

enum class Topics { accounts, events }
enum class NamedStores { account_store, daily_stats_windowed}

/**
 * models accounts
 * create/update as classes rather than string parameters which don't document well
 * Dto is what is returned to the client
 * Account is the domain object in Kafka
 */
data class AccountCreateCommand(val name:String)
data class AccountUpdateCommand(val name:String)
data class AccountDto(val id: Long, val name: String)
// I find extension fn like this much more versatile and better
// than the alternative of harming readability by adding this function to the class itself
// I would normally even put them into a different file
fun Account.asDto(): AccountDto = AccountDto(this.id, this.name)
data class Account(
    //this is redundant as it is the key too
    //but is more extendable and no copy around needed
    val id: Long,
    val name: String,
)


/**
 * models events
 * create/update as classes rather than string parameters which don't document well
 * Dto is what is returned to the client
 * Account is the domain object in Kafka
 */
data class EventCreate(val type:String)
data class AccountScopedEventDto(val happenedAt: Instant, val type: String)
fun Event.asDto(): AccountScopedEventDto = AccountScopedEventDto(this.happenedAt, this.type)
data class Event(
    val happenedAt: Instant,
    val accountId: Long,
    val type: String,
)

/**
 * special statistics view
 */
data class EventStatisticDto(
    val day: LocalDate,
    val type: String,
    var count: Long,
)

/**
 * spring json kafka is just sad
 * it seems to break for streams or basic kafka but not both at the same time
 *
 * in reality avro should be used or protobuf with schema registry
 * json is only okay for an experiment or a prototype
 * so I hacked this poor-mans string serialization and just told kafka to just use Strings.
 *  was easier than adding avro and works but is obviously not something to copy.
 * extension functions are great for hiding away ugly weird hacks like this.
 */
val mapper:ObjectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
fun String.asAccount():Account = mapper.readValue(this)
fun String.asEvent():Event = mapper.readValue(this)
fun String.asEventStatistic():EventStatisticDto = mapper.readValue(this)
fun Account.asJson():String = mapper.writeValueAsString(this)
fun Event.asJson():String = mapper.writeValueAsString(this)
fun EventStatisticDto.asJson():String = mapper.writeValueAsString(this)
data class Aggregate(val types: MutableMap<String, Long> = mutableMapOf()) {
    fun add(type: String): Aggregate {
        types[type] = (types[type] ?: 0) + 1
        return this
    }
    fun asJson(): String = mapper.writeValueAsString(this)
}
fun String.aggregate(): Aggregate = mapper.readValue(this)