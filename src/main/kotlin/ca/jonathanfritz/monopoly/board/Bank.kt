package ca.jonathanfritz.monopoly.board

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.deed.TitleDeed

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
        println("${player.name} receives \$$amount")
        money -= amount
        player.money += amount
    }
}