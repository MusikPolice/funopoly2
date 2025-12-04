package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.card.Card
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.event.GameEvent
import ca.jonathanfritz.monopoly.exception.BankruptcyException
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import ca.jonathanfritz.monopoly.strategy.DefaultStrategy
import ca.jonathanfritz.monopoly.strategy.PlayerStrategy
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KClass

@Suppress("ktlint:standard:no-blank-line-in-list")
open class Player(
    // this player's name, for display purposes only
    val name: String,

    // the amount of money that this player has
    var money: Int = 0,

    // the player's position on the board
    var position: Int = 0,

    // the properties that this player owns, along with their development state
    val deeds: MutableMap<TitleDeed, Development> = mutableMapOf(),

    // true if the player has fully liquidated all assets and does not have enough money to cover a debt
    private var isBankrupt: Boolean = false,

    // any Get out of Jail Free cards that the player has in their inventory
    private val getOutOfJailFreeCards: MutableList<Card.GetOutOfJailFreeCard> = mutableListOf(),

    // optional event bus for statistics collection
    private val eventBus: EventBus? = null,

    // strategy for decision-making (defaults to original behavior)
    private val strategy: PlayerStrategy = DefaultStrategy(),
) {
    // true if the player is in jail (as opposed to just visiting)
    var remainingTurnsInJail = 0
        private set
    var isInJail: Boolean = false
        set(value) {
            remainingTurnsInJail =
                if (value) {
                    3
                } else {
                    0
                }
            field = value
        }

    // expose isBankrupt as read-only
    fun isBankrupt() = isBankrupt

    fun decrementRemainingTurnsInJail(): Int {
        if (isInJail && remainingTurnsInJail > 0) {
            if (remainingTurnsInJail == 1) {
                isInJail = false
            } else {
                return --remainingTurnsInJail
            }
        }
        return 0
    }

    fun <T : TitleDeed> isOwner(titleDeed: KClass<T>): Boolean = deeds.keys.map { it::class }.contains(titleDeed)

    fun getDevelopment(deedClass: KClass<out TitleDeed>): Development =
        deeds.entries.firstOrNull { it.key::class == deedClass }?.value
            ?: throw PropertyOwnershipException("$name does not own ${deedClass.simpleName}")

    // a player has a monopoly on a property set if they own all properties that belong to that set
    fun hasMonopoly(colourGroup: ColourGroup) =
        deeds.keys
            .filter { deed ->
                deed.colourGroup == colourGroup
            }.map {
                it::class
            }.containsAll(
                colourGroup.titleDeeds().values.map { it::class },
            )

    // net worth is all cash on hand, plus the price of all owned properties, plus the price of all developed buildings
    fun netWorth(): Int =
        money +
            deeds.keys.sumOf { it.price } +
            deeds
                .filter { it.key.isBuildable }
                .map { (deed, development) ->
                    val buildingCost = (deed as Property).buildingCost
                    val developments = development.numHouses + if (development.hasHotel) 1 else 0
                    buildingCost * developments
                }.sum()

    // income tax is the lesser of $200 or 10% of net worth
    fun incomeTaxAmount(): Int = ceil(min(200.0, netWorth() * 0.10)).toInt()

    // give the player a get out of jail free card for later use
    fun grantGetOutOfJailFreeCard(card: Card.GetOutOfJailFreeCard) = getOutOfJailFreeCards.add(card)

    fun hasGetOutOfJailFreeCard(): Boolean = getOutOfJailFreeCards.isNotEmpty()

    // returns an instance of a Get out of Jail Free card if the player intends to use one, else null
    fun useGetOutOfJailFreeCard(): Card.GetOutOfJailFreeCard? {
        if (isInJail && hasGetOutOfJailFreeCard()) {
            println("\t\t$name uses a Get out of Jail Free card")
            return getOutOfJailFreeCards.removeAt(0)
        }
        return null
    }

    // returns true if the player intends to pay a fine to get out of jail on this turn
    fun isPayingGetOutOfJailEarlyFee(
        amount: Int,
        board: Board,
    ): Boolean {
        // Basic checks: must be in jail, no card, turns remaining
        if (!isInJail || hasGetOutOfJailFreeCard() || remainingTurnsInJail <= 0) {
            return false
        }
        
        // Delegate to strategy for decision
        return strategy.shouldPayJailFee(amount, this, board)
    }

    // returns a Pair<num houses, num hotels> that includes developments on all owned properties
    fun countDevelopments(): Pair<Int, Int> = deeds.values.sumOf { it.numHouses } to deeds.values.sumOf { (if (it.hasHotel) 1 else 0) }

    fun pay(
        amount: Int,
        other: Player,
        bank: Bank,
        board: Board,
        reason: String = "",
    ) {
        if (amount < 0) throw IllegalArgumentException("Amount to pay must be greater than $0")
        if (money < amount) {
            try {
                liquidateAssets(amount, bank, board)
            } catch (_: BankruptcyException) {
                declareBankruptcy(other, bank, board)
                return
            }
        }

        println($$"\t\t$$name pays $${other.name} $$$amount $$reason")
        money -= amount
        other.money += amount
    }

    fun isBuying(
        deed: TitleDeed,
        bank: Bank,
        board: Board,
    ): Boolean = strategy.shouldBuyProperty(deed, this, bank, board)

    fun shouldUnmortgageProperty(
        deed: TitleDeed,
        mortgageValue: Int,
        board: Board,
    ): Boolean {
        val unmortgageCost = ceil(mortgageValue * 1.1).toInt()
        return strategy.shouldUnmortgageProperty(deed, unmortgageCost, this, board)
    }

    fun developProperties(
        bank: Bank,
        board: Board,
    ) {
        // Filter to properties that CAN be developed (monopolies, affordable, even-building legal)
        val developableProperties =
            deeds
                .filterNot { it.value.hasHotel }
                .map { it.key }
                .filterIsInstance<Property>()
                .filter { property ->
                    // Must have monopoly on the color group
                    hasMonopoly(property.colourGroup)
                }
                .filter { property ->
                    // Must be able to afford it
                    property.buildingCost <= money
                }
                .filter { property ->
                    // Must respect even building rules
                    when (getDevelopment(property::class).numHouses) {
                        4 -> property.addingOrRemovingHotelRespectsEvenBuildingRules(this)
                        else -> property.addingHouseRespectsEvenBuildingRules(this)
                    }
                }

        // Delegate to strategy to select which property to develop
        val propertyToDevelop = strategy.selectPropertyToDevelop(developableProperties, this, bank, board)

        // Build on the selected property
        propertyToDevelop?.let { property ->
            when (getDevelopment(property::class).numHouses) {
                4 -> bank.sellHotelToPlayer(property::class, this, board)
                else -> bank.sellHouseToPlayer(property::class, this, board)
            }
        }
    }

    fun unmortgageProperties(
        bank: Bank,
        board: Board,
    ) {
        deeds
            .filter { (_, development) -> development.isMortgaged }
            .filter { (deed, _) ->
                // only unmortgage if strategy says we should
                shouldUnmortgageProperty(deed, deed.mortgageValue, board)
            }.forEach { (deed, _) ->
                bank.unmortgageDeed(deed::class, this, board)
            }
    }

    // when this is called, the player will attempt to mortgage or sell enough assets to cover the specified amount
    fun liquidateAssets(
        requiredAmount: Int,
        bank: Bank,
        board: Board,
    ) {
        // Step 1: Mortgage non-monopoly properties first
        val nonMonopolyDeeds =
            deeds
                .filterNot { hasMonopoly(it.key.colourGroup) }
                .keys
                .filter { deed -> !deeds[deed]!!.isMortgaged }
                .filter { deed ->
                    val dev = deeds[deed]!!
                    !dev.hasHotel && dev.numHouses == 0
                }

        val mortgagePriority = strategy.prioritizeMortgages(nonMonopolyDeeds.toList(), this, board)
        for (deed in mortgagePriority) {
            if (money >= requiredAmount) return
            bank.mortgageDeed(deed::class, this)
        }
        if (money >= requiredAmount) return

        do {
            // Step 2: Sell buildings (must respect even building rules)
            val developedProperties =
                deeds
                    .filter { it.value.hasHotel || it.value.numHouses > 0 }
                    .keys
                    .filterIsInstance<Property>()

            val sellPriority = strategy.prioritizeBuildingSales(developedProperties, this, board)

            // Sell buildings one at a time, respecting even building rules
            var soldSomething = false
            for (property in sellPriority) {
                if (money >= requiredAmount) return

                val development = deeds[property]!!
                val canSell =
                    if (development.hasHotel) {
                        property.addingOrRemovingHotelRespectsEvenBuildingRules(this)
                    } else {
                        property.removingHouseRespectsEvenBuildingRules(this)
                    }

                if (canSell) {
                    if (development.hasHotel) {
                        bank.buyHotelFromPlayer(property::class, this)
                    } else {
                        bank.buyHouseFromPlayer(property::class, this)
                    }
                    soldSomething = true
                    break // Restart loop to re-evaluate priorities
                }
            }

            if (money >= requiredAmount) return

            // Step 3: Mortgage monopoly properties (now that they're undeveloped)
            val monopolyDeeds =
                deeds
                    .filter { hasMonopoly(it.key.colourGroup) }
                    .keys
                    .filter { deed -> !deeds[deed]!!.isMortgaged }
                    .filter { deed ->
                        val dev = deeds[deed]!!
                        !dev.hasHotel && dev.numHouses == 0
                    }

            val monopolyMortgagePriority = strategy.prioritizeMortgages(monopolyDeeds.toList(), this, board)
            for (deed in monopolyMortgagePriority) {
                if (money >= requiredAmount) return
                bank.mortgageDeed(deed::class, this)
            }
            if (money >= requiredAmount) return

            // If we didn't sell anything and still need money, we're stuck
            if (!soldSomething) break
        } while (!hasFullyLiquidatedAssets())

        // if all else fails, this player is bankrupt
        println($$"\t\t$$name owes $$$requiredAmount but has liquidated all assets and only has $$$money remaining")
        throw BankruptcyException($$"$$name has insufficient funds ($$$money < $$$requiredAmount)")
    }

    private fun hasFullyLiquidatedAssets(): Boolean =
        deeds.isEmpty() ||
            deeds.values.all { development ->
                development.numHouses == 0 && !development.hasHotel && development.isMortgaged
            }

    fun declareBankruptcy(
        bank: Bank,
        board: Board,
    ) {
        if (isBankrupt) {
            throw IllegalStateException("$name is already bankrupt and cannot declare bankruptcy again")
        }

        if (!hasFullyLiquidatedAssets()) {
            throw IllegalStateException("$name has declared bankruptcy without first liquidating their assets")
        }

        val netWorthAtBankruptcy = netWorth()

        // money
        bank.charge(money, this, board, "in the bankruptcy settlement")

        // cards
        while (hasGetOutOfJailFreeCard()) board.returnGetOutOfJailFreeCard(this.getOutOfJailFreeCards.removeAt(0))

        // deeds - this is meant to trigger an auction
        bank.transferMortgagedDeeds(this.deeds.keys)
        this.deeds.clear()

        isBankrupt = true
        println("\t\t$name is bankrupt!")

        // emit bankruptcy event
        eventBus?.emit(GameEvent.PlayerBankrupted(this, bank, board.currentRound, netWorthAtBankruptcy))
    }

    private fun declareBankruptcy(
        creditor: Player,
        bank: Bank,
        board: Board,
    ) {
        if (isBankrupt) {
            throw IllegalStateException("$name is already bankrupt and cannot declare bankruptcy again")
        }

        if (!hasFullyLiquidatedAssets()) {
            throw IllegalStateException("$name has declared bankruptcy without first liquidating their assets")
        }

        val netWorthAtBankruptcy = netWorth()

        // 1. Transfer cash
        creditor.money += this.money
        this.money = 0

        // 2. Transfer Get Out of Jail Free cards
        while (hasGetOutOfJailFreeCard()) {
            creditor.grantGetOutOfJailFreeCard(this.getOutOfJailFreeCards.removeAt(0))
        }

        // 3. Pre-calculate total mortgage fees creditor will owe
        val totalMortgageFees =
            this.deeds.values
                .filter { it.isMortgaged }
                .sumOf { development ->
                    val deed = this.deeds.keys.first { this.deeds[it] == development }
                    if (creditor.shouldUnmortgageProperty(deed, deed.mortgageValue, board)) {
                        ceil(deed.mortgageValue * 1.1).toInt() // Unmortgage cost
                    } else {
                        ceil(deed.mortgageValue * 0.1).toInt() // Assumption fee
                    }
                }

        // 4. Ensure creditor can pay fees (liquidate if necessary)
        if (creditor.money < totalMortgageFees) {
            try {
                creditor.liquidateAssets(totalMortgageFees, bank, board)
            } catch (_: BankruptcyException) {
                // Cascading bankruptcy: creditor can't afford to receive assets
                println("\t\t${creditor.name} cannot afford to receive ${this.name}'s assets")

                // Creditor bankrupts to bank
                creditor.declareBankruptcy(bank, board)

                // Original bankrupt player also bankrupts to bank (change creditor from player to bank)
                // Transfer remaining assets to bank instead of creditor
                bank.charge(this.money, this, board, "in the bankruptcy settlement")
                while (hasGetOutOfJailFreeCard()) board.returnGetOutOfJailFreeCard(this.getOutOfJailFreeCards.removeAt(0))
                bank.transferMortgagedDeeds(this.deeds.keys)
                this.deeds.clear()

                isBankrupt = true
                println("\t\t$name is bankrupt!")

                // Emit bankruptcy to bank, not to the original creditor
                eventBus?.emit(GameEvent.PlayerBankrupted(this, bank, board.currentRound, netWorthAtBankruptcy))
                return
            }
        }

        // 5. Transfer deeds with mortgage handling
        val deedsToTransfer = this.deeds.toList() // snapshot to avoid concurrent modification
        for ((deed, development) in deedsToTransfer) {
            // Fail fast if properties have development - this indicates a logic error
            if (development.numHouses > 0 || development.hasHotel) {
                throw ca.jonathanfritz.monopoly.exception.PropertyDevelopmentException(
                    "$name cannot transfer ${deed::class.simpleName} with development " +
                        "(houses: ${development.numHouses}, hotel: ${development.hasHotel}). " +
                        "Properties must be fully liquidated before bankruptcy.",
                )
            }

            // Transfer the deed first
            creditor.deeds[deed] = development

            if (development.isMortgaged) {
                // Creditor must decide: unmortgage or assume
                if (creditor.shouldUnmortgageProperty(deed, deed.mortgageValue, board)) {
                    // Unmortgage: pay 110% and emit event
                    bank.unmortgageDeed(deed::class, creditor, board)
                } else {
                    // Assume mortgage: pay 10% fee
                    val assumptionFee = ceil(deed.mortgageValue * 0.1).toInt()
                    bank.charge(assumptionFee, creditor, board, "to assume mortgage on ${deed::class.simpleName}")
                    // development.isMortgaged remains true
                }
            }
        }

        // 6. Clear bankrupt player's deeds
        this.deeds.clear()

        isBankrupt = true
        println("\t\t$name is bankrupt!")

        // emit bankruptcy event
        eventBus?.emit(GameEvent.PlayerBankrupted(this, creditor, board.currentRound, netWorthAtBankruptcy))
    }

    data class Development(
        var numHouses: Int = 0,
        var hasHotel: Boolean = false,
        var isMortgaged: Boolean = false,
    )
}
