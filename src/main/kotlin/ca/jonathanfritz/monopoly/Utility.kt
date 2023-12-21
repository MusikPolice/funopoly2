package ca.jonathanfritz.monopoly

class Utility(titleDeedName: TitleDeedName): TitleDeed(titleDeedName, 150, 75) {
    // TODO: if one utility is owned, rent is 4x dice roll; if both are owned, rent is 10x dice roll

    // https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
    companion object {
        val electricCompany = Utility(TitleDeedName.ElectricCompany)
        val waterWorks = Utility(TitleDeedName.WaterWorks)
    }
}