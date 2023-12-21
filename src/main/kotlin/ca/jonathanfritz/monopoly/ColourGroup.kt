package ca.jonathanfritz.monopoly

enum class ColourGroup {
    Railroads,
    Utilities,
    Brown,
    LightBlue,
    Pink,
    Orange,
    Red,
    Yellow,
    Green,
    DarkBlue;

    fun titleDeedNames(): List<TitleDeedName> = TitleDeedName.values().filter { it.colourGroup == this }
}