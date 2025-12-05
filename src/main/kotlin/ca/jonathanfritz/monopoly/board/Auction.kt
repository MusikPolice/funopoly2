package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.event.GameEvent
import ca.jonathanfritz.monopoly.exception.BankruptcyException

class Auction(
    private val deed: TitleDeed,
    participants: List<Player>,
    private val bank: Bank,
    private val board: Board,
    private val eventBus: EventBus?,
) {
    private val filteredParticipants: List<Player> = participants.filter { !it.isBankrupt() }
    private var currentBid: Int = board.config.auctionStartingBid
    private var currentWinner: Player? = null
    private val activeBidders: MutableSet<Player> = filteredParticipants.toMutableSet()
    private var roundNumber: Int = 0

    fun conduct(): Player? {
        eventBus?.emit(GameEvent.AuctionStarted(deed, filteredParticipants, board.config.auctionStartingBid))
        println("\n\tAUCTION: ${deed::class.simpleName} (List Price: $${deed.price})")
        println("\tStarting Bid: $${board.config.auctionStartingBid}")
        println("\tParticipants: ${filteredParticipants.joinToString(", ") { it.name }}")

        while (shouldContinueAuction()) {
            roundNumber++
            println("\n\tRound $roundNumber: Current bid $$currentBid, ${activeBidders.size} bidder(s) remaining")

            val bids = collectBids()
            processRound(bids)

            if (roundNumber > board.config.auctionMaxRounds) {
                println("\tWARNING: Auction exceeded maximum rounds")
                break
            }
        }

        return finalizeAuction()
    }

    private fun shouldContinueAuction(): Boolean {
        if (activeBidders.isEmpty()) return false
        if (activeBidders.size == 1 && currentWinner in activeBidders) return false
        return true
    }

    private fun collectBids(): Map<Player, Int?> {
        val minimumBid = currentBid + board.config.auctionMinimumIncrement
        return activeBidders.associateWith { it -> it.calculateBidIncrease(deed, currentBid, minimumBid, bank, board) }
    }

    private fun removeActiveBidder(player: Player) {
        activeBidders.remove(player)
        eventBus?.emit(GameEvent.AuctionPlayerDropped(player, deed, currentBid))
        println("\t\t${player.name} drops out at $$currentBid")
    }

    private fun processRound(bids: Map<Player, Int?>) {
        val roundStartBid = currentBid
        var highestBidThisRound = currentBid
        var highestBidderThisRound: Player? = null

        for ((player, bidAmount) in bids) {
            if (bidAmount == null) {
                removeActiveBidder(player)
            } else if (bidAmount > roundStartBid) {
                if (bidAmount < roundStartBid + board.config.auctionMinimumIncrement) {
                    println(
                        "\t\tWARNING: ${player.name} bid $$bidAmount is not at least $$${board.config.auctionMinimumIncrement} higher than current $$roundStartBid",
                    )
                    removeActiveBidder(player)
                } else if (bidAmount > highestBidThisRound) {
                    highestBidThisRound = bidAmount
                    highestBidderThisRound = player
                    println("\t\t${player.name} bids $$bidAmount")
                } else {
                    println("\t\t${player.name} bids $$bidAmount (outbid)")
                }
            } else {
                println("\t\tWARNING: ${player.name} bid $$bidAmount is not higher than current $$roundStartBid")
                removeActiveBidder(player)
            }
        }

        if (highestBidderThisRound != null) {
            currentBid = highestBidThisRound
            currentWinner = highestBidderThisRound
            eventBus?.emit(GameEvent.AuctionBid(highestBidderThisRound, deed, highestBidThisRound, roundStartBid))
        }
    }

    private fun finalizeAuction(): Player? {
        val winner = currentWinner
        val winningBid = if (winner != null) currentBid else null

        eventBus?.emit(GameEvent.AuctionEnded(deed, winner, winningBid, filteredParticipants.size, roundNumber))

        if (winner != null) {
            try {
                if (winner.money < currentBid) {
                    winner.liquidateAssets(currentBid, bank, board)
                }
                bank.sellDeedToPlayer(deed::class, winner, board, auctionPrice = currentBid)
                println("\n\tAUCTION RESULT: ${winner.name} wins ${deed::class.simpleName} for $$currentBid")
                println("\t${winner.name} purchased ${deed::class.simpleName} at auction for $$currentBid (list price: $${deed.price})")
            } catch (_: BankruptcyException) {
                println("\tERROR: Auction winner ${winner.name} cannot afford bid $$currentBid")
                winner.declareBankruptcy(bank, board)
                return null
            }
        } else {
            println("\n\tAUCTION RESULT: No bids received - ${deed::class.simpleName} remains with the bank")
        }

        return winner
    }
}
