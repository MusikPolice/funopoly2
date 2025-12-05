package ca.jonathanfritz.monopoly.statistics

/**
 * Formats statistics reports for different output formats (console, JSON, etc.)
 */
object StatisticsFormatter {
    /**
     * Formats a statistics report as human-readable console output.
     */
    fun formatConsole(report: StatisticsReport): String =
        buildString {
            appendLine()
            appendLine("═".repeat(80))
            appendLine("GAME STATISTICS REPORT".padStart(50))
            appendLine("═".repeat(80))
            appendLine()

            // Game Summary
            appendLine("┌─ GAME SUMMARY " + "─".repeat(63))
            appendLine("│ Total Rounds: ${report.gameSummary.totalRounds}")
            appendLine("│ Winner: ${report.gameSummary.winner ?: "None"}")
            appendLine("│ End Reason: ${report.gameSummary.endReason}")
            appendLine("│ Total Players: ${report.gameSummary.totalPlayers}")
            appendLine("│ Bankruptcies: ${report.gameSummary.bankruptcies}")
            appendLine("└" + "─".repeat(79))
            appendLine()

            // Player Statistics
            appendLine("┌─ PLAYER STATISTICS " + "─".repeat(58))
            report.playerStatistics.sortedByDescending { it.totalRentCollected }.forEach { player ->
                appendLine("│")
                appendLine("│ Player: ${player.playerName}")
                appendLine("│   Strategy: ${player.strategyName}")
                appendLine("│   Rent Collected: \$${player.totalRentCollected}")
                appendLine("│   Rent Paid: \$${player.totalRentPaid}")
                appendLine("│   Net Rent: \$${player.totalRentCollected - player.totalRentPaid}")
                appendLine("│   Properties Offered: ${player.propertiesOffered}")
                if (player.propertiesPurchasedList.isNotEmpty()) {
                    val propList = player.propertiesPurchasedList.joinToString(", ")
                    appendLine("│   Properties Purchased: ${player.propertiesPurchased} ($propList)")
                } else {
                    appendLine("│   Properties Purchased: ${player.propertiesPurchased}")
                }
                appendLine("│   Purchase Rate: ${"%.1f".format(player.purchaseRate * 100)}%")
                if (player.propertiesWonAtAuction > 0) {
                    if (player.propertiesWonAtAuctionList.isNotEmpty()) {
                        val auctionList = player.propertiesWonAtAuctionList.joinToString(", ")
                        appendLine("│   Properties Won at Auction: ${player.propertiesWonAtAuction} ($auctionList)")
                    } else {
                        appendLine("│   Properties Won at Auction: ${player.propertiesWonAtAuction}")
                    }
                }
                if (player.propertiesAcquiredViaBankruptcy.isNotEmpty()) {
                    val bankruptcyList = player.propertiesAcquiredViaBankruptcy.joinToString(", ")
                    appendLine("│   Properties Acquired via Bankruptcy: ${player.propertiesAcquiredViaBankruptcy.size} ($bankruptcyList)")
                }
                if (player.monopoliesAcquired.isNotEmpty()) {
                    val colorNames = player.monopoliesAcquired.joinToString(", ") { it.name }
                    appendLine("│   Monopolies Acquired: ${player.monopoliesAcquired.size} ($colorNames)")
                }
                appendLine("│   Property Spending: \$${player.totalPropertySpending}")
                appendLine("│   Houses Built: ${player.housesBuilt}")
                appendLine("│   Hotels Built: ${player.hotelsBuilt}")
                appendLine("│   Passed GO: ${player.timesPassedGo} times")
                appendLine("│   Doubles Rolled: ${player.doublesRolled}")
                appendLine("│   Jail Visits: ${player.jailVisits}")
                appendLine("│   Jail Fee Paid: ${player.jailFeePaidCount} times")
                appendLine("│   Jail Waited: ${player.jailWaitedCount} times")
                if (player.bankruptcyRound != null) {
                    appendLine("│   Bankrupted: Round ${player.bankruptcyRound}")
                }
            }
            appendLine("└" + "─".repeat(79))
            appendLine()

            // Financial Summary
            appendLine("┌─ FINANCIAL SUMMARY " + "─".repeat(58))
            appendLine("│ Total Rent Paid: \$${report.financialSummary.totalRentPaid}")
            appendLine("│ Total Bank Payments: \$${report.financialSummary.totalBankPayments}")
            appendLine("│ Total Bank Charges: \$${report.financialSummary.totalBankCharges}")
            appendLine("│ Average Rent/Transaction: \$%.2f".format(report.financialSummary.averageRentPerTransaction))
            if (report.financialSummary.largestRentPayment != null) {
                val rent = report.financialSummary.largestRentPayment
                appendLine("│ Largest Rent Payment:")
                appendLine("│   \$${rent.amount} from ${rent.payer} to ${rent.recipient}")
                appendLine("│   Property: ${rent.property}")
            }
            appendLine("└" + "─".repeat(79))
            appendLine()

            // Property Statistics
            appendLine("┌─ PROPERTY STATISTICS " + "─".repeat(56))
            appendLine("│ Total Purchases: ${report.propertyStatistics.totalPurchases}")
            appendLine("│ Total Mortgages: ${report.propertyStatistics.totalMortgages}")
            appendLine("│ Total Unmortgages: ${report.propertyStatistics.totalUnmortgages}")
            if (report.propertyStatistics.mostExpensiveProperty != null) {
                val prop = report.propertyStatistics.mostExpensiveProperty
                appendLine("│ Most Expensive Purchase: ${prop.name} (\$${prop.price})")
            }
            if (report.propertyStatistics.propertiesByColorGroup.isNotEmpty()) {
                appendLine("│ Purchases by Color Group:")
                report.propertyStatistics.propertiesByColorGroup
                    .toList()
                    .sortedByDescending { it.second }
                    .forEach { (colorGroup, count) ->
                        appendLine("│   ${colorGroup.name}: $count")
                    }
            }
            appendLine("└" + "─".repeat(79))
            appendLine()

            // Movement Statistics
            appendLine("┌─ MOVEMENT STATISTICS " + "─".repeat(56))
            appendLine("│ Total Dice Rolls: ${report.movementStatistics.totalDiceRolls}")
            appendLine("│ Average Dice Roll: %.2f".format(report.movementStatistics.averageDiceRoll))
            appendLine("│ Total Doubles: ${report.movementStatistics.totalDoubles}")
            if (report.movementStatistics.mostLandedTile != null) {
                val tile = report.movementStatistics.mostLandedTile
                appendLine("│ Most Landed Tile: ${tile.name} (${tile.landingCount} times)")
            }
            if (report.movementStatistics.leastLandedTile != null) {
                val tile = report.movementStatistics.leastLandedTile
                appendLine("│ Least Landed Tile: ${tile.name} (${tile.landingCount} times)")
            }
            appendLine("└" + "─".repeat(79))
            appendLine()

            // Development Statistics
            appendLine("┌─ DEVELOPMENT STATISTICS " + "─".repeat(53))
            appendLine("│ Total Houses Built: ${report.developmentStatistics.totalHousesBuilt}")
            appendLine("│ Total Hotels Built: ${report.developmentStatistics.totalHotelsBuilt}")
            appendLine("│ Total Houses Sold: ${report.developmentStatistics.totalHousesSold}")
            appendLine("│ Total Hotels Sold: ${report.developmentStatistics.totalHotelsSold}")
            if (report.developmentStatistics.mostDevelopedColorGroup != null) {
                val dev = report.developmentStatistics.mostDevelopedColorGroup
                appendLine("│ Most Developed: ${dev.colorGroup.name} (${dev.developmentCount} transactions)")
            }
            if (report.developmentStatistics.developmentByColorGroup.isNotEmpty()) {
                appendLine("│ Development by Color Group:")
                report.developmentStatistics.developmentByColorGroup
                    .toList()
                    .sortedByDescending { it.second }
                    .forEach { (colorGroup, count) ->
                        appendLine("│   ${colorGroup.name}: $count transactions")
                    }
            }
            appendLine("└" + "─".repeat(79))
            appendLine()

            // Auction Statistics
            appendLine("┌─ AUCTION STATISTICS " + "─".repeat(57))
            appendLine("│ Total Auctions: ${report.auctionStatistics.totalAuctions}")
            appendLine("│ Successful Auctions: ${report.auctionStatistics.successfulAuctions}")
            appendLine("│ Failed Auctions: ${report.auctionStatistics.failedAuctions}")
            appendLine("│ Total Bids: ${report.auctionStatistics.totalBids}")
            appendLine("│ Average Bids/Auction: %.2f".format(report.auctionStatistics.averageBidsPerAuction))
            appendLine("│ Total Auction Spending: \$${report.auctionStatistics.totalAuctionSpending}")
            if (report.auctionStatistics.successfulAuctions > 0) {
                appendLine("│ Average Winning Bid: \$%.2f".format(report.auctionStatistics.averageWinningBid))
            }
            if (report.auctionStatistics.highestWinningBid != null) {
                val auction = report.auctionStatistics.highestWinningBid
                appendLine("│ Highest Winning Bid:")
                appendLine("│   ${auction.property}: \$${auction.winningBid} (${auction.winner})")
            }
            if (report.auctionStatistics.lowestWinningBid != null) {
                val auction = report.auctionStatistics.lowestWinningBid
                appendLine("│ Lowest Winning Bid:")
                appendLine("│   ${auction.property}: \$${auction.winningBid} (${auction.winner})")
            }
            if (report.auctionStatistics.playerAuctionWins.isNotEmpty()) {
                appendLine("│ Auction Wins by Player:")
                report.auctionStatistics.playerAuctionWins
                    .toList()
                    .sortedByDescending { it.second }
                    .forEach { (player, wins) ->
                        appendLine("│   $player: $wins")
                    }
            }
            if (report.auctionStatistics.playerAuctionParticipation.isNotEmpty()) {
                appendLine("│ Auction Participation (Bids) by Player:")
                report.auctionStatistics.playerAuctionParticipation
                    .toList()
                    .sortedByDescending { it.second }
                    .forEach { (player, bids) ->
                        appendLine("│   $player: $bids bids")
                    }
            }
            appendLine("└" + "─".repeat(79))
            appendLine()

            appendLine("═".repeat(80))
        }

    /**
     * Formats a statistics report as JSON.
     */
    fun formatJson(report: StatisticsReport): String {
        // Simple JSON formatting without external dependencies
        return buildString {
            appendLine("{")
            appendLine("  \"gameSummary\": {")
            appendLine("    \"totalRounds\": ${report.gameSummary.totalRounds},")
            appendLine("    \"winner\": ${formatJsonString(report.gameSummary.winner)},")
            appendLine("    \"endReason\": ${formatJsonString(report.gameSummary.endReason)},")
            appendLine("    \"totalPlayers\": ${report.gameSummary.totalPlayers},")
            appendLine("    \"bankruptcies\": ${report.gameSummary.bankruptcies}")
            appendLine("  },")

            appendLine("  \"playerStatistics\": [")
            report.playerStatistics.forEachIndexed { index, player ->
                appendLine("    {")
                appendLine("      \"playerName\": ${formatJsonString(player.playerName)},")
                appendLine("      \"strategyName\": ${formatJsonString(player.strategyName)},")
                appendLine("      \"totalRentPaid\": ${player.totalRentPaid},")
                appendLine("      \"totalRentCollected\": ${player.totalRentCollected},")
                appendLine("      \"propertiesPurchased\": ${player.propertiesPurchased},")
                append("      \"propertiesPurchasedList\": [")
                append(player.propertiesPurchasedList.joinToString(", ") { "\"$it\"" })
                appendLine("],")
                appendLine("      \"propertiesOffered\": ${player.propertiesOffered},")
                appendLine("      \"purchaseRate\": ${player.purchaseRate},")
                appendLine("      \"propertiesWonAtAuction\": ${player.propertiesWonAtAuction},")
                append("      \"propertiesWonAtAuctionList\": [")
                append(player.propertiesWonAtAuctionList.joinToString(", ") { "\"$it\"" })
                appendLine("],")
                append("      \"propertiesAcquiredViaBankruptcy\": [")
                append(player.propertiesAcquiredViaBankruptcy.joinToString(", ") { "\"$it\"" })
                appendLine("],")
                appendLine("      \"totalPropertySpending\": ${player.totalPropertySpending},")
                appendLine("      \"housesBuilt\": ${player.housesBuilt},")
                appendLine("      \"hotelsBuilt\": ${player.hotelsBuilt},")
                appendLine("      \"timesPassedGo\": ${player.timesPassedGo},")
                appendLine("      \"doublesRolled\": ${player.doublesRolled},")
                appendLine("      \"jailVisits\": ${player.jailVisits},")
                appendLine("      \"jailFeePaidCount\": ${player.jailFeePaidCount},")
                appendLine("      \"jailWaitedCount\": ${player.jailWaitedCount},")
                appendLine("      \"bankruptcyRound\": ${player.bankruptcyRound},")
                append("      \"monopoliesAcquired\": [")
                append(player.monopoliesAcquired.joinToString(", ") { "\"${it.name}\"" })
                appendLine("]")
                append("    }")
                if (index < report.playerStatistics.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ],")

            appendLine("  \"financialSummary\": {")
            appendLine("    \"totalRentPaid\": ${report.financialSummary.totalRentPaid},")
            appendLine("    \"totalBankPayments\": ${report.financialSummary.totalBankPayments},")
            appendLine("    \"totalBankCharges\": ${report.financialSummary.totalBankCharges},")
            appendLine("    \"averageRentPerTransaction\": ${report.financialSummary.averageRentPerTransaction},")
            append("    \"largestRentPayment\": ")
            if (report.financialSummary.largestRentPayment != null) {
                val rent = report.financialSummary.largestRentPayment
                appendLine("{")
                appendLine("      \"payer\": ${formatJsonString(rent.payer)},")
                appendLine("      \"recipient\": ${formatJsonString(rent.recipient)},")
                appendLine("      \"amount\": ${rent.amount},")
                appendLine("      \"property\": ${formatJsonString(rent.property)}")
                appendLine("    }")
            } else {
                appendLine("null")
            }
            appendLine("  },")

            appendLine("  \"propertyStatistics\": {")
            appendLine("    \"totalPurchases\": ${report.propertyStatistics.totalPurchases},")
            appendLine("    \"totalMortgages\": ${report.propertyStatistics.totalMortgages},")
            appendLine("    \"totalUnmortgages\": ${report.propertyStatistics.totalUnmortgages}")
            appendLine("  },")

            appendLine("  \"movementStatistics\": {")
            appendLine("    \"totalDiceRolls\": ${report.movementStatistics.totalDiceRolls},")
            appendLine("    \"averageDiceRoll\": ${report.movementStatistics.averageDiceRoll},")
            appendLine("    \"totalDoubles\": ${report.movementStatistics.totalDoubles}")
            appendLine("  },")

            appendLine("  \"developmentStatistics\": {")
            appendLine("    \"totalHousesBuilt\": ${report.developmentStatistics.totalHousesBuilt},")
            appendLine("    \"totalHotelsBuilt\": ${report.developmentStatistics.totalHotelsBuilt},")
            appendLine("    \"totalHousesSold\": ${report.developmentStatistics.totalHousesSold},")
            appendLine("    \"totalHotelsSold\": ${report.developmentStatistics.totalHotelsSold}")
            appendLine("  },")

            appendLine("  \"auctionStatistics\": {")
            appendLine("    \"totalAuctions\": ${report.auctionStatistics.totalAuctions},")
            appendLine("    \"successfulAuctions\": ${report.auctionStatistics.successfulAuctions},")
            appendLine("    \"failedAuctions\": ${report.auctionStatistics.failedAuctions},")
            appendLine("    \"totalBids\": ${report.auctionStatistics.totalBids},")
            appendLine("    \"averageBidsPerAuction\": ${report.auctionStatistics.averageBidsPerAuction},")
            appendLine("    \"averageWinningBid\": ${report.auctionStatistics.averageWinningBid},")
            appendLine("    \"totalAuctionSpending\": ${report.auctionStatistics.totalAuctionSpending},")
            append("    \"highestWinningBid\": ")
            if (report.auctionStatistics.highestWinningBid != null) {
                val auction = report.auctionStatistics.highestWinningBid
                appendLine("{")
                appendLine("      \"property\": ${formatJsonString(auction.property)},")
                appendLine("      \"winner\": ${formatJsonString(auction.winner)},")
                appendLine("      \"winningBid\": ${auction.winningBid},")
                appendLine("      \"participantCount\": ${auction.participantCount}")
                appendLine("    },")
            } else {
                appendLine("null,")
            }
            append("    \"lowestWinningBid\": ")
            if (report.auctionStatistics.lowestWinningBid != null) {
                val auction = report.auctionStatistics.lowestWinningBid
                appendLine("{")
                appendLine("      \"property\": ${formatJsonString(auction.property)},")
                appendLine("      \"winner\": ${formatJsonString(auction.winner)},")
                appendLine("      \"winningBid\": ${auction.winningBid},")
                appendLine("      \"participantCount\": ${auction.participantCount}")
                appendLine("    },")
            } else {
                appendLine("null,")
            }
            appendLine("    \"playerAuctionWins\": {")
            report.auctionStatistics.playerAuctionWins.entries.forEachIndexed { index, (player, wins) ->
                append("      ${formatJsonString(player)}: $wins")
                if (index < report.auctionStatistics.playerAuctionWins.size - 1) appendLine(",") else appendLine()
            }
            appendLine("    },")
            appendLine("    \"playerAuctionParticipation\": {")
            report.auctionStatistics.playerAuctionParticipation.entries.forEachIndexed { index, (player, bids) ->
                append("      ${formatJsonString(player)}: $bids")
                if (index < report.auctionStatistics.playerAuctionParticipation.size - 1) appendLine(",") else appendLine()
            }
            appendLine("    }")
            appendLine("  }")

            appendLine("}")
        }
    }

    private fun formatJsonString(value: String?): String =
        if (value == null) {
            "null"
        } else {
            "\"${value.replace("\"", "\\\"")}\""
        }
}
