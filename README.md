# Funopoly2

A Monopoly implementation written in Kotlin.

The long term goal for this project is to build a monte-carlo simulation that evaluates the relative "fun" level
of different house rules that are commonly used when playing the game.

For now, the focus is on building a correct and fully tested implementation of the base game that adheres exactly
to the official rules as published in game editions dating from 2021.

---

## Features

- **Complete Base Game Implementation**: All official Monopoly rules from 2021 edition
- **Player-to-Player Bankruptcy**: Full asset transfer with cascading bankruptcy
- **Property Development**: Houses, hotels, and even-building rules
- **Property Mortgaging**: Mortgage and unmortgage mechanics with proper fees
- **Event System**: Comprehensive event bus for game state tracking
- **Statistics Collection**: Detailed game statistics with console and JSON output

---

## Statistics Collection

Funopoly2 includes a comprehensive statistics collection system that tracks all game events and generates detailed reports.

### Quick Start

Enable statistics collection when creating a game:

```kotlin
import ca.jonathanfritz.monopoly.*
import ca.jonathanfritz.monopoly.event.EventBus
import ca.jonathanfritz.monopoly.statistics.StatisticsOutputFormat
import kotlin.random.Random

fun main() {
    val eventBus = EventBus()
    val config = Config(
        collectStatistics = true,
        statisticsOutputFormat = StatisticsOutputFormat.CONSOLE  // or JSON
    )
    
    Monopoly(
        players = listOf(
            Player("Alice", eventBus = eventBus),
            Player("Bob", eventBus = eventBus),
            Player("Charlie", eventBus = eventBus),
            Player("Dana", eventBus = eventBus),
        ),
        rng = Random(42),  // Use seeded RNG for reproducible games
        config = config,
        eventBus = eventBus,
    ).executeGame()
}
```

### Report Sections

The statistics report includes seven comprehensive sections:

1. **Game Summary**: Total rounds, winner, end reason, player count, bankruptcies
2. **Player Statistics**: Per-player metrics including:
   - Rent paid/collected and net rent
   - Properties purchased (with detailed lists)
   - Properties acquired via bankruptcy (with detailed lists)
   - Property spending
   - Houses and hotels built
   - Times passed GO
   - Doubles rolled
   - Jail visits
   - Monopolies acquired (color groups)
   - Bankruptcy round (if applicable)
3. **Financial Summary**: Total rent paid, bank transactions, largest rent payment, averages
4. **Property Statistics**: Total purchases/mortgages/unmortgages, most expensive property, purchases by color group
5. **Movement Statistics**: Dice rolls, averages, doubles, most/least landed tiles
6. **Development Statistics**: Houses/hotels built and sold, development by color group, most developed color group

### Output Formats

**Console Output** (default):
- Beautiful box-drawing characters with organized sections
- Game Summary, Player Statistics, Financial Summary, Property Statistics, Movement Statistics, Development Statistics

**JSON Output**:
- Valid JSON structure with proper null handling
- Suitable for programmatic analysis or external tools

### Configuration Options

The `Config` data class supports the following options:

```kotlin
data class Config(
    val maxRounds: Int = 1000,                  // Maximum rounds before draw
    val collectStatistics: Boolean = false,      // Enable statistics collection
    val statisticsOutputFormat: StatisticsOutputFormat = StatisticsOutputFormat.CONSOLE
)

enum class StatisticsOutputFormat {
    CONSOLE,  // Pretty-printed text with box-drawing characters
    JSON      // Valid JSON structure
}
```

### Event System

The statistics system is built on a comprehensive event bus that emits 22 different event types:

- **Game lifecycle**: `RoundStarted`, `RoundEnded`, `TurnStarted`, `TurnEnded`, `GameEnded`
- **Movement**: `DiceRolled`, `PlayerMoved`, `TileLanded`, `PassedGo`
- **Financial**: `BankPaidPlayer`, `PlayerPaidBank`, `RentPaid`
- **Property**: `PropertyPurchased`, `PropertyMortgaged`, `PropertyUnmortgaged`
- **Development**: `HousePurchased`, `HotelPurchased`, `HouseSold`, `HotelSold`
- **Jail**: `PlayerSentToJail`, `PlayerLeftJail`
- **Cards**: `CardDrawn`
- **Bankruptcy**: `PlayerBankrupted`

See `docs/TechnicalAnalysis.md` for detailed event system documentation.

---

## Building & Testing

**Run all tests:**
```bash
./gradlew test
```

**Run the game:**
```bash
./gradlew run
```

---

## Project Structure

See `docs/TechnicalAnalysis.md` for comprehensive technical documentation including:
- Architecture & code structure
- Core domain model
- Game mechanics implementation details
- Design patterns and best practices
- Event system architecture
- Statistics collection internals

See `docs/plan.md` for the phased development plan and progress tracking.
