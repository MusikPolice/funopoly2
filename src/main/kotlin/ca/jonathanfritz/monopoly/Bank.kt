package ca.jonathanfritz.monopoly

class Bank {
    // https://www.hasbro.com/common/instruct/00009.pdf
    var houses = 32
    var hotels = 12

    // https://www.monopolyland.com/how-much-money-in-monopoly-set/
    // TODO: apparently old sets (pre-2008) shipped with $15,140 - could be a difference that we can test
    // TODO: per the rules, the bank can never run out of money, so maybe this doesn't matter?
    var money = 20580

    // the title deeds that the bank can sell to players
    val titleDeeds: MutableList<TitleDeed> = mutableListOf(
        Property.mediterraneanAvenue,
        Property.balticAvenue,
        Railroad.reading,
        Property.orientalAvenue,
        Property.vermontAvenue,
        Property.connecticutAvenue,

        Property.stCharlesPlace,
        Utility.electricCompany,
        Property.statesAvenue,
        Property.virginiaAvenue,
        Railroad.pennsylvania,
        Property.stJamesPlace,
        Property.tennesseeAvenue,
        Property.newYorkAvenue,

        Property.kentuckyAvenue,
        Property.indianaAvenue,
        Property.illinoisAvenue,
        Railroad.bo,
        Property.atlanticAvenue,
        Property.ventnorAvenue,
        Utility.waterWorks,
        Property.marvinGardens,

        Property.pacificAvenue,
        Property.northCarolinaAvenue,
        Property.pennsylvaniaAvenue,
        Railroad.shortLine,
        Property.parkPlace,
        Property.boardwalk
    )
}