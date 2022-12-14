package sam.example.accounts.repositories

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.TestInputTopic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import sam.example.accounts.*
import sam.example.accounts.configs.KafkaStreamsTopology
import org.apache.kafka.streams.state.ReadOnlyWindowStore
import org.junit.jupiter.api.Disabled
import sam.example.accounts.configs.KafkaEventConfig
import java.time.Duration
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventStoreImplTest {
    /**
     * Embedded Kafka does not work to test this.
     * it does not wait for the stream to start and connects to the actual kafka.
     */
    //these should be possible to put random names need
    private val topicName = Topics.events.toString()
    private val storeName = NamedStores.daily_stats_windowed.toString()

    private val streamConfig = KafkaStreamsTopology()
    private val kafka = MockKafkaStreams {
        streamConfig.accountTableEventSourced(this)
        streamConfig.dailyStatistics(this)
    }
    private val inputTopic: TestInputTopic<Long, String> = kafka.driver
        .createInputTopic(
            topicName,
            Serdes.Long().serializer(),
            Serdes.String().serializer()
        )
    val store = setup(topicName, storeName, inputTopic)

    @AfterAll
    fun tearDown() {
        kafka.close()
    }

    @Test
    @Disabled("this cannot be test in this setup, will hang until timeout")
    fun `list should only return data from single account`() {
        runBlocking {
            val accOne = 1L
            val accTwo = 2L
            (1..2).map { "Event2" }.forEach { store.create(accTwo, EventCreate(it)) }
            (1..1).map { "Event1" }.forEach { store.create(accOne, EventCreate(it)) }
            (1..3).map { "Event3" }.forEach { store.create(accTwo, EventCreate(it)) }
            val flow = store.findAll(accOne)
            kafka.driver.advanceWallClockTime(Duration.ofSeconds(10))
            val list = flow.toList()
            assertThat(list).isNotEmpty
            assertThat(list.map { it.accountId }).containsOnly(accOne)
        }
    }

    @Test
    fun `insert and check statistics`() {
        runBlocking {
            val accId = 1L
            (1..1).map { "Event1" }.forEach { store.create(accId, EventCreate(it)) }
            (1..2).map { "Event2" }.forEach { store.create(accId, EventCreate(it)) }
            (1..3).map { "Event3" }.forEach { store.create(accId, EventCreate(it)) }
            val stats = store.statistics(accId)?.types
            assertThat(stats).`as`("should exist").isNotNull
            assertThat(stats!!).containsAllEntriesOf(mapOf(
                "Event1" to 1,
                "Event2" to 2,
                "Event3" to 3
            ))
        }
    }

    @Test
    fun `empty stats should not fail`() {
        runBlocking {
            val accId = Random().nextLong()
            val stats = store.statistics(accId)?.types
            assertThat(stats).`as`("should not exist").isNull()
        }
    }

    private fun setup(
        topicName: String,
        storeName: String,
        inputTopic: TestInputTopic<Long, String>
    ): EventStoreImpl {
        //too much mocking :(
        val mockProducer = replacementProducer(topicName, inputTopic)
        val embeddedKafka = embeddedKafka(storeName)
        val embeddedKafkaSupplier = embeddedKafkaSupplier(embeddedKafka)
        return EventStoreImpl(mockProducer, embeddedKafkaSupplier, object : KafkaEventConfig {
            override val eventTopicName: String
                get() = topicName
            override val eventTopicPartitions: Int
                get() = 1
        }, KafkaProperties())
    }

    /**
     * just republishes unchanged what normally goes to eh CoProducer to the TestInputTopic instead
     * essentially replaces the backend of it.
     */
    private fun replacementProducer(
        topicName: String,
        input: TestInputTopic<Long, String>
    ): CoProducer<Long, String> {
        return mockk<CoProducer<Long, String>>().apply {
            val k = slot<Long>()
            val v = slot<String>()
            coEvery { publish(topicName, capture(k), capture(v)) } answers {
                input.pipeInput(k.captured, v.captured)
                v.captured
            }
            coEvery { delete(topicName, capture(k)) } answers {
                input.pipeInput(k.captured, null)
            }
        }
    }

    private fun embeddedKafkaSupplier(embeddedKafka: KafkaStreams): StreamsBuilderFactoryBean {
        return mockk<StreamsBuilderFactoryBean>().apply {
            every<KafkaStreams> { kafkaStreams } returns embeddedKafka
            every<Boolean> { isRunning } returns true //or add a latch or
            every { topology.describe()?.toString() } returns "does not matter"
        }
    }

    private fun embeddedKafka(storeName: String): KafkaStreams =
        mockk<KafkaStreams>().apply {
            every {
                store<ReadOnlyWindowStore<Long, String>>(any())
            } returns kafka.driver.getWindowStore(storeName)
        }
}