package sam.example.accounts.repositories

import org.apache.kafka.streams.KafkaStreams
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import sam.example.accounts.Logging
import sam.example.accounts.logger

abstract class StreamCtx(val streamsFactory: StreamsBuilderFactoryBean? = null): Logging {
    private val log = logger()
    protected val streams: KafkaStreams by lazy {
        getKafkaStreams() ?: throw IllegalStateException("no kafka streams available")
    }
    /**
     * because spring boot is not helping
     */
    private fun getKafkaStreams(): KafkaStreams? {
        while (streamsFactory?.isRunning()?.not() == true) {
            log.info("Waiting for Kafka Streams to start up")
            Thread.sleep(1000)
        }
        try {
            log.debug(streamsFactory?.topology?.describe()?.toString())
        } catch (e: Exception) {
            log.debug("could not print topology")
        }
        return streamsFactory?.kafkaStreams
    }
}