package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.exception.InsufficientFundsException

class Bank (
    // https://www.hasbro.com/common/instruct/00009.pdf
    private var houses: Int = 32,
    private var hotels: Int = 12,

    // https://www.monopolyland.com/how-much-money-in-monopoly-set/
    // TODO: apparently old sets (pre-2008) shipped with $15,140 - could be a difference that we can test
    // TODO: per the rules, the bank can never run out of money, so maybe this doesn't matter?
    var money: Int = 20580,

    // a list of all title deeds that the bank can sell to players
    private val titleDeeds: MutableList<TitleDeed> = mutableListOf(*TitleDeed.values.values.toTypedArray())
) {
    fun pay(player: Player, amount: Int) {
        if (amount < 0) throw IllegalArgumentException("Amount to pay must be greater than $0")

        println("${player.name} receives \$$amount")
        money -= amount
        player.money += amount
    }

    fun charge(player: Player, amount: Int, reason: String = "") {
        if (amount < 0) throw IllegalArgumentException("Amount to charge must be greater than $0")
        if (player.money < amount) throw InsufficientFundsException("${player.name} has insufficient funds (${player.money} < $amount) $reason")

        println("\t\t${player.name} pays \$$amount $reason")
        money += amount
        player.money -= amount
    }

    fun useGetOutOfJailFreeCard(player: Player) {
        TODO("Not yet implemented - need chest/community chest first!")
    }
}