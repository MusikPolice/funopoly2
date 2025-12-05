package ca.jonathanfritz.monopoly.statistics

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class StatisticsFormatterTest {
    
    @Test
    fun `formatConsole produces valid output`() {
        val report = createSampleReport()
        
        val output = StatisticsFormatter.formatConsole(report)
        
        // Verify key sections are present
        assertTrue(output.contains("GAME STATISTICS REPORT"))
        assertTrue(output.contains("GAME SUMMARY"))
        assertTrue(output.contains("PLAYER STATISTICS"))
        assertTrue(output.contains("FINANCIAL SUMMARY"))
        assertTrue(output.contains("PROPERTY STATISTICS"))
        assertTrue(output.contains("MOVEMENT STATISTICS"))
        assertTrue(output.contains("DEVELOPMENT STATISTICS"))
        assertTrue(output.contains("AUCTION STATISTICS"))
        
        // Verify data is included
        assertTrue(output.contains("Winner: Player1"))
        assertTrue(output.contains("Total Rounds: 10"))
        assertTrue(output.contains("Player: Player1"))
        assertTrue(output.contains("Player: Player2"))
    }
    
    @Test
    fun `formatJson produces valid JSON structure`() {
        val report = createSampleReport()
        
        val output = StatisticsFormatter.formatJson(report)
        
        // Verify JSON structure
        assertTrue(output.startsWith("{"))
        assertTrue(output.endsWith("}\n"))
        assertTrue(output.contains("\"gameSummary\":"))
        assertTrue(output.contains("\"playerStatistics\":"))
        assertTrue(output.contains("\"financialSummary\":"))
        assertTrue(output.contains("\"propertyStatistics\":"))
        assertTrue(output.contains("\"movementStatistics\":"))
        assertTrue(output.contains("\"developmentStatistics\":"))
        assertTrue(output.contains("\"auctionStatistics\":"))
        
        // Verify data is included
        assertTrue(output.contains("\"winner\": \"Player1\""))
        assertTrue(output.contains("\"totalRounds\": 10"))
        assertTrue(output.contains("\"playerName\": \"Player1\""))
    }
    
    @Test
    fun `formatConsole handles empty report`() {
        val report = StatisticsReport(
            gameSummary = GameSummary(0, null, "", 0, 0),
            playerStatistics = emptyList(),
            propertyStatistics = PropertyStatistics(0, 0, 0, null, emptyMap()),
            financialSummary = FinancialSummary(0, 0, 0, null, 0.0),
            movementStatistics = MovementStatistics(0, 0.0, 0, emptyMap(), null, null),
            developmentStatistics = DevelopmentStatistics(0, 0, 0, 0, emptyMap(), null),
            auctionStatistics = AuctionStatistics(0, 0, 0, 0, 0.0, 0.0, null, null, 0, emptyMap(), emptyMap()),
        )
        
        val output = StatisticsFormatter.formatConsole(report)
        
        assertTrue(output.contains("GAME STATISTICS REPORT"))
        assertTrue(output.contains("Winner: None"))
        assertTrue(output.contains("AUCTION STATISTICS"))
        assertTrue(output.contains("Total Auctions: 0"))
    }
    
    @Test
    fun `formatJson handles null values correctly`() {
        val report = StatisticsReport(
            gameSummary = GameSummary(5, null, "test", 2, 0),
            playerStatistics = listOf(
                PlayerStatistics("Player1", "TestStrategy", 100, 200, 3, emptyList(), emptyList(), 500, 2, 1, 4, 3, 2, null, emptyList(), 5, 0.6, 1, 1, 0, emptyList())
            ),
            propertyStatistics = PropertyStatistics(3, 1, 0, null, emptyMap()),
            financialSummary = FinancialSummary(300, 400, 200, null, 50.0),
            movementStatistics = MovementStatistics(10, 7.0, 2, emptyMap(), null, null),
            developmentStatistics = DevelopmentStatistics(2, 1, 0, 0, emptyMap(), null),
            auctionStatistics = AuctionStatistics(0, 0, 0, 0, 0.0, 0.0, null, null, 0, emptyMap(), emptyMap()),
        )
        
        val output = StatisticsFormatter.formatJson(report)
        
        assertTrue(output.contains("\"winner\": null"))
        assertTrue(output.contains("\"bankruptcyRound\": null"))
        assertTrue(output.contains("\"largestRentPayment\": null"))
        assertTrue(output.contains("\"auctionStatistics\":"))
        assertTrue(output.contains("\"totalAuctions\": 0"))
    }
    
    private fun createSampleReport(): StatisticsReport {
        val player1Stats = PlayerStatistics(
            playerName = "Player1",
            strategyName = "DefaultStrategy",
            totalRentPaid = 100,
            totalRentCollected = 200,
            propertiesPurchased = 3,
            propertiesPurchasedList = listOf("Boardwalk", "ParkPlace", "MarvinGardens"),
            propertiesAcquiredViaBankruptcy = emptyList(),
            totalPropertySpending = 1000,
            housesBuilt = 2,
            hotelsBuilt = 1,
            timesPassedGo = 4,
            doublesRolled = 3,
            jailVisits = 2,
            bankruptcyRound = null,
            monopoliesAcquired = listOf(ColourGroup.DarkBlue),
            propertiesOffered = 5,
            purchaseRate = 0.6,
            jailFeePaidCount = 1,
            jailWaitedCount = 1,
            propertiesWonAtAuction = 0,
            propertiesWonAtAuctionList = emptyList(),
        )
        
        val player2Stats = PlayerStatistics(
            playerName = "Player2",
            strategyName = "SlumlordStrategy (Oscar)",
            totalRentPaid = 200,
            totalRentCollected = 100,
            propertiesPurchased = 2,
            propertiesPurchasedList = listOf("NewYorkAvenue", "TennesseeAvenue"),
            propertiesAcquiredViaBankruptcy = emptyList(),
            totalPropertySpending = 300,
            housesBuilt = 1,
            hotelsBuilt = 0,
            timesPassedGo = 3,
            doublesRolled = 2,
            jailVisits = 1,
            bankruptcyRound = 10,
            monopoliesAcquired = emptyList(),
            propertiesOffered = 4,
            purchaseRate = 0.5,
            jailFeePaidCount = 0,
            jailWaitedCount = 1,
            propertiesWonAtAuction = 0,
            propertiesWonAtAuctionList = emptyList(),
        )
        
        return StatisticsReport(
            gameSummary = GameSummary(
                totalRounds = 10,
                winner = "Player1",
                endReason = "bankruptcy",
                totalPlayers = 2,
                bankruptcies = 1,
            ),
            playerStatistics = listOf(player1Stats, player2Stats),
            propertyStatistics = PropertyStatistics(
                totalPurchases = 5,
                totalMortgages = 2,
                totalUnmortgages = 1,
                mostExpensiveProperty = PropertyInfo("Boardwalk", 400),
                propertiesByColorGroup = mapOf(ColourGroup.DarkBlue to 2, ColourGroup.Red to 3),
            ),
            financialSummary = FinancialSummary(
                totalRentPaid = 300,
                totalBankPayments = 2000,
                totalBankCharges = 500,
                largestRentPayment = RentInfo("Player2", "Player1", 150, "Boardwalk"),
                averageRentPerTransaction = 50.0,
            ),
            movementStatistics = MovementStatistics(
                totalDiceRolls = 20,
                averageDiceRoll = 7.0,
                totalDoubles = 5,
                tileLandingFrequency = mapOf("Go" to 4, "Boardwalk" to 2),
                mostLandedTile = TileInfo("Go", 4),
                leastLandedTile = TileInfo("Boardwalk", 2),
            ),
            developmentStatistics = DevelopmentStatistics(
                totalHousesBuilt = 3,
                totalHotelsBuilt = 1,
                totalHousesSold = 0,
                totalHotelsSold = 0,
                developmentByColorGroup = mapOf(ColourGroup.DarkBlue to 2, ColourGroup.Red to 2),
                mostDevelopedColorGroup = ColorGroupInfo(ColourGroup.DarkBlue, 2),
            ),
            auctionStatistics = AuctionStatistics(
                totalAuctions = 3,
                successfulAuctions = 2,
                failedAuctions = 1,
                totalBids = 8,
                averageBidsPerAuction = 2.67,
                averageWinningBid = 125.0,
                highestWinningBid = AuctionInfo("Boardwalk", "Player1", 200, 2),
                lowestWinningBid = AuctionInfo("BalticAvenue", "Player2", 50, 2),
                totalAuctionSpending = 250,
                playerAuctionWins = mapOf("Player1" to 1, "Player2" to 1),
                playerAuctionParticipation = mapOf("Player1" to 5, "Player2" to 3),
            ),
        )
    }
}
