package sam.example.accounts.controllers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sam.example.accounts.*
import sam.example.accounts.repositories.AccountStore

/**
 * controller behaviour tests
 * Not much logic to test here
 */
class AccountsControllerTest {

    var store: AccountStore = mockk()
    var controller:AccountsController = AccountsController(store)

    @Test
    fun findAll() {
        runBlocking {
            val list = (0..3).map { Account(it.toLong(), "name$it")}
            coEvery { store.findAll() } returns flowOf(Account(1L, "name1"), Account(2L, "name2"))
            assertThat(store.findAll().toList()).hasSize(2)
        }
    }

    @Test
    fun `find one returns dto`() {
        runBlocking {
            val id = 1L
            coEvery { store.findOne(id) } returns Account(id, "test")
            assertThat(controller.findOne(id)).isEqualTo(AccountDto(id, "test"))
            coVerify { store.findOne(id) }
        }
    }

    @Test
    fun `create returns dto`() {
        runBlocking {
            val id = 1L
            val cmd = AccountCreateCommand("test")
            coEvery { store.create(cmd) } returns Account(id, "test")
            assertThat(controller.createOne(cmd)).isEqualTo(AccountDto(id, "test"))
            coVerify { store.create(cmd) }
        }
    }

    @Test
    fun `find nothing throws`() {
        runBlocking {
            val missingId = 1L
            coEvery { store.findOne(any()) } throws NoSuchKey(missingId)
            assertThrows<NoSuchKey> {
                controller.findOne(missingId)
            }
        }
    }

    @Test
    fun `update returns update object`() {
        runBlocking {
            val id = 1L
            val cmd = AccountUpdateCommand("test2")
            coEvery { store.update(id,cmd) } returns Account(id, "test2")
            assertThat(controller.update(id, cmd)).isEqualTo(AccountDto(id, "test2"))
            coVerify { store.update(id, cmd) }
        }
    }

    @Test
    fun `update does not swallow no such key`() {
        runBlocking {
            val id = 1L
            val cmd = AccountUpdateCommand("test2")
            coEvery { store.update(id,cmd) } throws NoSuchKey(id)
            assertThrows<NoSuchKey> {
                controller.update(id, cmd)
            }
        }
    }

    @Test
    fun delete() {
        runBlocking {
            val id= 1L
            coEvery { store.delete(id ) } throws NoSuchKey(id)
            assertThrows<NoSuchKey> {
                controller.delete(id)
            }
            coVerify { store.delete(id) }
        }
    }

}