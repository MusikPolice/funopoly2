@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.statistics

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.card.Card
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.event.GameEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GameStatisticsTest {
    private lateinit var stats: GameStatistics
    private lateinit var player1: Player
    private lateinit var player2: Player

    @BeforeEach
    fun setup() {
        stats = GameStatistics()
        player1 = Player("Player1")
        player2 = Player("Player2")
    }

    @Test
    fun `initial snapshot has zero values`() {
        val snapshot = stats.snapshot()

        assertEquals(0, snapshot.totalRounds)
        assertFalse(snapshot.gameEnded)
        assertNull(snapshot.winner)
        assertEquals(0, snapshot.totalDiceRolls)
        assertEquals(0, snapshot.totalBankPayments)
        assertEquals(0, snapshot.totalPropertiesPurchased)
        assertEquals(0, snapshot.totalJailSentences)
    }

    @Test
    fun `tracks round lifecycle`() {
        stats.onEvent(GameEvent.RoundStarted(1))
        stats.onEvent(GameEvent.RoundEnded(1))
        stats.onEvent(GameEvent.RoundStarted(2))
        stats.onEvent(GameEvent.RoundEnded(2))

        val snapshot = stats.snapshot()
        assertEquals(2, snapshot.totalRounds)
    }

    @Test
    fun `tracks dice rolls`() {
        stats.onEvent(GameEvent.DiceRolled(player1, 3, 4, false))
        stats.onEvent(GameEvent.DiceRolled(player1, 5, 5, true))
        stats.onEvent(GameEvent.DiceRolled(player2, 2, 6, false))

        val snapshot = stats.snapshot()
        assertEquals(3, snapshot.totalDiceRolls)
        assertEquals(1, snapshot.doublesCount[player1])
        assertNull(snapshot.doublesCount[player2]) // Never rolled doubles
        assertEquals(8.33, snapshot.averageDiceRoll, 0.01) // (7 + 10 + 8) / 3 = 8.33
    }

    @Test
    fun `tracks player movement and GO passings`() {
        stats.onEvent(GameEvent.PlayerMoved(player1, 0, 5, false))
        stats.onEvent(GameEvent.PlayerMoved(player1, 35, 3, true))
        stats.onEvent(GameEvent.PlayerMoved(player2, 38, 2, true))

        val snapshot = stats.snapshot()
        assertEquals(1, snapshot.goPassings[player1])
        assertEquals(1, snapshot.goPassings[player2])
    }

    @Test
    fun `tracks tile landings`() {
        stats.onEvent(GameEvent.TileLanded(player1, Tile.Go))
        stats.onEvent(GameEvent.TileLanded(player1, Tile.Go))
        stats.onEvent(GameEvent.TileLanded(player2, Tile.FreeParking))

        val snapshot = stats.snapshot()
        assertEquals(2, snapshot.tileLandings["Go"])
        assertEquals(1, snapshot.tileLandings["FreeParking"])
    }

    @Test
    fun `tracks bank payments`() {
        stats.onEvent(GameEvent.BankPaidPlayer(player1, 200, "for passing go"))
        stats.onEvent(GameEvent.BankPaidPlayer(player1, 50, "test"))
        stats.onEvent(GameEvent.BankPaidPlayer(player2, 100, "test"))

        val snapshot = stats.snapshot()
        assertEquals(350, snapshot.totalBankPayments)
    }

    @Test
    fun `tracks bank charges`() {
        stats.onEvent(GameEvent.PlayerChargedByBank(player1, 200, "income tax"))
        stats.onEvent(GameEvent.PlayerChargedByBank(player2, 100, "luxury tax"))

        val snapshot = stats.snapshot()
        assertEquals(300, snapshot.totalBankCharges)
    }

    @Test
    fun `tracks rent payments`() {
        val boardwalk = Property.Boardwalk()
        val readingRailroad = Railroad.ReadingRailroad()
        
        stats.onEvent(GameEvent.RentPaid(player1, player2, 50, boardwalk))
        stats.onEvent(GameEvent.RentPaid(player2, player1, 35, readingRailroad))

        val snapshot = stats.snapshot()
        assertEquals(85, snapshot.totalRentPaid)
        assertEquals(2, snapshot.rentTransactions.size)
        assertEquals(50, snapshot.rentTransactions[0].amount)
        assertEquals(player2, snapshot.rentTransactions[0].recipient)
    }

    @Test
    fun `tracks property purchases`() {
        val boardwalk = Property.Boardwalk()
        val parkPlace = Property.ParkPlace()
        val readingRailroad = Railroad.ReadingRailroad()
        
        stats.onEvent(GameEvent.PropertyPurchased(player1, boardwalk, 400))
        stats.onEvent(GameEvent.PropertyPurchased(player1, parkPlace, 350))
        stats.onEvent(GameEvent.PropertyPurchased(player2, readingRailroad, 200))

        val snapshot = stats.snapshot()
        assertEquals(3, snapshot.totalPropertiesPurchased)
        assertEquals(950, snapshot.totalPropertySpending)
        assertEquals(2, snapshot.propertiesByPlayer[player1])
        assertEquals(1, snapshot.propertiesByPlayer[player2])
    }

    @Test
    fun `tracks property mortgages`() {
        val boardwalk = Property.Boardwalk()
        val parkPlace = Property.ParkPlace()
        
        stats.onEvent(GameEvent.PropertyMortgaged(player1, boardwalk, 200))
        stats.onEvent(GameEvent.PropertyMortgaged(player1, parkPlace, 175))

        val snapshot = stats.snapshot()
        assertEquals(2, snapshot.totalMortgages)
    }

    @Test
    fun `tracks property unmortgages`() {
        val boardwalk = Property.Boardwalk()
        
        stats.onEvent(GameEvent.PropertyMortgaged(player1, boardwalk, 200))
        stats.onEvent(GameEvent.PropertyUnmortgaged(player1, boardwalk, 220))

        val snapshot = stats.snapshot()
        assertEquals(1, snapshot.totalMortgages)
        assertEquals(1, snapshot.totalUnmortgages)
    }

    @Test
    fun `tracks house purchases`() {
        val boardwalk = Property.Boardwalk()
        val parkPlace = Property.ParkPlace()
        
        stats.onEvent(GameEvent.HousePurchased(player1, boardwalk, 1, 200))
        stats.onEvent(GameEvent.HousePurchased(player1, boardwalk, 2, 200))
        stats.onEvent(GameEvent.HousePurchased(player1, parkPlace, 1, 200))

        val snapshot = stats.snapshot()
        assertEquals(3, snapshot.totalHousesPurchased) // 3 transactions (Boardwalk x2, ParkPlace x1)
        assertEquals(3, snapshot.developmentByColorGroup[boardwalk.colourGroup]) // All 3 in same color group
    }

    @Test
    fun `tracks hotel purchases`() {
        val boardwalk = Property.Boardwalk()
        val medAvenue = Property.MediterraneanAvenue()
        
        stats.onEvent(GameEvent.HotelPurchased(player1, boardwalk, 200))
        stats.onEvent(GameEvent.HotelPurchased(player2, medAvenue, 50))

        val snapshot = stats.snapshot()
        assertEquals(2, snapshot.totalHotelsPurchased)
    }

    @Test
    fun `tracks house sales`() {
        val boardwalk = Property.Boardwalk()
        
        stats.onEvent(GameEvent.HouseSold(player1, boardwalk, 3, 100))
        stats.onEvent(GameEvent.HouseSold(player1, boardwalk, 2, 100))

        val snapshot = stats.snapshot()
        assertEquals(2, snapshot.totalHousesSold)
    }

    @Test
    fun `tracks hotel sales`() {
        val boardwalk = Property.Boardwalk()
        
        stats.onEvent(GameEvent.HotelSold(player1, boardwalk, 100))

        val snapshot = stats.snapshot()
        assertEquals(1, snapshot.totalHotelsSold)
    }

    @Test
    fun `tracks jail events`() {
        stats.onEvent(GameEvent.PlayerSentToJail(player1, "landed on Go To Jail"))
        stats.onEvent(GameEvent.PlayerSentToJail(player1, "three consecutive doubles"))
        stats.onEvent(GameEvent.PlayerSentToJail(player2, "landed on Go To Jail"))

        val snapshot = stats.snapshot()
        assertEquals(3, snapshot.totalJailSentences)
    }

    @Test
    fun `tracks jail release methods`() {
        stats.onEvent(GameEvent.PlayerLeftJail(player1, "rolled doubles"))
        stats.onEvent(GameEvent.PlayerLeftJail(player1, "paid fee"))
        stats.onEvent(GameEvent.PlayerLeftJail(player2, "used card"))
        stats.onEvent(GameEvent.PlayerLeftJail(player2, "paid fee"))

        val snapshot = stats.snapshot()
        assertEquals(1, snapshot.jailReleasesByMethod["rolled doubles"])
        assertEquals(2, snapshot.jailReleasesByMethod["paid fee"])
        assertEquals(1, snapshot.jailReleasesByMethod["used card"])
    }

    @Test
    fun `tracks card draws`() {
        stats.onEvent(GameEvent.CardDrawn(player1, "Chance", Card.AdvanceToGo))
        stats.onEvent(GameEvent.CardDrawn(player1, "Community Chest", Card.AdvanceToGo))
        stats.onEvent(GameEvent.CardDrawn(player2, "Chance", Card.AdvanceToGo))

        val snapshot = stats.snapshot()
        assertEquals(3, snapshot.totalCardsDrawn)
        assertEquals(2, snapshot.cardsByDeck["Chance"])
        assertEquals(1, snapshot.cardsByDeck["Community Chest"])
    }

    @Test
    fun `tracks bankruptcies`() {
        val bank = Bank()
        stats.onEvent(GameEvent.PlayerBankrupted(player1, bank, 15, 250))
        stats.onEvent(GameEvent.PlayerBankrupted(player2, player1, 23, 100))

        val snapshot = stats.snapshot()
        assertEquals(2, snapshot.totalBankruptcies)
        assertEquals(listOf(15, 23), snapshot.bankruptcyRounds)
    }

    @Test
    fun `tracks game end with winner`() {
        stats.onEvent(GameEvent.GameEnded(player1, 25, "bankruptcy"))

        val snapshot = stats.snapshot()
        assertTrue(snapshot.gameEnded)
        assertEquals(player1, snapshot.winner)
        assertEquals(25, snapshot.totalRounds)
        assertEquals("bankruptcy", snapshot.endReason)
    }

    @Test
    fun `tracks game end without winner`() {
        stats.onEvent(GameEvent.GameEnded(null, 100, "max rounds reached"))

        val snapshot = stats.snapshot()
        assertTrue(snapshot.gameEnded)
        assertNull(snapshot.winner)
        assertEquals(100, snapshot.totalRounds)
        assertEquals("max rounds reached", snapshot.endReason)
    }

    @Test
    fun `snapshot is immutable and repeatable`() {
        stats.onEvent(GameEvent.DiceRolled(player1, 3, 4, false))
        
        val snapshot1 = stats.snapshot()
        val snapshot2 = stats.snapshot()
        
        assertEquals(snapshot1.totalDiceRolls, snapshot2.totalDiceRolls)
        assertEquals(snapshot1.averageDiceRoll, snapshot2.averageDiceRoll, 0.01)
    }

    @Test
    fun `snapshot reflects cumulative data`() {
        val boardwalk = Property.Boardwalk()
        val parkPlace = Property.ParkPlace()
        
        // Add some events
        stats.onEvent(GameEvent.DiceRolled(player1, 3, 4, false))
        stats.onEvent(GameEvent.PropertyPurchased(player1, boardwalk, 400))
        
        val snapshot1 = stats.snapshot()
        assertEquals(1, snapshot1.totalDiceRolls)
        assertEquals(1, snapshot1.totalPropertiesPurchased)
        
        // Add more events
        stats.onEvent(GameEvent.DiceRolled(player2, 5, 6, false))
        stats.onEvent(GameEvent.PropertyPurchased(player2, parkPlace, 350))
        
        val snapshot2 = stats.snapshot()
        assertEquals(2, snapshot2.totalDiceRolls)
        assertEquals(2, snapshot2.totalPropertiesPurchased)
        assertEquals(750, snapshot2.totalPropertySpending)
    }

    @Test
    fun `handles zero dice rolls gracefully`() {
        val snapshot = stats.snapshot()
        assertEquals(0.0, snapshot.averageDiceRoll, 0.01)
    }

    @Test
    fun `tracks complex game scenario`() {
        val boardwalk = Property.Boardwalk()
        
        // Simulate a mini game
        stats.onEvent(GameEvent.RoundStarted(1))
        stats.onEvent(GameEvent.TurnStarted(player1, 1))
        stats.onEvent(GameEvent.DiceRolled(player1, 3, 4, false))
        stats.onEvent(GameEvent.PlayerMoved(player1, 0, 7, false))
        stats.onEvent(GameEvent.TileLanded(player1, Tile.Go))
        stats.onEvent(GameEvent.PropertyPurchased(player1, boardwalk, 400))
        stats.onEvent(GameEvent.TurnEnded(player1, 1))
        
        stats.onEvent(GameEvent.TurnStarted(player2, 1))
        stats.onEvent(GameEvent.DiceRolled(player2, 5, 5, true))
        stats.onEvent(GameEvent.PlayerMoved(player2, 0, 10, false))
        stats.onEvent(GameEvent.BankPaidPlayer(player2, 200, "for passing go"))
        stats.onEvent(GameEvent.TurnEnded(player2, 1))
        stats.onEvent(GameEvent.RoundEnded(1))
        
        stats.onEvent(GameEvent.GameEnded(player1, 1, "test"))
        
        val snapshot = stats.snapshot()
        assertEquals(1, snapshot.totalRounds)
        assertTrue(snapshot.gameEnded)
        assertEquals(2, snapshot.totalDiceRolls)
        assertEquals(1, snapshot.totalPropertiesPurchased)
        assertEquals(200, snapshot.totalBankPayments)
        assertEquals(1, snapshot.doublesCount[player2])
    }
}
