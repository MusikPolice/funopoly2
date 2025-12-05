package ca.jonathanfritz.monopoly.statistics

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Comprehensive statistics report generated from game data.
 * Contains both raw statistics and derived metrics.
 */
data class StatisticsReport(
    val gameSummary: GameSummary,
    val playerStatistics: List<PlayerStatistics>,
    val propertyStatistics: PropertyStatistics,
    val financialSummary: FinancialSummary,
    val movementStatistics: MovementStatistics,
    val developmentStatistics: DevelopmentStatistics,
    val auctionStatistics: AuctionStatistics,
)

data class GameSummary(
    val totalRounds: Int,
    val winner: String?,
    val endReason: String,
    val totalPlayers: Int,
    val bankruptcies: Int,
)

data class PlayerStatistics(
    val playerName: String,
    val strategyName: String,
    val totalRentPaid: Int,
    val totalRentCollected: Int,
    val propertiesPurchased: Int,
    val propertiesPurchasedList: List<String>,
    val propertiesAcquiredViaBankruptcy: List<String>,
    val totalPropertySpending: Int,
    val housesBuilt: Int,
    val hotelsBuilt: Int,
    val timesPassedGo: Int,
    val doublesRolled: Int,
    val jailVisits: Int,
    val bankruptcyRound: Int?,
    val monopoliesAcquired: List<ColourGroup>,
    val propertiesOffered: Int,
    val purchaseRate: Double,
    val jailFeePaidCount: Int,
    val jailWaitedCount: Int,
    val propertiesWonAtAuction: Int,
    val propertiesWonAtAuctionList: List<String>,
)

data class PropertyStatistics(
    val totalPurchases: Int,
    val totalMortgages: Int,
    val totalUnmortgages: Int,
    val mostExpensiveProperty: PropertyInfo?,
    val propertiesByColorGroup: Map<ColourGroup, Int>,
)

data class PropertyInfo(
    val name: String,
    val price: Int,
)

data class FinancialSummary(
    val totalRentPaid: Int,
    val totalBankPayments: Int,
    val totalBankCharges: Int,
    val largestRentPayment: RentInfo?,
    val averageRentPerTransaction: Double,
)

data class RentInfo(
    val payer: String,
    val recipient: String,
    val amount: Int,
    val property: String,
)

data class MovementStatistics(
    val totalDiceRolls: Int,
    val averageDiceRoll: Double,
    val totalDoubles: Int,
    val tileLandingFrequency: Map<String, Int>,
    val mostLandedTile: TileInfo?,
    val leastLandedTile: TileInfo?,
)

data class TileInfo(
    val name: String,
    val landingCount: Int,
)

data class DevelopmentStatistics(
    val totalHousesBuilt: Int,
    val totalHotelsBuilt: Int,
    val totalHousesSold: Int,
    val totalHotelsSold: Int,
    val developmentByColorGroup: Map<ColourGroup, Int>,
    val mostDevelopedColorGroup: ColorGroupInfo?,
)

data class ColorGroupInfo(
    val colorGroup: ColourGroup,
    val developmentCount: Int,
)

data class AuctionStatistics(
    val totalAuctions: Int,
    val successfulAuctions: Int,
    val failedAuctions: Int,
    val totalBids: Int,
    val averageBidsPerAuction: Double,
    val averageWinningBid: Double,
    val highestWinningBid: AuctionInfo?,
    val lowestWinningBid: AuctionInfo?,
    val totalAuctionSpending: Int,
    val playerAuctionWins: Map<String, Int>,
    val playerAuctionParticipation: Map<String, Int>,
)

data class AuctionInfo(
    val property: String,
    val winner: String,
    val winningBid: Int,
    val participantCount: Int,
)
