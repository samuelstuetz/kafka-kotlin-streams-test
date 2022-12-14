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
import org.apache.kafka.streams.state.KeyValueStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import sam.example.accounts.*
import sam.example.accounts.configs.KafkaConfig
import sam.example.accounts.configs.KafkaStreamsTopology
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountStoreImplTest {
    /**
     * Embedded Kafka does not work to test this.
     * it does not wait for the stream to start and connects to the actual kafka.
     */
    //these should be possible to put random names need
    private val  topicName = Topics.accounts.toString()
    private val  storeName = NamedStores.account_store.toString()

    private val baseConfig = KafkaConfig()
    private val  streamConfig = KafkaStreamsTopology()
    private val  kafka = MockKafkaStreams { streamConfig.accountTableEventSourced(this) }
    private val inputTopic: TestInputTopic<Long, String> = kafka.driver
        .createInputTopic(topicName, Serdes.Long().serializer(), Serdes.String().serializer())
    val store = setup(topicName, storeName, inputTopic)

    @BeforeEach
    fun flushStores(){
        val internalStore = kafka.driver.getKeyValueStore<Long,String>(storeName)
        internalStore.all().forEach {
            internalStore.delete(it.key)
        }
    }

    @AfterAll
    fun tearDown() {
        kafka.close()
    }

    @Test
    fun `insert and find an account`() {
        runBlocking {
            val accName1 = "test1"
            val acc1 = store.create(AccountCreateCommand(accName1))
            assertThat(store.findOne(acc1.id)?.name).`as`("name of found account")
                .isEqualTo(accName1)
        }
    }

    @Test
    fun `one that does not exist just returns null`(){
        runBlocking {
            val nonsenseId = Random().nextLong()
            assertThat(store.findOne(nonsenseId))
                .`as`("non existent should return null")
                .isNull()
        }
    }

    @Test
    fun `insert random stuff and list it`() {
        runBlocking {
            val inserted = (1..3).map { "a${Random().nextInt()}" }
                .map { store.create(AccountCreateCommand(it));it }
            val list = store.findAll().toList()
            assertThat(list).hasSize(3)
            assertThat(list.map { it.name }).`as`("all inserted random names match")
                .containsAll(inserted)
        }
    }

    @Test
    fun `insert update account`() {
        runBlocking {
            val acc1 = store.create(AccountCreateCommand("firstName"))
            store.update(acc1.id, AccountUpdateCommand("updatedName1"))
            assertThat(store.findOne(acc1.id)?.name).`as`("updated name of found account")
                .isEqualTo("updatedName1")
        }
    }

    @Test
    fun `delete inserted account`() {
        runBlocking {
            val acc1 = store.create(AccountCreateCommand("something"))
            assertThat( store.findOne(acc1.id) ).`as`("should not be found")
                .isNotNull()
            store.delete(acc1.id)
            assertThat( store.findOne(acc1.id) ).`as`("should not be found")
                .isNull()
        }
    }

    private fun setup(topicName:String, storeName: String, inputTopic:TestInputTopic<Long, String>): AccountStoreImpl {
        //too much mocking :(
        val mockProducer = replacementProducer(topicName, inputTopic)
        val embeddedKafka = embeddedKafka(storeName)
        val embeddedKafkaSupplier = embeddedKafkaSupplier(embeddedKafka)
        return AccountStoreImpl(mockProducer, embeddedKafkaSupplier, baseConfig)
    }

    /**
     * just republishes unchanged what normally goes to eh CoProducer to the TestInputTopic instead
     * essentially replaces the backend of it.
     */
    private fun replacementProducer(
        topicName: String,
        accountTopic: TestInputTopic<Long, String>
    ): CoProducer<Long, String> {
        return mockk<CoProducer<Long, String>>().apply {
            val k = slot<Long>()
            val v = slot<String>()
            coEvery { publish(topicName, capture(k), capture(v)) } answers {
                accountTopic.pipeInput(k.captured, v.captured)
                v.captured
            }
            coEvery { delete(topicName, capture(k)) } answers {
                accountTopic.pipeInput(k.captured, null)
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

    private fun embeddedKafka(storeName: String):KafkaStreams =
        mockk<KafkaStreams>().apply {
            every {
                store<KeyValueStore<Long, String>>(any())
            } returns kafka.driver.getKeyValueStore<Long,String>(storeName)
        }
}