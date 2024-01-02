package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.exception.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `sell property to player with insufficient funds test`() {
        val player = Player("Oscar")
        val bank = Bank()
        val bankStartingBalance = bank.money

        assertThrows<InsufficientFundsException> {
            bank.sellPropertyToPlayer(Property.MediterraneanAvenue::class, player)
        }
        assertEquals(0, player.money)
        assertEquals(bankStartingBalance, bank.money)
        assertTrue(player.deeds.isEmpty())
    }

    @Test
    fun `sell property twice test`() {
        val bert = Player("Bert", 100)
        val ernie = Player("Ernie", 100)
        val bank = Bank()
        val bankStartingBalance = bank.money

        // bert successfully buys a property
        bank.sellPropertyToPlayer(Property.MediterraneanAvenue::class, bert)
        assertTrue(bert.deeds.any { it.key::class == Property.MediterraneanAvenue::class })
        assertEquals(40, bert.money)
        assertEquals(bankStartingBalance + 60, bank.money)

        // that same property cannot be sold to ernie because the bank no longer has it
        assertThrows<PropertyOwnershipException> {
            bank.sellPropertyToPlayer(Property.MediterraneanAvenue::class, ernie)
        }
        assertEquals(100, ernie.money)
        assertEquals(bankStartingBalance + 60, bank.money)
        assertTrue(ernie.deeds.isEmpty())
    }

    @Test
    fun `building a house on an unowned property throws an exception`() {
        val player = Player("Count von Count")
        val bank = Bank()

        assertThrows<PropertyOwnershipException> {
            bank.buildHouse(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `building a house without a monopoly throws an exception`() {
        val player = Player("Count von Count", 500)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        assertThrows<MonopolyOwnershipException> {
            bank.buildHouse(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `uneven house building throws an exception`() {
        val player = Player("Count von Count", 5000)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // can build a single house on either property
        bank.buildHouse(Property.ParkPlace::class, player)

        // attempting to build another house on that property without first developing
        // the other property in the monopoly throws an exception
        assertThrows<PropertyDevelopmentException> {
            bank.buildHouse(Property.ParkPlace::class, player)
        }

        // the other property in the monopoly can be developed
        bank.buildHouse(Property.Boardwalk::class, player)

        // because the properties are evenly developed, a second house can be added to either
        bank.buildHouse(Property.Boardwalk::class, player)

        // adding a third house to Boardwalk throws an exception
        assertThrows<PropertyDevelopmentException> {
            bank.buildHouse(Property.Boardwalk::class, player)
        }

        // but Park Place can still be developed, leaving us with two houses on each property
        bank.buildHouse(Property.ParkPlace::class, player)
        assertTrue(player.deeds.map { it.value.numHouses }.all { it == 2 })
    }

    @Test
    fun `building more than four houses throws an exception`() {
        val player = Player("Count von Count", 5000)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // the first four houses build as expected
        (0 until 4).forEach { _ ->
            bank.buildHouse(Property.ParkPlace::class, player)
            bank.buildHouse(Property.Boardwalk::class, player)
        }

        // but attempting to build a fifth house causes an exception to be thrown
        assertThrows<PropertyDevelopmentException> {
            bank.buildHouse(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `the bank has a limited number of houses to sell`() {
        val player = Player("Count von Count", 5000)
        val bank = Bank(availableHouses = 2)

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // the player can build on each of their properties
        bank.buildHouse(Property.ParkPlace::class, player)
        bank.buildHouse(Property.Boardwalk::class, player)

        // attempting to build another house throws an exception because the bank has no more to sell
        assertThrows<InsufficientTokenException> {
            bank.buildHouse(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `building a house with insufficient funds throws an exception`() {
        val player = Player("Count von Count", 750)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        assertThrows<InsufficientFundsException> {
            bank.buildHouse(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `build house test`() {
        val player = Player("Count von Count", 950)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        bank.buildHouse(Property.ParkPlace::class, player)
        assertEquals(0, player.money)

        val development = player.deeds.filter { it.key::class == Property.ParkPlace::class }.values.first()
        assertEquals(1, development.numHouses)
        assertFalse(development.hotel)
        assertEquals(0, player.money)
    }

    @Test
    fun `building a hotel on an unowned property throws an exception`() {
        val player = Player("Count von Count")
        val bank = Bank()

        assertThrows<PropertyOwnershipException> {
            bank.buildHotel(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `building a hotel without a monopoly throws an exception`() {
        val player = Player("Count von Count", 500)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        assertThrows<MonopolyOwnershipException> {
            bank.buildHotel(Property.ParkPlace::class, player)
        }
    }

    @Test
    fun `uneven hotel building throws an exception`() {
        val player = Player("Count von Count", 5000)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // player builds four houses on Park Place but only three on Boardwalk
        bank.buildHouse(Property.ParkPlace::class, player)
        (1 .. 3).forEach { _ ->
            bank.buildHouse(Property.Boardwalk::class, player)
            bank.buildHouse(Property.ParkPlace::class, player)
        }

        // cannot build a hotel on Park Place, even though it has four houses
        // because Boardwalk only has three
        assertThrows<PropertyDevelopmentException> {
            bank.buildHotel(Property.ParkPlace::class, player)
        }

        // building a house on boardwalk resolves the issue
        bank.buildHouse(Property.Boardwalk::class, player)

        bank.buildHotel(Property.ParkPlace::class, player)
        bank.buildHotel(Property.Boardwalk::class, player)

    }

    @Test
    fun `building more than one hotel throws an exception`() {
        val player = Player("Count von Count", 5000)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // hotels build as expected
        (1 .. 4).forEach { _ ->
            bank.buildHouse(Property.ParkPlace::class, player)
            bank.buildHouse(Property.Boardwalk::class, player)
        }
        bank.buildHotel(Property.ParkPlace::class, player)
        bank.buildHotel(Property.Boardwalk::class, player)

        // attempting to build another hotel on either property throws an exception
        assertThrows<PropertyDevelopmentException> {
            bank.buildHotel(Property.ParkPlace::class, player)
        }
        assertThrows<PropertyDevelopmentException> {
            bank.buildHotel(Property.Boardwalk::class, player)
        }
    }

    @Test
    fun `the bank has a limited number of hotels to sell`() {
        val player = Player("Count von Count", 5000)
        val bank = Bank(availableHotels = 1)

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // the player can build on each of their properties
        (1 .. 4).forEach { _ ->
            bank.buildHouse(Property.ParkPlace::class, player)
            bank.buildHouse(Property.Boardwalk::class, player)
        }
        bank.buildHotel(Property.ParkPlace::class, player)

        // building a legal hotel fails because the bank has run out
        assertThrows<InsufficientTokenException> {
            bank.buildHotel(Property.Boardwalk::class, player)
        }
    }

    @Test
    fun `building a hotel with insufficient funds throws an exception`() {
        val player = Player("Count von Count", 2549)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        (1 .. 4).forEach { _ ->
            bank.buildHouse(Property.ParkPlace::class, player)
            bank.buildHouse(Property.Boardwalk::class, player)
        }

        assertThrows<InsufficientFundsException> {
            bank.buildHotel(Property.Boardwalk::class, player)
        }
    }

    @Test
    fun `build hotel test`() {
        val player = Player("Count von Count", 2750)
        val bank = Bank()

        bank.sellPropertyToPlayer(Property.ParkPlace::class, player)
        bank.sellPropertyToPlayer(Property.Boardwalk::class, player)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        (1 .. 4).forEach { _ ->
            bank.buildHouse(Property.ParkPlace::class, player)
            bank.buildHouse(Property.Boardwalk::class, player)
        }
        bank.buildHotel(Property.ParkPlace::class, player)
        bank.buildHotel(Property.Boardwalk::class, player)

        assertTrue(player.deeds.values.all { it.numHouses == 0 && it.hotel })
        assertEquals(0, player.money)
    }
}