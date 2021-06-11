package sam.example.accounts.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.utils.Utils
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import sam.example.accounts.Event
import sam.example.accounts.asEvent
import java.time.Duration
import java.util.*

/**
 * One of consumer.
 * Use by creating an instance consuming it and then throw it away.
 * It attaches to one partition only and filters that for the searched key and returns all events in it.
 *
 * requirements for read topic:
 *  - topic should have a reasonable ttl (delete) or it will read too much data
 *  - topic should have a delete policy not compact or else the operation is not deterministic
 *  - if topic starts to rebalance partitions it will stop working and may need to restart
 *  - topic should use DefaultPartitioner as Hashing + partition count decide in which partition a key can be found.
 *  - topic should have a high partition count as this reads the entire partition to filter
 *    ( this is where Kafka just does not make sense for the use case )
 *
 * @param kafkaProperties spring kafka properties to be able to configure the consumer
 * @param topic to consume
 * @param key to filter by
 * @param numPartitions to find along with DefaultPartitioner hashing strategy the right partition
 */
class ConsumerForKey(kafkaProperties:KafkaProperties, val topic: String, val key: Long, val numPartitions: Int) {
    private val kafkaConsumer: KafkaConsumer<Long, String>
    private var relevantPartition = TopicPartition(topic, partition(key))
    private var partitions = mutableListOf(relevantPartition)

    private fun partition(k: Long) = LongSerializer().serialize(topic, k).let { keyBytes ->
        Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions
    }

    init {
        val config = kafkaProperties.buildConsumerProperties().apply {
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.java
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            this[ConsumerConfig.GROUP_ID_CONFIG] = UUID.randomUUID().toString()
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest" //makes sense to know when to stop
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        }
        kafkaConsumer = KafkaConsumer<Long,String>(config).apply {
            assign(mutableListOf(TopicPartition(topic, partition(key))))
        }
    }

    /**
     * returns a coroutine flow of this kafka consumer emiting only the messages for the given key.
     * the consumer only reads one partition in which the key should be given its DefaultPartitioner
     * and filters that
     * It first marks the current offset, then seeks to the beginning of the partition
     * then emit all messages until the marked offset it was at when the operation started
     * then completes
     */
    fun consume(): Flow<Event> = flow {
        kafkaConsumer.use { kc ->
            kc.seekToEnd(partitions)
            val offsetAtStart = kc.position(relevantPartition, Duration.ofMillis(3000))
            println(offsetAtStart)
            if (offsetAtStart > 1){
                kc.seekToBeginning(partitions)
                while (kc.position(relevantPartition) < offsetAtStart) {
                    println(kc.position(relevantPartition))
                    kc.poll(Duration.ofMillis(200))?.forEach {
                        if (it.key() == key) {
                           emit(it.value().asEvent())
                        }
                    }
                }
            }
        }
    }
}