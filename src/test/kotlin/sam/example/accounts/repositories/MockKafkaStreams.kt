package sam.example.accounts.repositories

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import java.util.*

class MockKafkaStreams(properties: Properties? = null, topologyInit: StreamsBuilder.() -> Unit):AutoCloseable{
    private var builder = StreamsBuilder()
    val props = properties ?: Properties().apply {
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass.name
    }
    val driver: TopologyTestDriver

    init {
       builder.topologyInit()
       driver = TopologyTestDriver(builder.build(), props)
    }
    override fun close(){
        driver.close()
    }
}