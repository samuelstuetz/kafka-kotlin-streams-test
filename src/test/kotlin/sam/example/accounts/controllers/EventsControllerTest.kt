package sam.example.accounts.controllers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sam.example.accounts.*
import java.time.Instant

/**
 * controller behaviour tests
 * what behaviour is intended vs an accident
 */
class EventsControllerTest {

    var store: EventStore = mockk()
    var controller = EventsController(store)

    @Test
    fun `create returns created`() {
        runBlocking {
            val genTs = Instant.now()
            val cmd = EventCreate("but this")
            coEvery { store.create(1L, any()) } returns Event(genTs, 1L, "not this")
            coEvery { store.create(2L, cmd) } returns Event(genTs, 2L, "but this")
            val response:AccountScopedEventDto = controller.publish(2L, cmd)
            coVerify { store.create(2L, cmd) }
            assertThat(response.type).isEqualTo(cmd.type)
            assertThat(response.happenedAt).isEqualTo(genTs)
        }
    }

    @Test
    fun `returns some stats`() {
        runBlocking {
            coEvery { store.statistics(1L) } returns Aggregate(mutableMapOf("some" to 1L, "other" to 2L))
            val stat = controller.summary(1L)
            coVerify { store.statistics(1L) }
            assertThat(stat).hasSize(2)
        }
    }

    @Test
    fun `throw no such key for missing data`() {
        runBlocking {
            coEvery { store.statistics(1L) } returns null
            assertThrows<NoSuchKey> {
                controller.summary(1L)
            }
        }
    }

    @Test
    fun `empty is just empty`() {
        runBlocking {
            coEvery { store.statistics(1L) } returns Aggregate(mutableMapOf())
            controller.summary(1L).isEmpty()
        }
    }

}