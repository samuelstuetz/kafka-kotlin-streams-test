package sam.example.accounts.configs

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import sam.example.accounts.*
import java.time.Duration

/**
 * these config interfaces should be all implemented by the same Config class.
 * their field names not overlap.
 * That way you have strong typed configs you cannot use wrong or forget,
 * and its easier to understand.
 * I dislike Springs way of using strings everywhere and then randomly injecting stuff
 * you have to debug painfully what you need where as settings.
 */

/**
 * relevant config EventStore
 */
interface KafkaEventConfig {
    val eventTopicName:String
    val eventTopicPartitions:Int
}

/**
 * relevant config AccountStore
 */
interface KafkaAccountConfig {
    val accountTopicName:String
}

@Configuration
@EnableKafka
class KafkaConfig(
    //add value annotations
    @Value("\${spring.kafka.properties.bootstrap.servers}") val bootstrapServers: String = "localhost:9092",
    override val eventTopicPartitions: Int = 4,
    private val accountTopicPartitions: Int = 10,
    override val accountTopicName: String = Topics.accounts.toString(),
    override val eventTopicName: String = Topics.events.toString(),
    val eventTopicTTLDays: Long = 30L,
) : Logging, KafkaEventConfig, KafkaAccountConfig {

    @Bean
    fun producer(template:KafkaTemplate<Long,String>) = object : CoProducer<Long,String> {
        override suspend fun delete(topic: String, key: Long) {
           template.delete(topic,key).producerRecord.value()
        }
        override suspend fun publish(topic: String, key: Long, value: String): String {
           return template.publish(topic, key, value).producerRecord.value()
        }
    }

    /**
     * topic to receive account events create/update/deletes
     */
    @Bean
    fun accountTopic(): NewTopic = TopicBuilder.name(accountTopicName)
        .partitions(accountTopicPartitions)
        .replicas(3)
        .compact()
        .build()

    /**
     * topic to receive the "events"
     * this will be cleaned up, ttl 30 days
     * the kafka streams state store if running will persist the aggregate daily statistics
     */
    @Bean
    fun eventTopic(): NewTopic = TopicBuilder.name(eventTopicName)
        .partitions(eventTopicPartitions)
        .replicas(3)
        .configs(
            mapOf(
                TopicConfig.CLEANUP_POLICY_CONFIG to "delete",
                TopicConfig.RETENTION_MS_CONFIG to
                        Duration.ofDays(eventTopicTTLDays).toMillis().toString()
            )
        )
        .build()

}
