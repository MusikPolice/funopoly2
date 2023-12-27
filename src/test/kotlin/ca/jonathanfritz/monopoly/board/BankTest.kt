package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.exception.InsufficientFundsException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BankTest {

    @Test
    fun `pay player negative value throws exception`() {
        val player = Player("Cookie", 500)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        assertThrows<IllegalArgumentException> {
            bank.pay(player, -10)
        }

        assertEquals(startingBankBalance, bank.money)
        assertEquals(startingPlayerBalance, player.money)
    }

    @Test
    fun `pay player zero doesn't change balances`() {
        val player = Player("Elmo", 500)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        bank.pay(player, 0)

        assertEquals(startingBankBalance, bank.money)
        assertEquals(startingPlayerBalance, player.money)
    }

    @Test
    fun `pay player test`() {
        val player = Player("Elmo", 500)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        bank.pay(player, 100)

        assertEquals(startingBankBalance - 100, bank.money)
        assertEquals(startingPlayerBalance + 100, player.money)
    }

    @Test
    fun `charge player invalid values throws exception`() {
        val player = Player("Cookie", 500)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        assertThrows<IllegalArgumentException> {
            bank.pay(player, -10)
        }

        assertEquals(startingBankBalance, bank.money)
        assertEquals(startingPlayerBalance, player.money)
    }

    @Test
    fun `charge player zero not change balances`() {
        val player = Player("Cookie", 500)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        bank.pay(player, 0)

        assertEquals(startingBankBalance, bank.money)
        assertEquals(startingPlayerBalance, player.money)
    }

    @Test
    fun `charge player test`() {
        val player = Player("Elmo", 500)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        bank.charge(player, 100)

        assertEquals(startingBankBalance + 100, bank.money)
        assertEquals(startingPlayerBalance - 100, player.money)
    }

    @Test
    fun `charge player with insufficient funds throws exception`() {
        val player = Player("Elmo", 12)
        val bank = Bank()

        val startingBankBalance = bank.money
        val startingPlayerBalance = player.money

        assertThrows<InsufficientFundsException> {
            bank.charge(player, 100)
        }

        assertEquals(startingBankBalance, bank.money)
        assertEquals(startingPlayerBalance, player.money)
    }
}