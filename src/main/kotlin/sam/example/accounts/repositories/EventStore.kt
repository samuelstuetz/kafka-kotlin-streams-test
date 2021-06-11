package sam.example.accounts

import kotlinx.coroutines.flow.Flow
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyWindowStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import sam.example.accounts.configs.KafkaEventConfig
import sam.example.accounts.repositories.StreamCtx
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Event repository the trick is to make this testable as it is the only part
 * that needs to be validated to see if this implementation does what it should
 */
interface EventStore {
    fun findAll(id: Long): Flow<Event>
    suspend fun create(id: Long, command: EventCreate): Event
    suspend fun statistics(id: Long): Aggregate?
}

@Component
class EventStoreImpl(
    val kafkaTemplate: CoProducer<Long, String>,
    streamsFactory: StreamsBuilderFactoryBean? = null,
    val config: KafkaEventConfig,
    @Autowired val kafkaProperties: KafkaProperties,
) : EventStore, Logging, StreamCtx(streamsFactory) {

    private val dailyStore: ReadOnlyWindowStore<Long, String> by lazy {
        streams.store(
            StoreQueryParameters.fromNameAndType(
                NamedStores.daily_stats_windowed.name,
                QueryableStoreTypes.windowStore<Long, String>()
            )
        )
    }

    override fun findAll(id:Long): Flow<Event> =
        ConsumerForKey(kafkaProperties, config.eventTopicName, id, config.eventTopicPartitions)
            .consume()

    override suspend fun create(id: Long, command: EventCreate): Event {
        val result = kafkaTemplate.publish(
            config.eventTopicName,
            id,
            Event(Instant.now(), id, command.type).asJson()
        )
        return result.asEvent()
    }

    override suspend fun statistics(id: Long): Aggregate? =
        dailyStore.backwardFetch(
            id,
            LocalDate.now().minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant(),
            LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        )
            .asSequence().firstOrNull()?.value?.aggregate()


}