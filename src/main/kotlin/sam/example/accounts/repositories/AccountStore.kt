package sam.example.accounts.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.stereotype.Component
import sam.example.accounts.*
import sam.example.accounts.configs.KafkaAccountConfig

interface AccountStore {
    suspend fun findOne(id: Long): Account?
    fun findAll(): Flow<Account>
    suspend fun create(command: AccountCreateCommand): Account
    suspend fun update(id: Long, command: AccountUpdateCommand): Account
    suspend fun delete(id: Long)
}

@Component
class AccountStoreImpl(
    val kafkaTemplate: CoProducer<Long, String>,
    streamsFactory: StreamsBuilderFactoryBean? = null,
    val config: KafkaAccountConfig,
) : AccountStore, Logging, StreamCtx(streamsFactory) {
    private val log = logger()
    private val accountStore: ReadOnlyKeyValueStore<Long, String?> by lazy {
        streams.store(
            StoreQueryParameters.fromNameAndType(
                NamedStores.account_store.name, QueryableStoreTypes.keyValueStore<Long, String?>()
            )
        )
    }

    override suspend fun findOne(id: Long): Account? {
        return accountStore.get(id)?.asAccount()
    }

    override fun findAll(): Flow<Account> = accountStore.all()
        .asFlow().mapNotNull { it.value?.asAccount() }

    override suspend fun create(command: AccountCreateCommand): Account {
        val customId = IdSource.newId()
        val result = kafkaTemplate.publish(
            config.accountTopicName,
            customId,
            Account(customId, command.name).asJson()
        )
        return result.asAccount()
    }

    override suspend fun update(id: Long, command: AccountUpdateCommand): Account {
        val existing = findOne(id)
        when (existing) {
            null -> throw NoSuchKey(id) //or non modified code
//             -> log.warn("nothing to update")//could ignore
            else -> {
                val result = kafkaTemplate.publish(
                    config.accountTopicName,
                    id,
                    Account(id, command.name).asJson()
                )
                return result.asAccount()
            }
        }
    }

    override suspend fun delete(id: Long) {
        val existingAccount = this.findOne(id)
        when (existingAccount) {
            null -> throw NoSuchKey(id)
            else -> kafkaTemplate.delete(config.accountTopicName, id)
        }
    }

}