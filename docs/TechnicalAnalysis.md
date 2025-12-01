# Funopoly2 Technical Analysis

**Version:** 1.0-SNAPSHOT  
**Last Updated:** December 1, 2025  
**Primary Language:** Kotlin 2.2.20  
**Target JVM:** 17

---

## 1. Project Purpose & Goals

### Primary Objective
Build a **Monte Carlo simulation** that evaluates the relative "fun" level of different house rules commonly used when playing Monopoly.

### Current Phase
Implementing a **correct and fully tested** base game that adheres to official Monopoly rules as published in 2021 editions.

### Future Vision
- Simulate thousands of games with varying rule configurations
- Collect statistical data on game outcomes, player experiences, and game duration
- Quantify the impact of house rules on game dynamics

---

## 2. Architecture & Code Structure

### Package Organization

```
ca.jonathanfritz.monopoly/
├── Monopoly.kt              # Main game orchestrator
├── Player.kt                # Player state and behavior
├── Config.kt                # Game configuration
├── board/
│   ├── Board.kt             # Game board, round execution
│   ├── Bank.kt              # Financial transactions, property sales
│   ├── Dice.kt              # Dice rolling mechanics
│   └── Tile.kt              # Board tile types and landing behavior
├── card/
│   ├── Card.kt              # Base card types
│   ├── ChanceCard.kt        # All Chance cards (16 cards)
│   ├── CommunityChestCard.kt # All Community Chest cards (17 cards)
│   └── Deck.kt              # Card deck management
├── deed/
│   ├── TitleDeed.kt         # Abstract base for all deeds
│   ├── Property.kt          # 22 buildable properties
│   ├── Railroad.kt          # 4 railroads
│   ├── Utility.kt           # 2 utilities
│   └── ColourGroup.kt       # Property grouping enum
└── exception/
    ├── BankruptcyException.kt
    ├── InsufficientFundsException.kt
    ├── InsufficientTokenException.kt
    ├── MonopolyOwnershipException.kt
    ├── PropertyDevelopmentException.kt
    └── PropertyOwnershipException.kt
```

### Design Patterns

**Sealed Classes**  
- `TitleDeed`, `Property`, `Railroad`, `Utility` - Type-safe property hierarchies
- `Tile` - All 40 board spaces as sealed class instances
- `Card`, `ChanceCard`, `CommunityChestCard` - Card type hierarchies

**Companion Objects**  
- Used in `Property`, `Railroad`, `Utility` to create pseudo-enum behavior with reflection
- Provides `values` map for accessing all instances of a sealed class

**Strategy Pattern**  
- Tiles implement `onLanding()` with behavior specific to each tile type
- Cards implement `onDraw()` with card-specific effects

---

## 3. Core Domain Model

### 3.1 Monopoly (Main Game Class)
**Location:** `Monopoly.kt`

**Responsibilities:**
- Game initialization (grants starting cash, sets player positions)
- Game loop execution via `executeGame()`
- Victory condition detection (one player remaining)

**Key Details:**
- Configurable via `Monopoly.Config` (currently only `maxRounds`)
- Uses seeded RNG for reproducible games (currently seed=1)
- Delegates round execution to `Board`

### 3.2 Player
**Location:** `Player.kt`

**State:**
- `name: String` - Display name
- `money: Int` - Cash on hand
- `position: Int` - Board position (0-39)
- `deeds: MutableMap<TitleDeed, Development>` - Owned properties with development state
- `isInJail: Boolean` - Jail status (auto-sets `remainingTurnsInJail`)
- `getOutOfJailFreeCards: MutableList` - Inventory of get-out-of-jail cards

**Key Methods:**

**`hasMonopoly(colourGroup: ColourGroup): Boolean`**
- Returns true if player owns all properties in a color group
- Essential for development and rent doubling

**`netWorth(): Int`**
- Formula: `cash + deed prices + building costs`
- Used for income tax calculation and liquidation decisions

**`incomeTaxAmount(): Int`**
- Returns lesser of $200 or 10% of net worth

**`isBuying(deed: TitleDeed): Boolean`**
- Current strategy: Buy if `money > deed.price`
- TODO: More sophisticated purchasing logic

**`developProperties(bank: Bank, board: Board)`**
- **Critical Algorithm** - See Section 4.1

**`liquidateAssets(requiredAmount: Int, bank: Bank, board: Board)`**
- **Critical Algorithm** - See Section 4.2

**`declareBankruptcy()`**
- Two variants: bankruptcy to bank vs. bankruptcy to player
- Transfers all assets, clears deeds, sets `isBankrupt = true`

### 3.3 Board
**Location:** `board/Board.kt`

**Responsibilities:**
- Maintains 40-tile board layout
- Manages Chance and Community Chest decks
- Executes game rounds
- Player movement and tile interaction

**Key Components:**

**Tiles List (40 tiles):**
```kotlin
Go -> MediterraneanAvenue -> CommunityChest -> BalticAvenue -> IncomeTax -> ...
```

**Decks:**
- `chance: Deck<Card>` - 16 Chance cards
- `communityChest: Deck<Card>` - 17 Community Chest cards
- Both decks shuffle when depleted

**Key Methods:**

**`executeRound(round: Int)`**
- Iterates through non-bankrupt players
- Handles jail escape attempts
- Manages dice rolling (including doubles)
- Calls `player.developProperties()` after each roll
- Enforces three-consecutive-doubles rule

**`advancePlayerBy(player, offset, collectSalary, rentOverride)`**
- Moves player by offset tiles (can be negative)
- Detects passing Go (only in forward direction)
- Triggers tile landing behavior
- Supports rent override for special card effects

**`advancePlayerToTile(player, tileClass, rentOverride)`**
- Advances to next instance of tile type
- Used by cards like "Advance to nearest Railroad"

### 3.4 Bank
**Location:** `board/Bank.kt`

**State:**
- `money: Int = 20580` - Total bank cash (per 2008+ rules)
- `availableHouses: Int = 32` - Limited supply
- `availableHotels: Int = 12` - Limited supply
- `titleDeeds: MutableList<TitleDeed>` - Unsold properties

**Key Methods:**

**`pay(amount, player, reason)`**
- Bank pays player
- Decreases bank money, increases player money

**`charge(amount, player, board, reason)`**
- Player pays bank
- Triggers `player.liquidateAssets()` if insufficient funds
- Triggers `player.declareBankruptcy()` on `BankruptcyException`

**`sellDeedToPlayer(deedClass, player, board)`**
- Validates player can afford deed
- Removes deed from bank inventory
- Adds deed to player's deeds with `Development()`

**`sellHouseToPlayer(propertyClass, player, board)`**
**`sellHotelToPlayer(propertyClass, player, board)`**
- Validates monopoly ownership
- Validates even building rules
- Checks house/hotel supply
- Updates player's `Development` state

**`buyHouseFromPlayer(propertyClass, player)`**
**`buyHotelFromPlayer(propertyClass, player)`**
- Player receives half of building cost
- Validates even building rules
- Returns houses/hotels to bank supply

**`mortgageDeed(deedClass, player)`**
- Pays player the `mortgageValue`
- Sets `Development.isMortgaged = true`
- Cannot mortgage developed properties

### 3.5 Title Deeds
**Location:** `deed/`

**TitleDeed (Abstract Base)**
- `colourGroup: ColourGroup`
- `price: Int`
- `mortgageValue: Int`
- `isBuildable: Boolean` (abstract)
- `calculateRent(owner, board): Int` (abstract)

**Property (22 instances)**
- Buildable (houses/hotels)
- Additional fields: `buildingCost`, rent schedule for 0-4 houses + hotel
- Rent calculation:
  - Mortgaged: $0
  - Hotel: `rentHotel`
  - 0 houses + monopoly: `rentNoHouse * 2`
  - Otherwise: `houseRents[numHouses]`

**Railroad (4 instances)**
- Not buildable
- Fixed price: $200, mortgage: $100
- Rent based on count of unmortgaged railroads owned:
  - 1: $25
  - 2: $50
  - 3: $100
  - 4: $200

**Utility (2 instances)**
- Not buildable
- Fixed price: $150, mortgage: $75
- Rent: `dice.previousRoll().amount * multiplier`
  - 1 utility: 4x
  - 2 utilities: 10x

**Even Building Rules (TitleDeed)**
- `addingHouseRespectsEvenBuildingRules(player)`
- `removingHouseRespectsEvenBuildingRules(player)`
- `addingOrRemovingHotelRespectsEvenBuildingRules(player)`

Rules:
1. Houses must be built evenly across a monopoly
2. Can add house if all properties have same count, OR target has minimum count
3. Can remove house if target has maximum count
4. Hotels require all properties in monopoly have 4 houses or a hotel

### 3.6 Tiles
**Location:** `board/Tile.kt`

**Sealed Class Hierarchy:**
- `Go` - Nothing special (unless house rules)
- `Buyable` (abstract)
  - `PropertyBuyable` - 22 properties
  - `RailroadBuyable` - 4 railroads
  - `UtilityBuyable` - 2 utilities
- `CommunityChest` - Draw card (4 tiles)
- `Chance` - Draw card (3 tiles)
- `IncomeTax` - Pay `player.incomeTaxAmount()`
- `LuxuryTax` - Pay $100
- `Jail` - Just visiting vs. in jail
- `FreeParking` - Nothing (unless house rules)
- `GoToJail` - Sends player to jail

**Buyable Landing Behavior:**
1. Find owner (if any)
2. If owned by self: no action
3. If owned by other:
   - If mortgaged: no rent
   - Otherwise: calculate and pay rent
4. If unowned:
   - Offer to buy
   - TODO: Trigger auction if declined

### 3.7 Cards
**Location:** `card/`

**16 Chance Cards:**
- `AdvanceToGo`, `AdvanceToProperty` (Illinois, St Charles, Boardwalk)
- `AdvanceToRailroad` (Reading, nearest x2)
- `AdvanceToNearestUtility`
- `BankPaysYouDividend` ($50)
- `GetOutOfJailFree`
- `GoBackThreeSpaces`
- `GoToJail`
- `GeneralRepairs` ($25/house, $100/hotel)
- `PoorTax` ($15)
- `ChairmanOfTheBoard` (pay $50 to each player)
- `BuildingAndLoan` ($150)

**17 Community Chest Cards:**
- `AdvanceToGo`
- `BankErrorInYourFavour` ($200)
- `DoctorsFees` ($50)
- `SaleOfStock` ($50)
- `GetOutOfJailFree`
- `GoToJail`
- `GrandOperaOpening` (collect $50 from each player)
- `HolidayFundMatures` ($100)
- `IncomeTaxRefund` ($20)
- `YourBirthday` (collect $10 from each player)
- `LifeInsurance` ($100)
- `HospitalFees` ($50)
- `SchoolFees` ($50)
- `ConsultancyFees` ($25)
- `StreetRepairs` ($40/house, $115/hotel)
- `BeautyContest` ($10)
- `Inheritance` ($100)

**Card Implementation Pattern:**
- Base class `Card` with `onDraw(player, bank, board)`
- Subclasses: `BankPaysYou`, `YouPayBank`, `GetOutOfJailFreeCard`
- Each card implements specific game effect

### 3.8 Dice
**Location:** `board/Dice.kt`

- Two six-sided dice
- Caches `previous: Roll` for utility rent calculation
- `Roll` data class: `die1`, `die2`, `amount`, `isDoubles`, `highest`

---

## 4. Key Algorithms & Business Logic

### 4.1 Property Development Algorithm
**Location:** `Player.developProperties()`

**Strategy:** Aggressive development prioritizing highest-ROI properties

**Algorithm:**
```
1. Filter deeds:
   - Exclude hotels (cannot develop further)
   - Include only Property instances (not railroads/utilities)

2. Group by colour group where player has monopoly

3. Filter affordable properties (buildingCost < money)

4. Sort by descending current rent (proxy for ROI)

5. Find first property that satisfies even building rules:
   - If 4 houses: can add hotel?
   - Otherwise: can add house?

6. Execute development:
   - 4 houses → buy hotel
   - <4 houses → buy house
```

**Characteristics:**
- **Greedy:** Develops highest-rent property first
- **Aggressive:** Spends all available cash (leaves minimal reserves)
- **One building per turn:** Only develops one property per call

**Known Issue (Line 166-167):**
> "This is pretty aggressive - On round 27, Elmo spends all but $44 to build a house. Consider holding at least highest rent on the board in escrow."

### 4.2 Asset Liquidation Algorithm
**Location:** `Player.liquidateAssets()`

**Goal:** Raise funds to cover debt while minimizing loss of future income

**Algorithm:**
```
1. PHASE 1: Mortgage non-monopoly properties
   - Filter properties NOT in a monopoly
   - Exclude already mortgaged
   - Exclude properties with houses/hotels
   - Sort by:
     a. Descending properties needed to complete monopoly (prefer far-from-monopoly)
     b. Ascending mortgage value (mortgage cheapest first)
   - Mortgage until debt covered
   
2. PHASE 2: Sell buildings (iterative)
   - Filter properties with houses or hotels
   - Sort by ascending rent (sell lowest-rent buildings first)
   - For each property:
     a. If hotel: sell hotel → 4 houses (if even building rules allow)
     b. If houses: sell one house (if even building rules allow)
   - Continue until debt covered
   
3. PHASE 3: Mortgage monopoly properties
   - Filter properties IN a monopoly
   - Exclude mortgaged
   - Exclude properties with buildings (should be cleared by Phase 2)
   - Same sorting as Phase 1
   - Mortgage until debt covered
   
4. Repeat Phases 2-3 until:
   - Debt covered, OR
   - All assets fully liquidated (throw BankruptcyException)
```

**Strategy Rationale:**
- Protect monopolies as long as possible (double rent, development potential)
- Minimize income loss by selling lowest-rent buildings
- Even building rules constrain which buildings can be sold

### 4.3 Rent Calculation

**Property Rent:**
```kotlin
if (mortgaged) 0
else if (hotel) rentHotel
else if (noHouses && hasMonopoly) rentNoHouse * 2
else houseRents[numHouses]
```

**Railroad Rent:**
- Count unmortgaged railroads owned
- 1→$25, 2→$50, 3→$100, 4→$200

**Utility Rent:**
- `previousDiceRoll * multiplier`
- 1 utility: 4x
- 2 utilities: 10x
- Special case: Some Chance cards override multiplier to 10x

### 4.4 Even Building Rules
**Location:** `TitleDeed.kt`

**House Addition:**
- All properties in monopoly have same house count, OR
- Target property has minimum house count

**House Removal:**
- Target property has maximum house count

**Hotel Addition/Removal:**
- All properties in monopoly have either 4 houses or a hotel

**Implementation:**
- Count houses on each property in color group
- Group by house count
- Validate target property against min/max counts

---

## 5. Testing Strategy

### Test Coverage

**Main Classes:**
- `MonopolyTest.kt` - Game initialization
- `PlayerTest.kt` - 388 lines, comprehensive player behavior
- `BoardTest.kt` - 486 lines, round execution, movement, jail
- `BankTest.kt` - 431 lines, transactions, property sales
- `DiceTest.kt` - Basic dice rolling
- `TileTest.kt` - Tile landing behavior

**Deeds:**
- `PropertyTest.kt` - Rent calculations, development
- `RailroadTest.kt` - Rent scaling
- `UtilityTest.kt` - Dice-based rent
- `TitleDeedTest.kt` - Even building rules
- `ColourGroupTest.kt` - Property grouping

**Cards:**
- `CardTest.kt` - Base card behavior
- `ChanceCardTest.kt` - 11,713 bytes, all Chance card effects
- `CommunityChestCardTest.kt` - Community Chest cards
- `DeckTest.kt` - Deck shuffling and drawing

### Testing Utilities
**Location:** `TestUtils.kt`

**Custom Assertions:**
- `assertPlayerOnProperty(player, Property::class)`
- `assertPlayerOnRailroad(player, Railroad::class)`
- `assertPlayerOnUtility(player, Utility::class)`
- `assertPlayerOn(player, Tile::class)`
- `assertPlayerOnChance(player, side)`
- `assertPlayerOnCommunityChest(player, side)`

**FakeDice:**
```kotlin
class FakeDice(private vararg val rolls: Roll) : Dice() {
    var rollCount = 0
    override fun roll(): Roll = rolls[rollCount++]
}
```
- Enables deterministic testing
- Tracks number of rolls executed

### Test Characteristics
- Uses JUnit 5 (`junit-jupiter` 6.0.1)
- Kotlin test assertions
- Heavy use of data class equality for validation
- Seeded RNG for reproducible test scenarios

---

## 6. Outstanding Features & TODOs

### High Priority (Core Game)

**Asset Transfer on Bankruptcy (`Player.kt:8`, `:287-289`)**
- When player bankrupts to another player, assets should transfer
- Receiving player must pay 10% fee on mortgaged properties OR unmortgage immediately
- Currently not implemented

**Property Auctions (Multiple locations)**
- `Monopoly.kt:11` - "property auctions on decline to buy?"
- `Tile.kt:69` - When player declines purchase
- `Bank.kt:264` - When bankrupted player returns mortgaged deeds
- `Bank.kt:94-95`, `172-174` - Houses/hotels can trigger auctions if multiple players want to build simultaneously

**Jail Payment Strategy (`Player.kt:110-111`)**
- Current: Always pay if `money > feeAmount`
- Should consider: Only pay if `money > highestRentOnBoard > $50`
- Staying in jail can be strategically advantageous late-game

**Purchase Strategy (`Player.kt:141-142`)**
- Current: Buy if `money > price`
- Should consider:
  - Reserve cash for rent obligations
  - Prioritize completing monopolies
  - May need to liquidate to complete strategic monopolies

**Development Strategy Refinement (`Player.kt:166-168`)**
- Current: Spends almost all cash on development
- Should: Hold highest rent on board in reserve

**Trading Between Players (`Monopoly.kt:12`, `Board.kt:172`)**
- Not implemented
- Critical for strategic gameplay
- Would enable monopoly formation through negotiation

**Unmortgaging Properties (`Board.kt:172`)**
- Players cannot currently unmortgage properties
- Should be possible on player's turn after rolling
- Costs `mortgageValue * 1.1`

### Medium Priority (Rules Accuracy)

**Update to 2023 Rules (`Monopoly.kt:9`)**
- Currently based on 2021 edition
- Need to verify changes in 2023 edition

**Bank Money Limit (`Bank.kt:21`)**
- Per rules: "the bank can never run out of money"
- Current implementation has fixed $20,580
- May not matter for simulation purposes

**Old Edition Differences (`Bank.kt:20`)**
- Pre-2008 sets had $15,140 in bank
- Could be interesting variant to test

**Rent Collection Attention Check (`Tile.kt:49-50`)**
- Rules: Owner must ask for rent before second following player rolls
- Could implement as probabilistic "attention" check
- Would affect game dynamics

### Low Priority (Simulation Features)

**Statistics Collection (`Monopoly.kt:10`)**
- Landing frequency by tile
- Rounds until victory
- Net worth deltas over time
- Property ownership patterns
- Development patterns

**House Rules (`Monopoly.kt:13`, `44`)**
- Free Parking pot
- Double salary on landing Go
- No auction on declined purchase
- Build out of turn
- Custom starting cash
- Custom house/hotel limits

---

## 7. Known Issues & Edge Cases

### 7.1 Edge Cases

**Doubles + Go to Jail Card (`BoardTest.kt:43-55`)**
- Documented edge case in test comments
- Player draws "Go to Jail" on first roll (doubles)
- System grants another turn because of doubles
- Player rolls doubles again and escapes jail
- **Current behavior:** May be incorrect per official rules
- **Needs clarification:** Should drawing "Go to Jail" card consume the doubles turn?

**Hotel Selling with House Shortage**
- Bank must have 4 houses available to buy hotel from player
- Creates interesting strategic scarcity
- Correctly implemented in `Bank.buyHotelFromPlayer()`

### 7.2 Incomplete Implementations

**Player-to-Player Bankruptcy**
- `Player.declareBankruptcy(player: Player)` exists but doesn't transfer assets
- Only prints bankruptcy message
- Full implementation requires:
  - Transfer all cash
  - Transfer all properties (mortgaged and unmortgaged)
  - Receiving player pays 10% fee on mortgages
  - Get Out of Jail Free cards return to decks

**Mortgaged Property Management**
- Players can mortgage but not unmortgage
- No fee calculation for assuming mortgages in bankruptcy
- Missing strategic depth

---

## 8. Technical Debt & Improvement Opportunities

### 8.1 Code Duplication

**Deed Lookup Patterns**
Repeated throughout `Bank.kt` and `Player.kt`:
```kotlin
val deed = player.deeds.keys.firstOrNull { it::class == deedClass }
    ?: throw PropertyOwnershipException(...)
```
Could be extracted to `Player.getDeed(deedClass)` method.

**Rent Override Lambda**
- `advancePlayerBy()`, `advancePlayerToTile()`, `advancePlayerToProperty()`, `advancePlayerToRailroad()` all take `rentOverride` parameter
- Only used by two Chance cards (AdvanceToNearestUtility, AdvanceToNearestRailroad)
- Could be refactored to event system or card-specific hooks

### 8.2 Type Safety

**KClass vs Type Parameters**
Many methods take `KClass<out TitleDeed>` when they could use type parameters:
```kotlin
// Current
fun sellDeedToPlayer(deedClass: KClass<out TitleDeed>, ...)

// Could be
fun <T : TitleDeed> sellDeedToPlayer(deedClass: KClass<T>, ...)
```
Would enable better compile-time type checking.

### 8.3 Complexity

**`Player.liquidateAssets()` (`Player.kt:192-252`)**
- 60 lines, complex nested logic
- Three phases with iteration
- TODO comment acknowledges "this code can be tidied up"
- Opportunity for:
  - Extract phases to separate methods
  - Create `LiquidationStrategy` class
  - Improve testability

**`Player.developProperties()` (`Player.kt:144-188`)**
- 44 lines of chained functional operations
- Multiple filters, maps, and sorts
- Opportunity for:
  - Extract candidate selection to separate method
  - Create `DevelopmentStrategy` class
  - Enable A/B testing different strategies

### 8.4 Configuration

**Config Underutilized**
- `Config.kt` has one field (`getOutOfJailEarlyFeeAmount`)
- `Monopoly.Config` has one field (`maxRounds`)
- Many hardcoded values that should be configurable:
  - Starting cash ($1500)
  - Go salary ($200)
  - Tax amounts
  - Building limits
  - Rent multipliers

**Strategy Pattern for Player Behavior**
- `Player.isBuying()`, `isPayingGetOutOfJailEarlyFee()`, `developProperties()` define AI behavior
- Marked as `open` for overriding
- Could be extracted to `PlayerStrategy` interface for:
  - Multiple AI implementations
  - Human player implementation
  - Strategy comparison in simulation

### 8.5 Testability

**Missing Test Utilities**
- No builder pattern for complex game states
- No test fixtures for common scenarios (monopoly ownership, bankruptcy situations)
- Each test manually constructs state

**Test Data Class Equality**
- Heavy reliance on data class `equals()`
- Works well but can make failures less readable
- Could benefit from custom matchers

### 8.6 Observability

**Logging**
- All output via `println()`
- Mixed responsibility (game state + debugging)
- No structured logging
- Difficult to:
  - Suppress output in tests
  - Parse game events programmatically
  - Generate statistics

**Event System**
- No event bus or listener pattern
- Statistics collection would require:
  - Event emission on all game state changes
  - Observer pattern for stat collectors
  - Currently would require littering code with stat calls

### 8.7 Performance

**Reflection Usage**
- Sealed class companion objects use reflection to enumerate instances
- Called once during initialization, so minimal impact
- Could be replaced with explicit lists if performance matters

**Immutability**
- Heavy use of mutable state (`var`, `MutableList`, `MutableMap`)
- Appropriate for simulation but limits concurrency
- Not a concern for current single-threaded Monte Carlo approach

---

## 9. Testing & Running

### Build System
**Gradle 8.11.1** with Kotlin DSL

### Dependencies
- `kotlin-stdlib`
- `kotlin-reflect` (for sealed class enumeration)
- `junit-jupiter` 6.0.1 (test framework)

### Commands
```bash
# Run tests
./gradlew test

# Run main game
./gradlew run

# Build
./gradlew build
```

### Main Entry Point
**`main()` in `Monopoly.kt`:**
- Creates 4 players (Elmo, Bert, Ernie, Cookie Monster)
- Uses seeded RNG (seed=1) for reproducibility
- Executes single game
- Prints game log to console

---

## 10. Future Enhancement Recommendations

### For Monte Carlo Simulation

1. **Extract Player Strategy Interface**
   ```kotlin
   interface PlayerStrategy {
       fun decidePurchase(deed: TitleDeed, player: Player): Boolean
       fun developProperties(player: Player, bank: Bank, board: Board)
       fun liquidationPriority(deeds: Map<TitleDeed, Development>): List<TitleDeed>
   }
   ```

2. **Implement Event System**
   ```kotlin
   interface GameEvent
   data class PropertyPurchased(player: Player, deed: TitleDeed) : GameEvent
   data class RentPaid(from: Player, to: Player, amount: Int) : GameEvent
   // ... etc
   ```

3. **Statistics Collector**
   ```kotlin
   class GameStatistics : GameEventListener {
       val landingCounts: Map<Tile, Int>
       val propertyOwnershipDuration: Map<TitleDeed, List<Int>>
       val playerNetWorthOverTime: Map<Player, List<Int>>
   }
   ```

4. **Batch Simulation Runner**
   ```kotlin
   class MonopolySimulator(val config: SimulationConfig) {
       fun runSimulation(iterations: Int): SimulationResults
   }
   ```

5. **House Rules Configuration**
   ```kotlin
   data class HouseRules(
       val freeParkingPot: Boolean = false,
       val doubleGoSalary: Boolean = false,
       val noAuctions: Boolean = false,
       val buildOutOfTurn: Boolean = false
   )
   ```

### For Code Quality

1. **Extract Complex Algorithms**
   - `LiquidationStrategy` class
   - `DevelopmentStrategy` class
   - `RentCalculator` class (consolidate all rent logic)

2. **Improve Type Safety**
   - Generic type parameters instead of `KClass`
   - Sealed interfaces for player actions

3. **Add Structured Logging**
   - Replace `println()` with proper logging framework
   - Support log levels and filtering
   - Enable structured event logging

4. **Create Test Fixtures**
   - Game state builders
   - Common scenario setups
   - Assertion libraries

---

## 11. References

**Official Rules:**
- Hasbro Official Rules: https://www.hasbro.com/common/instruct/00009.pdf
- 2021 Edition ruleset (current implementation basis)

**Property Data:**
- Monopoly Wiki: https://monopoly.fandom.com/wiki/List_of_Monopoly_Properties
- Card listings: https://monopoly.fandom.com/wiki/Chance, https://monopoly.fandom.com/wiki/Community_Chest

**Additional Context:**
- House bidding rules: https://boardgames.stackexchange.com/questions/25411/monopoly-houses-bidding
- Bank money amounts: https://www.monopolyland.com/how-much-money-in-monopoly-set/

---

## Appendix: File Sizes

**Source Files (by size):**
- `Player.kt` - 14,663 bytes (329 lines)
- `Board.kt` - 12,944 bytes (297 lines)
- `Bank.kt` - 12,594 bytes (292 lines)
- `ChanceCard.kt` - 6,519 bytes (162 lines)
- `Tile.kt` - 6,361 bytes (178 lines)
- `Property.kt` - 3,845 bytes (96 lines)
- `CommunityChestCard.kt` - 3,586 bytes (99 lines)
- `TitleDeed.kt` - 3,500 bytes (79 lines)
- `Card.kt` - 1,896 bytes (69 lines)
- `Monopoly.kt` - 1,865 bytes (61 lines)
- `Railroad.kt` - 1,760 bytes (54 lines)
- `Utility.kt` - 1,297 bytes (40 lines)
- `Deck.kt` - 911 bytes (30 lines)
- `Dice.kt` - 898 bytes (33 lines)
- `ColourGroup.kt` - 649 bytes (24 lines)
- `Config.kt` - 453 bytes (9 lines)

**Test Files (by size):**
- `BoardTest.kt` - 21,228 bytes (486 lines)
- `BankTest.kt` - 17,461 bytes (431 lines)
- `PlayerTest.kt` - 15,251 bytes (388 lines)
- `ChanceCardTest.kt` - 11,713 bytes
- `PropertyTest.kt` - 6,150 bytes
- `TitleDeedTest.kt` - 5,954 bytes
- `RailroadTest.kt` - 4,595 bytes
- `CommunityChestCardTest.kt` - 4,332 bytes
- `TileTest.kt` - 4,259 bytes
- `UtilityTest.kt` - 3,918 bytes
- `ColourGroupTest.kt` - 3,329 bytes
- `DeckTest.kt` - 3,213 bytes
- `CardTest.kt` - 2,319 bytes
- `DiceTest.kt` - 2,132 bytes
- `TestUtils.kt` - 2,101 bytes
- `MonopolyTest.kt` - 1,002 bytes

**Total:** ~174KB of source + tests