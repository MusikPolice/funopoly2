package ca.jonathanfritz.monopoly

class Bank {
    // https://www.hasbro.com/common/instruct/00009.pdf
    var houses = 32
    var hotels = 12

    // https://www.monopolyland.com/how-much-money-in-monopoly-set/
    // TODO: apparently old sets (pre-2008) shipped with $15,140 - could be a difference that we can test
    // TODO: per the rules, the bank can never run out of money, so maybe this doesn't matter?
    var money = 20580

    // a list of all title deeds that the bank can sell to players
    val titleDeeds: MutableList<TitleDeed> = mutableListOf(*TitleDeed.values.values.toTypedArray())
}