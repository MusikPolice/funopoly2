package ca.jonathanfritz.monopoly

data class Player(
    val name: String,
    private var money: Int,
    private val titleDeeds: MutableList<TitleDeed> = mutableListOf()
) {

    // a player has a monopoly on a property set if they own all properties that belong to that set
    private val monopolies = titleDeeds.groupBy { it.titleDeedName.colourGroup }
        .filter { (ownedColourGroup, ownedTitleDeeds) ->
            ColourGroup.values()
                .first { it == ownedColourGroup }.titleDeedNames()
                .all { titleDeedName -> ownedTitleDeeds.map { ownedTitleDeed -> ownedTitleDeed.titleDeedName }.contains(titleDeedName) }
        }

    fun isOwner(titleDeed: TitleDeed): Boolean = titleDeeds.contains(titleDeed)

    fun hasMonopoly(propertySet: ColourGroup) = monopolies.containsKey(propertySet)
}
