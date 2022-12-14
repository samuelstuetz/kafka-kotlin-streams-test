package sam.example.accounts.configs

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.state.WindowBytesStoreSupplier
import org.apache.kafka.streams.state.WindowStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import sam.example.accounts.*
import java.time.Duration


@Configuration
@EnableKafkaStreams
class KafkaStreamsTopology(
    //add value annotations
    private val eventWindowSize: Duration = Duration.ofDays(1),
) : Logging {

    @Bean
    fun accountTableEventSourced(
        @Qualifier(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
        builder: StreamsBuilder,
    ): KStream<Long, String> {
        val stream = builder.stream<Long, String>(
            Topics.accounts.name,
            Consumed.`as`<Long?, String?>("account_log")
                .withKeySerde(Serdes.Long())
                .withValueSerde(Serdes.String())
        )
        stream.toTable(
            Named.`as`(NamedStores.account_store.toString()),
            Materialized.`as`<Long?, String?>(Stores.persistentKeyValueStore(NamedStores.account_store.toString()))
                .withKeySerde(Serdes.Long())
                .withValueSerde(Serdes.String())
        )
        return stream
    }

    @Bean
    fun dailyStatistics(builder: StreamsBuilder): KTable<Windowed<Long>, String> =
        builder.stream<Long, String>(Topics.events.name, Consumed.`as`("event_log"))
            .groupByKey()
            //tumbling window aligned by epoch so midnight UnixTime
            .windowedBy(TimeWindows.of(eventWindowSize))
            .aggregate<String>(
                { Aggregate().asJson() },
                { _: Long, event: String, agg: String ->
                    event.asEvent().let { agg.aggregate().add(it.type) }.asJson()
                },
                Named.`as`("collect_to_daily_map"),
                Materialized.`as`<Long?, String?, WindowStore<Bytes, ByteArray>?>(NamedStores.daily_stats_windowed.toString())
            )

}
