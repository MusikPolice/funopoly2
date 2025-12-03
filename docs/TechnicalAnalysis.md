# Funopoly2 System Technical Analysis

**Version:** 1.0-SNAPSHOT  
**Last Reviewed:** December 3, 2025  
**Primary Language:** Kotlin 2.2.x  
**Target JVM:** 17

**Supersedes:** `docs/TechnicalAnalysis.md`, `docs/EventSystemGuide.md`

---

## 1. Project Purpose & Current Status

### 1.1 Objective

Funopoly2 is a Monopoly rules engine and simulator intended to support:

- **Accurate base rules implementation** of modern Monopoly (board layout, deeds, cards, jail, development, bankruptcy, etc.).
- **Programmatic simulation** of complete games with deterministic RNG for reproducibility.
- **Event-driven statistics collection** to enable later Monte Carlo and house-rule analysis.

### 1.2 Current Capabilities (From Code)

Implemented and present in code:

- **Core game loop** (`Monopoly.executeGame`) with:
  - Turn/round execution via `Board.executeRound`.
  - End conditions: all but one player bankrupt, or `maxRounds` reached.
- **Domain model:** players, board, tiles, dice, bank, deeds (properties, railroads, utilities), Chance & Community Chest cards, exceptions.
- **Bankruptcy and unmortgaging:**
  - Bankruptcy to bank and player-to-player bankruptcy with mortgage fee handling and cascading bankruptcy.
  - Property unmortgaging at 110% of mortgage value; strategy-based decision (`Player.shouldUnmortgageProperty`).
- **Property development and liquidation:**
  - Even-building rules enforced in `TitleDeed` helpers and `Bank` operations.
  - Greedy development and structured liquidation strategy in `Player`.
- **Event system:**
  - `EventBus`, `GameEvent` (22 event types), `GameEventListener`.
  - Integrated emissions from `Monopoly`, `Board`, `Bank`, `Tile`, and bankruptcy logic in `Player`.
- **Statistics system:**
  - `GameStatistics` listener that consumes all `GameEvent`s.
  - `StatisticsReport` data model with 6 sections.
  - `StatisticsFormatter` producing console text or JSON.
  - Configuration via `Config.collectStatistics` and `StatisticsOutputFormat`.

Not implemented or explicitly TODO in code:

- Property auctions after declined purchases and on bankruptcy returns.
- Player trading / negotiation.
- Rich **player strategy abstraction** (current logic is hard-coded methods on `Player`).
- House rules (Free Parking pot, double GO salary, variable starting cash, etc.).
- Monte Carlo batch runner and cross-game aggregation of stats.
- Structured logging (stdout `println` is used throughout).
- Updating rule implementation to a 2023 ruleset (TODO in `Monopoly.kt`).

---

## 2. Architecture & Code Structure

### 2.1 Package Overview

Root package: `ca.jonathanfritz.monopoly`

High-level structure (from `src/main/kotlin`):

```text
ca.jonathanfritz.monopoly/
├── Monopoly.kt              # Main game orchestrator & entry point
├── Config.kt                # Game configuration (rounds, statistics)
├── Player.kt                # Player state, strategy, liquidation & bankruptcy
├── board/
│   ├── Board.kt             # Board layout, round/turn execution, movement
│   ├── Bank.kt              # Money, deeds, houses/hotels, mortgages
│   ├── Dice.kt              # Dice rolling and previous roll tracking
│   └── Tile.kt              # 40-tile sealed hierarchy and landing behavior
├── card/
│   ├── Card.kt              # Base card types & shared behavior
│   ├── ChanceCard.kt        # 16 Chance cards
│   ├── CommunityChestCard.kt# 17 Community Chest cards
│   └── Deck.kt              # Deck shuffling/draw/discard
├── deed/
│   ├── TitleDeed.kt         # Abstract base for all deeds
│   ├── Property.kt          # 22 buildable properties
│   ├── Railroad.kt          # 4 railroads
│   ├── Utility.kt           # 2 utilities
│   └── ColourGroup.kt       # Property colour groups
├── event/
│   ├── GameEvent.kt         # Sealed class with 22 event types
│   ├── EventBus.kt          # Event distribution to listeners
│   └── GameEventListener.kt # Observer interface
├── statistics/
│   ├── GameStatistics.kt       # Event listener and accumulator
│   ├── StatisticsReport.kt     # Report data model
│   ├── StatisticsFormatter.kt  # Console/JSON formatters
│   └── StatisticsOutputFormat.kt # Output format enum
└── exception/
    ├── BankruptcyException.kt
    ├── InsufficientFundsException.kt
    ├── InsufficientTokenException.kt
    ├── MonopolyOwnershipException.kt
    ├── PropertyDevelopmentException.kt
    └── PropertyOwnershipException.kt
```

### 2.2 Key Design Patterns & Conventions

- **Sealed class hierarchies**
  - `Tile` models all 40 spaces.
  - `TitleDeed` → `Property`, `Railroad`, `Utility`.
  - `Card` and its Chance/CommunityChest variants.
  - `GameEvent` for event types.
- **Enum-like sealed registries**
  - Companion utilities (e.g., `TitleDeed.values`) use reflection to list all instances once at startup.
- **Strategy via overridable methods on `Player`**
  - `isBuying`, `isPayingGetOutOfJailEarlyFee`, `developProperties`, `liquidateAssets`, `shouldUnmortgageProperty` are `open` and intended as extension points, but not factored into a dedicated strategy interface yet.
- **Observer pattern for events and statistics**
  - `EventBus` with pluggable `GameEventListener`s (e.g., `GameStatistics`).
- **Exception-driven rule enforcement**
  - Illegal operations (e.g., building without monopoly, selling non-existent houses, mortgaging developed property) throw domain-specific exceptions.

---

## 3. Core Domain Model

### 3.1 Monopoly

**File:** `Monopoly.kt`

Responsibilities:

- **Game setup** in `init`:
  - Uses `Bank.pay(1500, player, "in starting salary")` to grant starting cash.
  - Initializes all players to `position = 0` (Go).
- **Game loop** (`executeGame`):
  - For rounds `1..config.maxRounds`, calls `board.executeRound(round)`.
  - If exactly one player is not bankrupt, declares winner with reason `"bankruptcy"` and emits `GameEvent.GameEnded`.
  - If `maxRounds` reached, selects a winner by highest `Player.netWorth()` and emits `GameEvent.GameEnded` with reason `"max rounds reached"`.
- **Statistics integration:**
  - If `Config.collectStatistics` is true, constructs an `EventBus`, registers `GameStatistics`, and wires `Bank` and `Board` to that bus.
  - On game end, calls `outputStatistics()` which uses `StatisticsFormatter` to print console or JSON according to `config.statisticsOutputFormat`.

### 3.2 Config

**File:** `Config.kt`

From code (not reproduced here), `Config` includes at least:

- `maxRounds: Int` – maximum round count before forced end.
- `collectStatistics: Boolean` – enables the event bus and `GameStatistics`.
- `statisticsOutputFormat: StatisticsOutputFormat` – `CONSOLE` or `JSON`.
- `getOutOfJailEarlyFeeAmount: Int` – fee used in Board jail logic.

Config is **underutilized** relative to the number of hardcoded constants in the game logic.

### 3.3 Player

**File:** `Player.kt`

State (selected fields):

- `name: String`, `money: Int`, `position: Int` (0–39).
- `deeds: MutableMap<TitleDeed, Development>` – owned deeds and their development state.
- `isBankrupt: Boolean` (private backing, `isBankrupt()` accessor).
- Jail state: `isInJail: Boolean` (setter manages `remainingTurnsInJail`), `remainingTurnsInJail: Int`.
- Inventory: `getOutOfJailFreeCards: MutableList<Card.GetOutOfJailFreeCard>`.
- Optional `eventBus: EventBus?` used only for bankruptcy events.

Important methods (behavior verified in code):

- **Ownership & monopoly:**
  - `isOwner(deedClass)`, `getDevelopment(deedClass)`.
  - `hasMonopoly(colourGroup)` computes ownership vs. `ColourGroup.titleDeeds()`.
- **Wealth & tax:**
  - `netWorth()` = cash + sum of deed prices + building cost of all current developments.
  - `incomeTaxAmount()` = `ceil(min(200, 10% of net worth))`.
- **Jail:**
  - `isPayingGetOutOfJailEarlyFee(amount)` – pays if in jail, no GOOJF card, still has remaining turns, and `money > amount`.
  - `useGetOutOfJailFreeCard()` – consumes a card if in jail.
- **Development:**
  - `developProperties(bank, board)` – greedy development strategy (Section 4.1).
  - `unmortgageProperties(bank, board)` – unmortgage based on `shouldUnmortgageProperty`.
- **Strategy hooks:**
  - `isBuying(deed)` – current rule: buy if `money > deed.price`.
  - `shouldUnmortgageProperty(deed, mortgageValue)` – default true when `money >= 2.2 * mortgageValue`.
- **Liquidation & bankruptcy:**
  - `liquidateAssets(requiredAmount, bank, board)` – 3-phase mortgage/sell strategy (Section 4.2).
  - `declareBankruptcy(bank, board)` – bankruptcy to bank (requires full liquidation; transfers deeds back to bank and emits `GameEvent.PlayerBankrupted` with creditor=`Bank`).
  - `private declareBankruptcy(creditor: Player, bank, board)` – full player-to-player bankruptcy; cash, GOOJF cards, all mortgaged deeds transferred; handles mortgage assumption/unmortgaging and cascading bankruptcy; emits `GameEvent.PlayerBankrupted` with creditor=`Player` or `Bank` depending on outcome.

### 3.4 Board & Tiles

**File:** `board/Board.kt`

- Maintains:
  - `players: List<Player>`.
  - `bank: Bank` (shared with `Monopoly`).
  - `dice: Dice`.
  - `chance: Deck<Card>` and `communityChest: Deck<Card>` pre-populated with 16/17 specific cards.
  - `tiles: List<Tile>` representing the full board including Go, properties, railroads, utilities, taxes, Chance, Community Chest, Jail/GoToJail, Free Parking.
  - `currentRound: Int` to annotate bankruptcy events.
- Core methods:
  - `executeRound(round: Int)` – runs one round:
    - Emits `RoundStarted`/`RoundEnded` events.
    - For each non-bankrupt player:
      - Emits `TurnStarted` / `TurnEnded`.
      - Handles jail escape attempts (`attemptToGetOutOfJail`).
      - Rolls dice (with doubles mechanic and three-doubles-to-jail rule).
      - Moves players via `advancePlayerBy` / `advancePlayerToTile` / `advancePlayerToProperty` / `advancePlayerToRailroad`.
      - Calls `Player.developProperties` then `Player.unmortgageProperties` after each movement.
  - `goToJail(player, reason)` – sets jail state, emits `PlayerSentToJail`, and moves to `Jail` tile.
  - `returnGetOutOfJailFreeCard(card)` – re-inserts card into appropriate deck.

**File:** `board/Tile.kt`

- `sealed class Tile` with variants including:
  - `Go`, `IncomeTax`, `LuxuryTax`, `FreeParking`, `Jail`, `GoToJail`.
  - `Buyable` subclasses: `PropertyBuyable`, `RailroadBuyable`, `UtilityBuyable`.
  - `Chance(side: Int)`, `CommunityChest(side: Int)`.
- Each tile implements `onLanding(player, bank, board, rentOverride, eventBus)` to enforce rules.
- Buyable landing behavior (confirmed in code):
  - Determine owner:
    - If owner is current player: no-op.
    - If owner is other player:
      - If mortgaged: no rent.
      - Otherwise: calculate rent, collect via `Player.pay`, and emit `GameEvent.RentPaid`.
    - If unowned:
      - Calls `player.isBuying(deed)`; on `true`, calls `Bank.sellDeedToPlayer`.
      - Auctions are **not implemented**; there are TODO comments indicating intended auctions in bank/tiles.

### 3.5 Bank

**File:** `board/Bank.kt`

State:

- `availableHouses: Int = 32`, `availableHotels: Int = 12`.
- `money: Int = 20580` (per post-2008 official sets). Comments indicate this is technically unbounded in rules.
- `titleDeeds: MutableList<TitleDeed>` – stock of unsold deeds initialized from `TitleDeed.values`.
- Optional `eventBus: EventBus?` for emitting financial and property events.

Key operations:

- **Cash flow:**
  - `pay(amount, player, reason)` – bank to player transfer; emits `BankPaidPlayer`.
  - `charge(amount, player, board, reason)` – player to bank transfer; if insufficient funds, calls `player.liquidateAssets` and possibly `player.declareBankruptcy(this, board)`; emits `PlayerChargedByBank`.
- **Deeds:**
  - `sellDeedToPlayer(deedClass, player, board)` – validates affordability; moves deed from bank inventory to `player.deeds`; emits `PropertyPurchased`.
  - `mortgageDeed(deedClass, player)` – validates owner and no development; pays player `mortgageValue`; marks `isMortgaged`; emits `PropertyMortgaged`.
  - `unmortgageDeed(deedClass, player, board)` – charges `ceil(mortgageValue * 1.1)`; clears `isMortgaged`; emits `PropertyUnmortgaged`.
  - `transferMortgagedDeeds(deeds)` – returns deed set to bank; has TODO for triggering an immediate auction; no current event emission for transfers.
- **Development:**
  - `sellHouseToPlayer` / `sellHotelToPlayer` – enforce ownership, monopoly, even-building rules, token limits, and affordability; update `Development` and emit `HousePurchased`/`HotelPurchased`.
  - `buyHouseFromPlayer` / `buyHotelFromPlayer` – reverse-operations, including hotel→4 houses; handle even-building rules and token stock; emit `HouseSold`/`HotelSold`.

### 3.6 Deeds & Rent Calculation

**Files:** `deed/TitleDeed.kt`, `Property.kt`, `Railroad.kt`, `Utility.kt`, `ColourGroup.kt`

- `TitleDeed`:
  - Fields: `colourGroup`, `price`, `mortgageValue`, `isBuildable` (abstract), and `calculateRent(owner, board)`.
  - Even-building helpers (Section 4.4): `addingHouseRespectsEvenBuildingRules`, `removingHouseRespectsEvenBuildingRules`, `addingOrRemovingHotelRespectsEvenBuildingRules`.
- `Property` (22 instances):
  - Fields include `buildingCost`, base rent, rent-by-house-count, hotel rent.
  - Rent rules:
    - If mortgaged: 0.
    - If hotel: `rentHotel`.
    - If 0 houses and monopoly: `rentNoHouse * 2`.
    - Else: look up from `houseRents` by `numHouses`.
- `Railroad` (4 instances):
  - Rent based on count of **unmortgaged** railroads: 25/50/100/200.
- `Utility` (2 instances):
  - Rent = previous dice roll total × multiplier.
  - 1 utility: 4×; 2 utilities: 10×.
  - Special Chance cards can override multiplier via a `rentOverride` lambda in board movement/card handling.

### 3.7 Cards & Decks

**Files:** `card/Card.kt`, `ChanceCard.kt`, `CommunityChestCard.kt`, `Deck.kt`

- `Deck<Card>` manages shuffling, drawing, and reinserting cards.
- `Card` sealed class with base `onDraw(player, bank, board)` behavior specialized in Chance and Community Chest subclasses.
- Cards implement all standard 16 Chance and 17 Community Chest effects, including movement, payments between players, bank payouts, jail, and repairs.
- `Board` uses `Card.GoToJail` and various Advance/nearest-utility/railroad cards to drive movement.
- `GameEvent.CardDrawn` is emitted from card drawing paths with `deck` = "Chance" or "Community Chest".

### 3.8 Dice

**File:** `board/Dice.kt`

- Encapsulates two six-sided dice, plus:
  - Tracks `previous: Roll` where `Roll` holds `die1`, `die2`, `amount`, `isDoubles`, `highest`.
  - Used by utilities for rent.

---

## 4. Key Algorithms & Business Logic

### 4.1 Property Development Strategy (`Player.developProperties`)

Summary from implementation:

- Filters owned deeds to developable properties: not already with hotel, type `Property`.
- Derives owned colour groups and filters to those where `hasMonopoly` is true.
- Within each monopoly group, filters to properties where `buildingCost < money` (no liquidation to develop).
- Sorts candidate properties **descending by current rent** (`calculateRent`) as a heuristic for ROI.
- Finds the first candidate that passes even-building rules:
  - If current `numHouses == 4`, uses `addingOrRemovingHotelRespectsEvenBuildingRules`.
  - Else uses `addingHouseRespectsEvenBuildingRules`.
- If a candidate is found, builds **exactly one** building:
  - `numHouses == 4` → `Bank.sellHotelToPlayer`.
  - Otherwise → `Bank.sellHouseToPlayer`.

Characteristics and caveats:

- **Greedy**: always favors the property with highest current rent; no lookahead.
- **Aggressive cash usage**: only requires `buildingCost < money`; no explicit reserve for future rent obligations.
- **Single-building per call**: per dice roll/turn, at most one house or hotel.
- No configuration or pluggable strategy object; overridden only by subclassing `Player`.

### 4.2 Asset Liquidation Strategy (`Player.liquidateAssets`)

`liquidateAssets(requiredAmount, bank, board)` is a 3-phase looped algorithm:

1. **Phase 1 – Mortgage non-monopoly properties**
   - Filter deeds not in a monopoly.
   - Within those, select eligible deeds via `selectDeedsToMortgage`:
     - Exclude already mortgaged.
     - Exclude developed properties (houses/hotel present).
     - Sort descending by "distance from monopoly" (favor properties farthest from completing a set), then ascending by `mortgageValue`.
   - Mortgage each in order using `Bank.mortgageDeed` until `money >= requiredAmount` or no more candidates.

2. **Phase 2 – Sell buildings**
   - Filter deeds that are `Property` and have houses or hotel.
   - Sort by **ascending rent** (sell least valuable income sources first).
   - For each, check even-building legality:
     - If hotel: `addingOrRemovingHotelRespectsEvenBuildingRules`.
     - Else: `removingHouseRespectsEvenBuildingRules`.
   - Sell using `Bank.buyHotelFromPlayer` (hotels) or `Bank.buyHouseFromPlayer` (houses) until `money >= requiredAmount` or stock exhausted.

3. **Phase 3 – Mortgage monopolies**
   - Now consider deeds in colour groups where the player still has a monopoly, again using `selectDeedsToMortgage`.
   - Mortgage until funds are sufficient or no more deeds.

4. **Looping and termination**
   - Phases 2 and 3 are wrapped in a `do { ... } while (!hasFullyLiquidatedAssets())` loop.
   - `hasFullyLiquidatedAssets()` returns true if there are no deeds, or all remaining are mortgaged and undeveloped.
   - If still short of `requiredAmount` after full liquidation, prints a message and throws `BankruptcyException`.

This code is non-trivial (~60 lines) and commented as a candidate for refactoring into a separate strategy/utility.

### 4.3 Even Building Rules

Implemented in `TitleDeed` helpers and enforced in `Bank` and `Player` methods:

- Houses must be built as evenly as possible across a monopoly.
- House addition is legal if:
  - All properties in the monopoly have same house count, **or**
  - The target property currently has the **minimum** house count.
- House removal is legal only from the property with the **maximum** count.
- Hotels require all properties in the monopoly have 4 houses or a hotel; analogous constraints apply when selling hotels.

These constraints are consulted by:

- `Bank.sellHouseToPlayer`, `Bank.sellHotelToPlayer`.
- `Bank.buyHouseFromPlayer`, `Bank.buyHotelFromPlayer`.
- `Player.developProperties` when choosing a candidate property.

### 4.4 Jail & Turn Flow

From `Board.executeRound` and helpers:

- Players in jail at turn start:
  - Attempt to use a GOOJF card (`Player.useGetOutOfJailFreeCard`) first; if used, card is returned to the appropriate deck and a `PlayerLeftJail` event is emitted with method `"used card"`.
  - Else, if `Player.isPayingGetOutOfJailEarlyFee(config.getOutOfJailEarlyFeeAmount)` returns true, `Bank.charge` is called with that fee, jail status is cleared, and `PlayerLeftJail` is emitted with method `"paid fee"`.
- Rolling:
  - Dice rolled via `Dice.roll()`; each roll emits `GameEvent.DiceRolled`.
  - If doubles are rolled while in jail, the player is released immediately, `PlayerLeftJail` emitted with method `"rolled doubles"`, and **no extra turn** is granted (doubles counter forced to 3).
  - For non-jail doubles, up to 3 extra rolls are allowed; on the third consecutive doubles the player goes directly to jail via `goToJail("three consecutive doubles")` and their turn ends.

### 4.5 Bankruptcy Logic

- **To bank** (`Player.declareBankruptcy(bank, board)`):
  - Requires `hasFullyLiquidatedAssets() == true`; otherwise throws an `IllegalStateException`.
  - Charges remaining money to bank (via `Bank.charge`).
  - Returns any GOOJF cards to decks.
  - Calls `Bank.transferMortgagedDeeds` with all remaining deeds and clears `deeds`.
  - Marks `isBankrupt = true`, prints a message, and emits `GameEvent.PlayerBankrupted` with creditor=`Bank` and `round = board.currentRound`.

- **To another player** (`Player.declareBankruptcy(creditor: Player, bank, board)`):
  - Also requires prior full liquidation.
  - Transfers remaining cash and all GOOJF cards to creditor.
  - Calculates **total mortgage fees** creditor will owe:
    - For each mortgaged deed, if `creditor.shouldUnmortgageProperty` returns true, assumes creditor will unmortgage (110% fee).
    - Otherwise charges a 10% mortgage assumption fee.
  - If creditor cannot pay the total fees, attempts `creditor.liquidateAssets(totalMortgageFees, bank, board)`. On `BankruptcyException`:
    - Logs cascading bankruptcy.
    - Creditor declares bankruptcy to the bank.
    - Original debtor then also bankrupts to the bank (money charged, deeds returned, cards returned), and a `PlayerBankrupted` event is emitted for debtor→bank.
  - If creditor can afford fees:
    - Transfers each deed to `creditor.deeds`.
    - For each mortgaged deed:
      - If creditor strategy indicates unmortgage, calls `Bank.unmortgageDeed` (emits `PropertyUnmortgaged`).
      - Else charges 10% assumption fee via `Bank.charge`.
  - Finally clears debtor deeds, marks bankrupt, prints, and emits `GameEvent.PlayerBankrupted` with creditor=`Player`.
  - Attempting to transfer developed properties (houses/hotels > 0) throws `PropertyDevelopmentException`, enforcing the expectation that liquidation fully cleared development first.

---

## 5. Event System & Statistics

### 5.1 Event Model (`event` package)

- `GameEvent` is a sealed class grouping **22 event types** into categories:
  - **Movement:** `RoundStarted`, `TurnStarted`, `DiceRolled`, `PlayerMoved`, `TileLanded`, `TurnEnded`, `RoundEnded`.
  - **Financial:** `BankPaidPlayer`, `PlayerChargedByBank`, `RentPaid`.
  - **Property:** `PropertyPurchased`, `PropertyMortgaged`, `PropertyUnmortgaged`.
  - **Development:** `HousePurchased`, `HotelPurchased`, `HouseSold`, `HotelSold`.
  - **Jail:** `PlayerSentToJail`, `PlayerLeftJail` (`method` includes "rolled doubles" / "paid fee" / "used card").
  - **Cards:** `CardDrawn` (`deck` field is "Chance" or "Community Chest").
  - **Bankruptcy:** `PlayerBankrupted` (creditor is `Player` or `Bank`), `AssetTransferred` (currently unused by statistics, available for future granularity).
  - **Game lifecycle:** `GameEnded`.

### 5.2 EventBus

`EventBus` is a simple synchronous dispatcher:

- `register(listener)`, `unregister(listener)` mutate an internal list.
- `emit(event)` iterates listeners in registration order and calls `onEvent`; listener exceptions are caught and logged to `System.err`.
- No thread safety or asynchronous behavior; the system is single-threaded by design.

Integration points (verified in constructors):

- `Monopoly` optionally constructs or accepts an `EventBus` and wires it to `Bank` and `Board`.
- `Board` and `Tile` use `eventBus?.emit(...)` consistently, so **disabling** the event system is equivalent to passing `null` and has minimal overhead.

### 5.3 Statistics Listener (`GameStatistics`)

`GameStatistics` implements `GameEventListener` and maintains in-memory collections:

- Movement/landing, dice, financial, property, development, jail, card, and bankruptcy data structures.
- A small amount of lifecycle state (game started/ended, total rounds, winner, endReason).

`onEvent` dispatches each `GameEvent` to type-specific handlers that populate the relevant collections. Notably:

- `TileLanded` normalizes tile names such that buyable tiles are keyed by deed class name, while non-buyable by tile class name.
- Financial events are converted to domain-specific records (`BankPayment`, `BankCharge`, `RentTransaction`).
- Property and development events build transaction lists used later for per-player and per-colour-group aggregations.
- `PlayerBankrupted` is the sole driver for bankruptcy statistics; `AssetTransferred` is intentionally ignored for now.

### 5.4 Statistics Snapshot & Report

- `snapshot(): StatisticsSnapshot` provides a read-only view of current statistics with primitive and collection fields, suitable for testing or mid-game inspection.
- `generateReport(): StatisticsReport` builds a higher-level report:
  - Derives list of **all players** that appear in any recorded transaction or event (rent, purchases, go passings, dice, jail, bankruptcies).
  - Reconstructs **final property ownership** by applying purchases and `PlayerBankrupted` events (to players and bank); infers monopolies accordingly.
  - Aggregates counts and derived metrics:
    - `GameSummary` – rounds, winner, endReason, total players, bankruptcies.
    - `PlayerStatistics` – rent paid/collected, property purchases, lists of purchased/obtained-via-bankruptcy properties, development counts, GO passes, doubles, jail visits, bankruptcy round, and color-group monopolies.
    - `PropertyStatistics` – total purchases, mortgages, unmortgages, most expensive purchase, purchases per `ColourGroup`.
    - `FinancialSummary` – totals and largest rent transaction plus average rent amount.
    - `MovementStatistics` – dice roll counts/averages, doubles count, tile landing frequencies, most/least landed tile.
    - `DevelopmentStatistics` – counts of houses/hotels built and sold, development transaction counts per `ColourGroup`, and most-developed group.

### 5.5 Formatting & Output

`StatisticsFormatter` has two main entry points:

- `formatConsole(report)` – human-readable multi-section text using box-style characters and section headers.
- `formatJson(report)` – manual JSON string builder with nested structures for game summary, player statistics, financial summary, property statistics (subset), movement, and development statistics.

Output selection is controlled by `Config.statisticsOutputFormat` (enum `CONSOLE` or `JSON`).

### 5.6 Configuration Toggle

Statistics are **opt-in**:

- `Config.collectStatistics == false`:
  - `Monopoly` constructs with `eventBus = null`.
  - `gameStatistics` is `null`; `outputStatistics` returns immediately.
- `Config.collectStatistics == true`:
  - `Monopoly` constructs an `EventBus` and `GameStatistics`, registers the listener, and shares the bus with `Bank` and `Board`.

This design keeps the base rules engine usable without statistics overhead.

---

## 6. Testing, Tooling & Build

### 6.1 Build & Dependencies

- **Build system:** Gradle (Kotlin DSL), version 8.x.
- **Test framework:** JUnit 5 (junit-jupiter).
- **Kotlin reflection:** Used primarily for sealed-class instance enumeration of deeds and tiles.

Typical commands (from `build.gradle.kts`):

- `./gradlew test` – run test suite.
- `./gradlew run` – run the main entry point (single game with fixed RNG seed).
- `./gradlew build` – full build.

### 6.2 Test Coverage (Qualitative)

From the test package names and file list (no exact counts asserted here):

- **Core engine tests:**
  - `MonopolyTest`, `PlayerTest`, `BoardTest`, `DiceTest`, `TileTest`, plus deed and card tests.
- **Bank and deed tests:**
  - `BankTest`, `PropertyTest`, `RailroadTest`, `UtilityTest`, `TitleDeedTest`, `ColourGroupTest`.
- **Card tests:**
  - `CardTest`, `ChanceCardTest`, `CommunityChestCardTest`, `DeckTest`, helper `CardTestUtils`.
- **Event and statistics tests:**
  - `event/EventBusTest`, `event/EventEmissionIntegrationTest`.
  - `statistics/GameStatisticsTest`, `statistics/GameStatisticsIntegrationTest`, `MonopolyStatisticsIntegrationTest`.

The suite appears to cover:

- Correctness of card and tile behavior.
- Movement rules, dice behavior, and jail edge cases.
- Development and even-building constraints.
- Bankruptcy and liquidation paths, including exceptions.
- Event emission for major flows.
- End-to-end statistics collection and output formatting.

---

## 7. Technical Debt & Known Gaps

This section lists issues that are clearly visible in code or TODO comments, not speculative future work.

### 7.1 Rules & Gameplay

- **Property auctions** are not implemented, despite explicit TODOs in `Monopoly.kt`, `Tile.kt`, and `Bank.transferMortgagedDeeds`.
- **Player trading** is entirely absent (no API or logic for negotiated exchanges).
- **House rules** are not supported; many potential variants are hard-coded to standard rules.
- **Bank money limit** is modeled with a finite `money` field, despite rules stating the bank never runs out; whether this matters in practice is unclear without stress tests.

### 7.2 Strategy Abstraction

- `Player` encodes both **rules** and **AI strategy** in the same class.
- No explicit `PlayerStrategy` interface exists; custom strategies require subclassing `Player` and overriding several methods.
- Development, purchase, and liquidation logic are not easily swappable or composable.

### 7.3 Configuration Underuse

- Many important game parameters remain hard-coded:
  - Starting cash ($1500), GO salary ($200), tax amounts, building limits, rent multipliers.
- `Config` currently only exposes a small subset (max rounds, statistics toggles, jail fee).

### 7.4 Observability & Logging

- Event system is present and well-integrated, but **general logging** still uses `println` on stdout.
- No structured log levels or pluggable logger.
- Tests may rely on console output for verification, which can make failures noisy.

### 7.5 Algorithm Complexity

- `Player.liquidateAssets` and `Player.developProperties` are complex, with nested functional transformations and multiple passes:
  - Harder to reason about or reuse for alternative strategies.
  - TODO comments acknowledge the need for tidying.

### 7.6 Concurrency & Scalability

- Entire system is intentionally **single-threaded**.
- `EventBus` and stateful classes (`Player`, `Bank`, `Board`, `Deck`, etc.) are **not thread-safe**.
- For Monte Carlo-style parallel simulations, each game instance must be fully isolated.

---

## 8. Future Directions (Grounded in Current Design)

These are natural extensions consistent with existing TODOs and architecture; they do *not* claim to be implemented.

### 8.1 Strategy Abstraction

- Introduce a `PlayerStrategy` interface to decouple decision-making from state:
  - Purchase decisions, development ordering, liquidation priority, jail fee vs. staying, unmortgage behavior.
- Allow plugging different strategies per player and across simulations.

### 8.2 Auctions & Trading

- Implement property auctions for:
  - Declined purchases on landing.
  - Deeds returned to bank on bankruptcy.
- Add trading API on `Board`/`Monopoly` to allow inter-player negotiation.

### 8.3 House Rules & Simulation Parameters

- Extend `Config` with house-rule toggles and numeric parameters:
  - Free Parking pot behavior, double GO salary, alternative starting cash, alternative bank stock, variable tax rules.
- Parameterize building supplies, bank money behavior, and card decks for scenario testing.

### 8.4 Monte Carlo Runner & Aggregated Stats

- Build a `Simulator` component that:
  - Runs many independent games, each with its own `Monopoly`, `Board`, `Bank`, `EventBus`, `GameStatistics`.
  - Aggregates `StatisticsReport`s into higher-level distributions (win rate per strategy, average rounds, tile landing distributions, etc.).

### 8.5 Logging & Tooling

- Replace core `println` calls with a simple logging abstraction.
- Optionally integrate with the event system to emit structured logs for critical state changes.

---

## 9. Maintenance Notes

- **Single source of truth for architecture:** this document should be treated as canonical over the older `TechnicalAnalysis.md` and `EventSystemGuide.md`. When code changes, update this file first.
- **Tests as specification:** for tricky rules (edge cases with doubles, jail interactions, bankruptcy cascades), prefer reading the relevant tests alongside this document to understand intended behavior.
- **Statistics evolution:** the current event and statistics design is flexible enough to support additional listeners (e.g., replay recorder, visualization) without touching core game logic; any new observability feature should try to build on `EventBus` rather than adding more ad-hoc logging.
