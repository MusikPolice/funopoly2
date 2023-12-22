package ca.jonathanfritz.monopoly.deed

import kotlin.reflect.KClass

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

    // returns a subset of all title deeds that belong to the colour group receiver
    fun titleDeeds(): Map<KClass<out TitleDeed>, TitleDeed> = Property.values.filter { (_, property) -> property.colourGroup == this } +
            Railroad.values.filter { (_, railroad) -> railroad.colourGroup == this } +
            Utility.values.filter { (_, utility) -> utility.colourGroup == this }
}