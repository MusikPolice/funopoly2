@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly.statistics

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Tile
import ca.jonathanfritz.monopoly.card.Card
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.event.GameEvent
import ca.jonathanfritz.monopoly.event.GameEventListener

/**
 * Collects and aggregates statistics from game events during Monopoly gameplay.
 * Implements GameEventListener to observe all game events and maintain comprehensive statistics.
 */
class GameStatistics : GameEventListener {
    // Game lifecycle
    private var gameStarted = false
    private var gameEnded = false
    private var totalRounds = 0
    private var winner: Player? = null
    private var endReason: String = ""

    // Movement & Landing Statistics
    private val tileLandings = mutableMapOf<String, Int>() // Tile class name -> count
    private val playerPositions = mutableMapOf<Player, MutableList<Int>>() // Track position history
    private val goPassings = mutableMapOf<Player, Int>() // Times passed GO

    // Dice Statistics
    private val diceRolls = mutableListOf<DiceRoll>()
    private val doublesRolled = mutableMapOf<Player, Int>()

    // Financial Statistics
    private val bankPayments = mutableListOf<BankPayment>()
    private val bankCharges = mutableListOf<BankCharge>()
    private val rentPayments = mutableListOf<RentTransaction>()
    
    // Property Statistics
    private val propertyPurchases = mutableListOf<PropertyTransaction>()
    private val mortgages = mutableListOf<MortgageTransaction>()
    private val unmortgages = mutableListOf<UnmortgageTransaction>()
    
    // Development Statistics
    private val housePurchases = mutableListOf<HouseTransaction>()
    private val hotelPurchases = mutableListOf<HotelTransaction>()
    private val houseSales = mutableListOf<HouseSaleTransaction>()
    private val hotelSales = mutableListOf<HotelSaleTransaction>()
    
    // Jail Statistics
    private val jailSentEvents = mutableListOf<JailEvent>()
    private val jailReleaseEvents = mutableListOf<JailReleaseEvent>()
    
    // Card Statistics
    private val cardsDrawn = mutableListOf<CardDrawnEvent>()
    
    // Bankruptcy Statistics
    private val bankruptcies = mutableListOf<BankruptcyEvent>()
    
    // Monopoly tracking - map of player to set of color groups they achieved monopoly on
    private val monopoliesAcquired = mutableMapOf<Player, MutableSet<ColourGroup>>()
    
    // Auction Statistics
    private val auctions = mutableListOf<AuctionEvent>()
    private val auctionBids = mutableListOf<AuctionBidEvent>()
    private val auctionDropouts = mutableListOf<AuctionDropoutEvent>()
    
    // Decision Metrics (Strategy-related)
    private val propertiesOfferedToPlayers = mutableMapOf<Player, Int>() // Times landed on unowned property
    private val playerStrategies = mutableMapOf<Player, String>() // Player -> strategy name

    override fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.RoundStarted -> handleRoundStarted(event)
            is GameEvent.RoundEnded -> handleRoundEnded(event)
            is GameEvent.TurnStarted -> handleTurnStarted(event)
            is GameEvent.TurnEnded -> handleTurnEnded(event)
            is GameEvent.DiceRolled -> handleDiceRolled(event)
            is GameEvent.PlayerMoved -> handlePlayerMoved(event)
            is GameEvent.TileLanded -> handleTileLanded(event)
            is GameEvent.BankPaidPlayer -> handleBankPaidPlayer(event)
            is GameEvent.PlayerChargedByBank -> handlePlayerChargedByBank(event)
            is GameEvent.RentPaid -> handleRentPaid(event)
            is GameEvent.PropertyPurchased -> handlePropertyPurchased(event)
            is GameEvent.PropertyMortgaged -> handlePropertyMortgaged(event)
            is GameEvent.PropertyUnmortgaged -> handlePropertyUnmortgaged(event)
            is GameEvent.HousePurchased -> handleHousePurchased(event)
            is GameEvent.HotelPurchased -> handleHotelPurchased(event)
            is GameEvent.HouseSold -> handleHouseSold(event)
            is GameEvent.HotelSold -> handleHotelSold(event)
            is GameEvent.PlayerSentToJail -> handlePlayerSentToJail(event)
            is GameEvent.PlayerLeftJail -> handlePlayerLeftJail(event)
            is GameEvent.CardDrawn -> handleCardDrawn(event)
            is GameEvent.PropertyOffered -> handlePropertyOffered(event)
            is GameEvent.PurchaseDecision -> handlePurchaseDecision(event)
            is GameEvent.AuctionStarted -> handleAuctionStarted(event)
            is GameEvent.AuctionBid -> handleAuctionBid(event)
            is GameEvent.AuctionPlayerDropped -> handleAuctionPlayerDropped(event)
            is GameEvent.AuctionEnded -> handleAuctionEnded(event)
            is GameEvent.PlayerBankrupted -> handlePlayerBankrupted(event)
            is GameEvent.AssetTransferred -> handleAssetTransferred(event)
            is GameEvent.GameEnded -> handleGameEnded(event)
        }
    }

    // Event Handlers
    private fun handleRoundStarted(event: GameEvent.RoundStarted) {
        if (!gameStarted) {
            gameStarted = true
        }
    }

    private fun handleRoundEnded(event: GameEvent.RoundEnded) {
        totalRounds = event.round
    }

    private fun handleTurnStarted(event: GameEvent.TurnStarted) {
        // Can be used for turn-level analysis if needed
    }

    private fun handleTurnEnded(event: GameEvent.TurnEnded) {
        // Can be used for turn-level analysis if needed
    }

    private fun handleDiceRolled(event: GameEvent.DiceRolled) {
        diceRolls.add(DiceRoll(event.player, event.die1, event.die2, event.isDoubles))
        if (event.isDoubles) {
            doublesRolled[event.player] = (doublesRolled[event.player] ?: 0) + 1
        }
    }

    private fun handlePlayerMoved(event: GameEvent.PlayerMoved) {
        playerPositions.getOrPut(event.player) { mutableListOf() }.add(event.to)
        if (event.passedGo) {
            goPassings[event.player] = (goPassings[event.player] ?: 0) + 1
        }
    }

    private fun handleTileLanded(event: GameEvent.TileLanded) {
        // For buyable tiles (properties, railroads, utilities), use the deed name instead of the tile type
        val tileName = when (val tile = event.tile) {
            is Tile.Buyable -> tile.deedClass.simpleName ?: "Unknown"
            else -> tile::class.simpleName ?: "Unknown"
        }
        tileLandings[tileName] = (tileLandings[tileName] ?: 0) + 1
    }
    
    private fun handlePropertyOffered(event: GameEvent.PropertyOffered) {
        propertiesOfferedToPlayers[event.player] = (propertiesOfferedToPlayers[event.player] ?: 0) + 1
        
        // Capture strategy name on first encounter - this is the right place since PropertyOffered
        // is emitted when a player makes a strategic decision
        if (!playerStrategies.containsKey(event.player)) {
            playerStrategies[event.player] = event.player.getStrategyName()
        }
    }
    
    private fun handlePurchaseDecision(event: GameEvent.PurchaseDecision) {
        // This event is emitted after PropertyOffered, so we can use it for future metrics
        // For now, PropertyPurchased event already tracks successful purchases
    }

    private fun handleBankPaidPlayer(event: GameEvent.BankPaidPlayer) {
        bankPayments.add(BankPayment(event.player, event.amount, event.reason))
    }

    private fun handlePlayerChargedByBank(event: GameEvent.PlayerChargedByBank) {
        bankCharges.add(BankCharge(event.player, event.amount, event.reason))
    }

    private fun handleRentPaid(event: GameEvent.RentPaid) {
        rentPayments.add(
            RentTransaction(
                payer = event.payer,
                recipient = event.recipient,
                amount = event.amount,
                property = event.property,
            ),
        )
    }

    private fun handlePropertyPurchased(event: GameEvent.PropertyPurchased) {
        propertyPurchases.add(
            PropertyTransaction(
                player = event.player,
                deed = event.deed,
                price = event.price,
            ),
        )
        
        // Check if this purchase completed a monopoly
        if (event.player.hasMonopoly(event.deed.colourGroup)) {
            monopoliesAcquired.getOrPut(event.player) { mutableSetOf() }.add(event.deed.colourGroup)
        }
    }

    private fun handlePropertyMortgaged(event: GameEvent.PropertyMortgaged) {
        mortgages.add(
            MortgageTransaction(
                player = event.player,
                deed = event.deed,
                mortgageValue = event.mortgageValue,
            ),
        )
    }

    private fun handlePropertyUnmortgaged(event: GameEvent.PropertyUnmortgaged) {
        unmortgages.add(
            UnmortgageTransaction(
                player = event.player,
                deed = event.deed,
                cost = event.cost,
            ),
        )
    }

    private fun handleHousePurchased(event: GameEvent.HousePurchased) {
        housePurchases.add(
            HouseTransaction(
                player = event.player,
                property = event.property,
                houseCount = event.houseCount,
                cost = event.cost,
            ),
        )
    }

    private fun handleHotelPurchased(event: GameEvent.HotelPurchased) {
        hotelPurchases.add(
            HotelTransaction(
                player = event.player,
                property = event.property,
                cost = event.cost,
            ),
        )
    }

    private fun handleHouseSold(event: GameEvent.HouseSold) {
        houseSales.add(
            HouseSaleTransaction(
                player = event.player,
                property = event.property,
                houseCount = event.houseCount,
                proceeds = event.proceeds,
            ),
        )
    }

    private fun handleHotelSold(event: GameEvent.HotelSold) {
        hotelSales.add(
            HotelSaleTransaction(
                player = event.player,
                property = event.property,
                proceeds = event.proceeds,
            ),
        )
    }

    private fun handlePlayerSentToJail(event: GameEvent.PlayerSentToJail) {
        jailSentEvents.add(JailEvent(event.player, event.reason))
    }

    private fun handlePlayerLeftJail(event: GameEvent.PlayerLeftJail) {
        jailReleaseEvents.add(JailReleaseEvent(event.player, event.method))
    }

    private fun handleCardDrawn(event: GameEvent.CardDrawn) {
        cardsDrawn.add(CardDrawnEvent(event.player, event.deck, event.card))
    }

    private fun handlePlayerBankrupted(event: GameEvent.PlayerBankrupted) {
        bankruptcies.add(
            BankruptcyEvent(
                player = event.player,
                creditor = event.creditor,
                round = event.round,
                netWorth = event.netWorth,
            ),
        )
    }

    private fun handleAssetTransferred(event: GameEvent.AssetTransferred) {
        // Asset transfers are tracked as part of bankruptcy events
        // This event provides detailed transfer information but doesn't need separate tracking
        // for the current statistics model
    }

    private fun handleAuctionStarted(event: GameEvent.AuctionStarted) {
        // Track auction initiation - will be completed when AuctionEnded fires
    }

    private fun handleAuctionBid(event: GameEvent.AuctionBid) {
        auctionBids.add(
            AuctionBidEvent(
                player = event.player,
                deed = event.deed,
                bidAmount = event.bidAmount,
                previousBid = event.previousBid,
            ),
        )
    }

    private fun handleAuctionPlayerDropped(event: GameEvent.AuctionPlayerDropped) {
        auctionDropouts.add(
            AuctionDropoutEvent(
                player = event.player,
                deed = event.deed,
                finalBid = event.finalBid,
            ),
        )
    }

    private fun handleAuctionEnded(event: GameEvent.AuctionEnded) {
        auctions.add(
            AuctionEvent(
                deed = event.deed,
                winner = event.winner,
                winningBid = event.winningBid,
                participantCount = event.participantCount,
                totalRounds = event.totalRounds,
            ),
        )
    }

    private fun handleGameEnded(event: GameEvent.GameEnded) {
        gameEnded = true
        winner = event.winner
        totalRounds = event.rounds
        endReason = event.reason
    }

    /**
     * Generates a comprehensive statistics report with derived metrics.
     * This includes aggregations, calculations, and formatted statistics for output.
     */
    fun generateReport(): StatisticsReport {
        val snapshot = snapshot()
        
        // Build auction statistics first (needed for player stats)
        val successfulAuctions = auctions.filter { it.winner != null }
        val failedAuctions = auctions.filter { it.winner == null }
        
        val playerAuctionWins = successfulAuctions
            .groupBy { it.winner?.name ?: "Unknown" }
            .mapValues { it.value.size }
        
        // Build player statistics
        val allPlayers = (rentPayments.map { it.payer } + 
                         rentPayments.map { it.recipient } + 
                         propertyPurchases.map { it.player } +
                         goPassings.keys +
                         doublesRolled.keys +
                         jailSentEvents.map { it.player } +
                         bankruptcies.map { it.player } +
                         propertiesOfferedToPlayers.keys).distinct()
        
        // Calculate final property ownership by tracking purchases and bankruptcy transfers
        val propertyOwnership = mutableMapOf<TitleDeed, Player>()
        val bankruptcyAcquisitions = mutableMapOf<Player, MutableList<TitleDeed>>()
        
        // Start with all purchases
        propertyPurchases.forEach { purchase ->
            propertyOwnership[purchase.deed] = purchase.player
        }
        
        // Apply bankruptcy transfers and track what each player acquired
        bankruptcies.forEach { bankruptcy ->
            // When a player goes bankrupt, all their properties transfer to the creditor
            if (bankruptcy.creditor is Player) {
                val bankruptPlayer = bankruptcy.player
                val creditor = bankruptcy.creditor
                val deedsToTransfer = propertyOwnership.entries
                    .filter { it.value == bankruptPlayer }
                    .map { it.key }
                    .toList()
                deedsToTransfer.forEach { deed ->
                    propertyOwnership[deed] = creditor
                    bankruptcyAcquisitions.getOrPut(creditor) { mutableListOf() }.add(deed)
                }
            }
            // If bankrupt to Bank, properties go to bank (removed from player ownership)
            else {
                val deedsToRemove = propertyOwnership.entries
                    .filter { it.value == bankruptcy.player }
                    .map { it.key }
                    .toList()
                deedsToRemove.forEach { propertyOwnership.remove(it) }
            }
        }
        
        val playerStats = allPlayers.map { player ->
            // Get monopolies that were acquired during the game (not based on final ownership)
            val monopolies = monopoliesAcquired[player]?.toList() ?: emptyList()
            
            // Get auction wins for this player
            val auctionWinsCount = playerAuctionWins[player.name] ?: 0
            val auctionWinsList = successfulAuctions
                .filter { it.winner == player }
                .map { it.deed::class.simpleName ?: "Unknown" }
            
            // Get auction win deed classes to filter them out from direct purchases
            val auctionWinDeedClasses = successfulAuctions
                .filter { it.winner == player }
                .map { it.deed::class }
                .toSet()
            
            // Get list of properties purchased directly (not via auction)
            val allPurchases = propertyPurchases.filter { it.player == player }
            val directPurchases = allPurchases.filter { it.deed::class !in auctionWinDeedClasses }
            val directPurchasesCount = directPurchases.size
            val purchasedList = directPurchases
                .map { it.deed::class.simpleName ?: "Unknown" }
            
            // Get list of properties acquired via bankruptcy
            val bankruptcyList = bankruptcyAcquisitions[player]
                ?.map { it::class.simpleName ?: "Unknown" }
                ?: emptyList()
            
            val propertiesOffered = propertiesOfferedToPlayers[player] ?: 0
            val propertiesPurchasedCount = directPurchasesCount
            // Purchase rate should only consider direct purchases vs offers (not auctions)
            val purchaseRate = if (propertiesOffered > 0) {
                directPurchasesCount.toDouble() / propertiesOffered
            } else {
                0.0
            }
            
            val jailFeePaid = jailReleaseEvents.count { it.player == player && it.method == "paid fee" }
            val jailWaited = jailReleaseEvents.count { it.player == player && it.method == "rolled doubles" }
            
            PlayerStatistics(
                playerName = player.name,
                strategyName = playerStrategies[player] ?: "Unknown",
                totalRentPaid = rentPayments.filter { it.payer == player }.sumOf { it.amount },
                totalRentCollected = rentPayments.filter { it.recipient == player }.sumOf { it.amount },
                propertiesPurchased = propertiesPurchasedCount,
                propertiesPurchasedList = purchasedList,
                propertiesAcquiredViaBankruptcy = bankruptcyList,
                totalPropertySpending = propertyPurchases.filter { it.player == player }.sumOf { it.price },
                housesBuilt = housePurchases.count { it.player == player },
                hotelsBuilt = hotelPurchases.count { it.player == player },
                timesPassedGo = goPassings[player] ?: 0,
                doublesRolled = doublesRolled[player] ?: 0,
                jailVisits = jailSentEvents.count { it.player == player },
                bankruptcyRound = bankruptcies.find { it.player == player }?.round,
                monopoliesAcquired = monopolies,
                propertiesOffered = propertiesOffered,
                purchaseRate = purchaseRate,
                jailFeePaidCount = jailFeePaid,
                jailWaitedCount = jailWaited,
                propertiesWonAtAuction = auctionWinsCount,
                propertiesWonAtAuctionList = auctionWinsList,
            )
        }
        
        // Build property statistics
        val mostExpensiveProperty = propertyPurchases.maxByOrNull { it.price }?.let {
            PropertyInfo(it.deed::class.simpleName ?: "Unknown", it.price)
        }
        
        val propertiesByColorGroup = propertyPurchases
            .groupBy { it.deed.colourGroup }
            .mapValues { it.value.size }
        
        val propStats = PropertyStatistics(
            totalPurchases = propertyPurchases.size,
            totalMortgages = mortgages.size,
            totalUnmortgages = unmortgages.size,
            mostExpensiveProperty = mostExpensiveProperty,
            propertiesByColorGroup = propertiesByColorGroup,
        )
        
        // Build financial summary
        val largestRent = rentPayments.maxByOrNull { it.amount }?.let {
            RentInfo(it.payer.name, it.recipient.name, it.amount, it.property::class.simpleName ?: "Unknown")
        }
        
        val avgRent = if (rentPayments.isNotEmpty()) {
            rentPayments.sumOf { it.amount }.toDouble() / rentPayments.size
        } else {
            0.0
        }
        
        val financialSummary = FinancialSummary(
            totalRentPaid = snapshot.totalRentPaid,
            totalBankPayments = snapshot.totalBankPayments,
            totalBankCharges = snapshot.totalBankCharges,
            largestRentPayment = largestRent,
            averageRentPerTransaction = avgRent,
        )
        
        // Build movement statistics
        val mostLanded = tileLandings.maxByOrNull { it.value }?.let {
            TileInfo(it.key, it.value)
        }
        
        val leastLanded = tileLandings.minByOrNull { it.value }?.let {
            TileInfo(it.key, it.value)
        }
        
        val movementStats = MovementStatistics(
            totalDiceRolls = snapshot.totalDiceRolls,
            averageDiceRoll = snapshot.averageDiceRoll,
            totalDoubles = doublesRolled.values.sum(),
            tileLandingFrequency = snapshot.tileLandings,
            mostLandedTile = mostLanded,
            leastLandedTile = leastLanded,
        )
        
        // Build development statistics
        val mostDeveloped = snapshot.developmentByColorGroup.maxByOrNull { it.value }?.let {
            ColorGroupInfo(it.key, it.value)
        }
        
        val devStats = DevelopmentStatistics(
            totalHousesBuilt = snapshot.totalHousesPurchased,
            totalHotelsBuilt = snapshot.totalHotelsPurchased,
            totalHousesSold = snapshot.totalHousesSold,
            totalHotelsSold = snapshot.totalHotelsSold,
            developmentByColorGroup = snapshot.developmentByColorGroup,
            mostDevelopedColorGroup = mostDeveloped,
        )
        
        // Build remaining auction statistics (successfulAuctions and failedAuctions already calculated above)
        val highestWinningBid = successfulAuctions.maxByOrNull { it.winningBid ?: 0 }?.let {
            AuctionInfo(
                property = it.deed::class.simpleName ?: "Unknown",
                winner = it.winner?.name ?: "Unknown",
                winningBid = it.winningBid ?: 0,
                participantCount = it.participantCount,
            )
        }
        
        val lowestWinningBid = successfulAuctions.minByOrNull { it.winningBid ?: Int.MAX_VALUE }?.let {
            AuctionInfo(
                property = it.deed::class.simpleName ?: "Unknown",
                winner = it.winner?.name ?: "Unknown",
                winningBid = it.winningBid ?: 0,
                participantCount = it.participantCount,
            )
        }
        
        val avgWinningBid = if (successfulAuctions.isNotEmpty()) {
            successfulAuctions.mapNotNull { it.winningBid }.average()
        } else {
            0.0
        }
        
        val avgBidsPerAuction = if (auctions.isNotEmpty()) {
            auctionBids.size.toDouble() / auctions.size
        } else {
            0.0
        }
        
        val playerAuctionParticipation = auctionBids
            .groupBy { it.player.name }
            .mapValues { it.value.size }
        
        val auctionStats = AuctionStatistics(
            totalAuctions = auctions.size,
            successfulAuctions = successfulAuctions.size,
            failedAuctions = failedAuctions.size,
            totalBids = auctionBids.size,
            averageBidsPerAuction = avgBidsPerAuction,
            averageWinningBid = avgWinningBid,
            highestWinningBid = highestWinningBid,
            lowestWinningBid = lowestWinningBid,
            totalAuctionSpending = successfulAuctions.mapNotNull { it.winningBid }.sum(),
            playerAuctionWins = playerAuctionWins,
            playerAuctionParticipation = playerAuctionParticipation,
        )
        
        // Build game summary
        val gameSummary = GameSummary(
            totalRounds = snapshot.totalRounds,
            winner = snapshot.winner?.name,
            endReason = snapshot.endReason,
            totalPlayers = allPlayers.size,
            bankruptcies = snapshot.totalBankruptcies,
        )
        
        return StatisticsReport(
            gameSummary = gameSummary,
            playerStatistics = playerStats,
            propertyStatistics = propStats,
            financialSummary = financialSummary,
            movementStatistics = movementStats,
            developmentStatistics = devStats,
            auctionStatistics = auctionStats,
        )
    }

    /**
     * Creates a snapshot of current statistics for mid-game inspection or final reporting.
     */
    fun snapshot(): StatisticsSnapshot {
        return StatisticsSnapshot(
            // Game Info
            totalRounds = totalRounds,
            gameEnded = gameEnded,
            winner = winner,
            endReason = endReason,
            
            // Movement
            tileLandings = tileLandings.toMap(),
            goPassings = goPassings.toMap(),
            
            // Dice
            totalDiceRolls = diceRolls.size,
            doublesCount = doublesRolled.toMap(),
            averageDiceRoll = if (diceRolls.isNotEmpty()) {
                diceRolls.map { it.die1 + it.die2 }.average()
            } else {
                0.0
            },
            
            // Financial
            totalBankPayments = bankPayments.sumOf { it.amount },
            totalBankCharges = bankCharges.sumOf { it.amount },
            totalRentPaid = rentPayments.sumOf { it.amount },
            rentTransactions = rentPayments.toList(),
            
            // Property
            totalPropertiesPurchased = propertyPurchases.size,
            totalPropertySpending = propertyPurchases.sumOf { it.price },
            propertiesByPlayer = propertyPurchases.groupBy { it.player }.mapValues { it.value.size },
            totalMortgages = mortgages.size,
            totalUnmortgages = unmortgages.size,
            
            // Development
            totalHousesPurchased = housePurchases.size, // Count transactions, not cumulative count
            totalHotelsPurchased = hotelPurchases.size,
            totalHousesSold = houseSales.size,
            totalHotelsSold = hotelSales.size,
            developmentByColorGroup = housePurchases.groupBy { it.property.colourGroup }
                .mapValues { it.value.size }, // Count transactions per color group
            
            // Jail
            totalJailSentences = jailSentEvents.size,
            jailReleasesByMethod = jailReleaseEvents.groupBy { it.method }.mapValues { it.value.size },
            
            // Cards
            totalCardsDrawn = cardsDrawn.size,
            cardsByDeck = cardsDrawn.groupBy { it.deck }.mapValues { it.value.size },
            
            // Bankruptcy
            totalBankruptcies = bankruptcies.size,
            bankruptcyRounds = bankruptcies.map { it.round },
        )
    }

    // Data classes for tracking events
    data class DiceRoll(val player: Player, val die1: Int, val die2: Int, val isDoubles: Boolean)
    data class BankPayment(val player: Player, val amount: Int, val reason: String)
    data class BankCharge(val player: Player, val amount: Int, val reason: String)
    data class RentTransaction(
        val payer: Player,
        val recipient: Player,
        val amount: Int,
        val property: TitleDeed,
    )
    data class PropertyTransaction(val player: Player, val deed: TitleDeed, val price: Int)
    data class MortgageTransaction(val player: Player, val deed: TitleDeed, val mortgageValue: Int)
    data class UnmortgageTransaction(val player: Player, val deed: TitleDeed, val cost: Int)
    data class HouseTransaction(
        val player: Player,
        val property: ca.jonathanfritz.monopoly.deed.Property,
        val houseCount: Int,
        val cost: Int,
    )
    data class HotelTransaction(
        val player: Player,
        val property: ca.jonathanfritz.monopoly.deed.Property,
        val cost: Int,
    )
    data class HouseSaleTransaction(
        val player: Player,
        val property: ca.jonathanfritz.monopoly.deed.Property,
        val houseCount: Int,
        val proceeds: Int,
    )
    data class HotelSaleTransaction(
        val player: Player,
        val property: ca.jonathanfritz.monopoly.deed.Property,
        val proceeds: Int,
    )
    data class JailEvent(val player: Player, val reason: String)
    data class JailReleaseEvent(val player: Player, val method: String)
    data class CardDrawnEvent(val player: Player, val deck: String, val card: Card)
    data class BankruptcyEvent(val player: Player, val creditor: Any, val round: Int, val netWorth: Int)
    data class AuctionEvent(val deed: TitleDeed, val winner: Player?, val winningBid: Int?, val participantCount: Int, val totalRounds: Int)
    data class AuctionBidEvent(val player: Player, val deed: TitleDeed, val bidAmount: Int, val previousBid: Int)
    data class AuctionDropoutEvent(val player: Player, val deed: TitleDeed, val finalBid: Int)
}

/**
 * Immutable snapshot of game statistics at a point in time.
 * Can be generated mid-game or at game end for reporting.
 */
data class StatisticsSnapshot(
    // Game Info
    val totalRounds: Int,
    val gameEnded: Boolean,
    val winner: Player?,
    val endReason: String,
    
    // Movement
    val tileLandings: Map<String, Int>,
    val goPassings: Map<Player, Int>,
    
    // Dice
    val totalDiceRolls: Int,
    val doublesCount: Map<Player, Int>,
    val averageDiceRoll: Double,
    
    // Financial
    val totalBankPayments: Int,
    val totalBankCharges: Int,
    val totalRentPaid: Int,
    val rentTransactions: List<GameStatistics.RentTransaction>,
    
    // Property
    val totalPropertiesPurchased: Int,
    val totalPropertySpending: Int,
    val propertiesByPlayer: Map<Player, Int>,
    val totalMortgages: Int,
    val totalUnmortgages: Int,
    
    // Development
    val totalHousesPurchased: Int,
    val totalHotelsPurchased: Int,
    val totalHousesSold: Int,
    val totalHotelsSold: Int,
    val developmentByColorGroup: Map<ColourGroup, Int>,
    
    // Jail
    val totalJailSentences: Int,
    val jailReleasesByMethod: Map<String, Int>,
    
    // Cards
    val totalCardsDrawn: Int,
    val cardsByDeck: Map<String, Int>,
    
    // Bankruptcy
    val totalBankruptcies: Int,
    val bankruptcyRounds: List<Int>,
)
