package sam.example.accounts

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface CoProducer<K,V> {
    suspend fun publish(topic: String, key:K, value:V):V
    suspend fun delete(topic: String, key: K)
}

/**
 * extension function to publish V to a kafka topic
 * wraps the KafkaTemplate to use a clean coroutine style code by eliminating the callback to here. 
 */
suspend inline fun <reified K : Any, reified V : Any> KafkaTemplate<K, V>.publish(
    topic: String,
    key: K,
    value: V
): SendResult<K, V> =
    suspendCoroutine { continuation: Continuation<SendResult<K, V>> ->
        this.send(topic, key, value).addCallback(
            { result: SendResult<K, V>? ->
                result?.let { nonNullResult -> continuation.resume(nonNullResult) }
                    ?: continuation.resumeWithException(
                        IllegalStateException("KafkaTemplate returned null SendResult on success")
                    )
            },
            { continuation.resumeWithException(it) }
        )
    }

/**
 * same as @see publish but inserts just a tombstone (null) for the data
 * separate function so publish can remain non null
 */
suspend inline fun <reified K : Any, reified V : Any> KafkaTemplate<K, V>.delete(
    topic: String,
    key: K,
): SendResult<K, V?> =
    suspendCoroutine { continuation: Continuation<SendResult<K, V?>> ->
        this.send(topic, key, null).addCallback(
            { result: SendResult<K, V?>? ->
                result?.let { nonNullResult -> continuation.resume(nonNullResult) }
                    ?: continuation.resumeWithException(
                        IllegalStateException("KafkaTemplate returned null SendResult on success")
                    )
            },
            { continuation.resumeWithException(it) }
        )
    }

/**
 * similar to scala style
 * just put get logger code out of the way
 */
interface Logging
inline fun <reified T : Logging> T.logger(): Logger = getLogger(T::class.java)
