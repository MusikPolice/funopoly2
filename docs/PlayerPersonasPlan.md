# Player Personas Technical Plan

**Version:** 1.0  
**Created:** December 3, 2025  
**Status:** DRAFT - Awaiting Review

---

## 1. Executive Summary

This plan describes how to introduce **Player Personas** to the Funopoly2 Monopoly implementation. Personas will give players distinct decision-making behaviors for property purchases, development strategies, cash management, and auction participation. This work is a **prerequisite** for implementing property auctions, as players need agency to decline purchases and intelligent bidding strategies.

### Goals

1. **Player Agency**: Allow players to decline property purchases (currently they always buy if affordable)
2. **Property Valuation**: Give players a way to assess what a property is "worth" to them for auction bidding
3. **Diverse Behaviors**: Model 7 distinct Monopoly player archetypes using Sesame Street characters
4. **Extensibility**: Design for easy addition of new personas and strategies in the future
5. **Testability**: Maintain comprehensive test coverage and deterministic behavior

### Non-Goals

- Implementing property auctions (separate feature, depends on this work)
- Player trading/negotiation (future work)
- Machine learning or adaptive strategies (future work)
- Multi-threaded strategy evaluation (system is single-threaded by design)

---

## 2. Current State Analysis

### 2.1 Existing Strategy Hooks

The `Player` class already has several `open` methods intended as strategy extension points:

- **`isBuying(deed: TitleDeed): Boolean`** (line 147)
  - Current: Always returns `money > deed.price`
  - TODO comment acknowledges need for more sophisticated logic
  
- **`isPayingGetOutOfJailEarlyFee(amount: Int): Boolean`** (line 117)
  - Current: Pays if in jail, no GOOJF card, turns remaining, and `money > amount`
  - TODO comment suggests considering highest rent on board
  
- **`shouldUnmortgageProperty(deed: TitleDeed, mortgageValue: Int): Boolean`** (line 151)
  - Current: Returns `money >= mortgageValue * 2.2`
  - Used during bankruptcy asset transfers
  
- **`developProperties(bank: Bank, board: Board)`** (line 156)
  - Current: Greedy strategy - builds on highest-rent property that respects even-building rules
  - TODO comment notes aggressive cash usage (no reserve for rent obligations)
  - Not marked `open` - would need to be made overridable

- **`liquidateAssets(requiredAmount: Int, bank: Bank, board: Board)`** (line 218)
  - Current: 3-phase algorithm (mortgage non-monopolies → sell buildings → mortgage monopolies)
  - TODO comment acknowledges need for tidying
  - Not marked `open` - would need to be made overridable

### 2.2 Limitations of Current Design

1. **Tight Coupling**: Strategy logic is embedded directly in `Player` class
2. **Limited Extensibility**: Overriding behavior requires subclassing and duplicating state management
3. **No Property Valuation**: No concept of "how much is this property worth to me?"
4. **Binary Purchase Decision**: Can't express "I want this but not at full price" (needed for auctions)
5. **No Cash Buffer Strategy**: Development and liquidation don't consider safety reserves
6. **Hard-Coded Priorities**: Liquidation order and development preferences are fixed

### 2.3 Integration Points

Strategy decisions are invoked from:

- **`Tile.Buyable.onLanding`** → calls `player.isBuying(deed)` to decide purchase
- **`Board.executeRound`** → calls `player.developProperties()` after each movement
- **`Board.executeRound`** → calls `player.unmortgageProperties()` after development
- **`Board.attemptToGetOutOfJail`** → calls `player.isPayingGetOutOfJailEarlyFee()`
- **`Player.pay`** and **`Bank.charge`** → trigger `player.liquidateAssets()` on insufficient funds
- **`Player.declareBankruptcy(creditor: Player)`** → calls `creditor.shouldUnmortgageProperty()` for each deed

---

## 3. Proposed Architecture

### 3.1 Design Principles

1. **Strategy Pattern**: Extract decision-making into a separate `PlayerStrategy` interface
2. **Composition over Inheritance**: `Player` holds a strategy instance rather than being subclassed
3. **Stateless Strategies**: Strategy instances hold no mutable state; all decisions based on parameters passed to methods
4. **Reusable Strategies**: Single strategy instance can be shared across multiple players/games
5. **Minimal Player Changes**: Keep existing `Player` API stable; strategies are internal detail
6. **Backward Compatibility**: Default strategy matches current behavior exactly

### 3.2 Core Components

```
ca.jonathanfritz.monopoly.strategy/
├── PlayerStrategy.kt           # Strategy interface
├── DefaultStrategy.kt          # Current behavior (baseline)
├── PropertyValuation.kt        # Value assessment result
├── CashReservePolicy.kt        # Cash buffer management
└── persona/
    ├── SlumlordStrategy.kt     # Oscar the Grouch
    ├── HighRentStrategy.kt     # Count von Count
    ├── ImpulsiveStrategy.kt    # Elmo
    ├── ConservativeStrategy.kt # Big Bird
    ├── GamblerStrategy.kt      # Cookie Monster
    ├── CalculatingStrategy.kt  # Bert
    └── ChaoticStrategy.kt      # Ernie
```

### 3.3 PlayerStrategy Interface

```kotlin
package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Defines decision-making behavior for a Monopoly player.
 * 
 * All methods receive full game state (player, bank, board) to enable
 * sophisticated strategies. The Board contains all players, allowing strategies
 * to examine opponent states (cash, properties, developments) for competitive
 * decisions like blocking monopolies or strategic bidding.
 * 
 * Implementations MUST be stateless - all decisions based purely on method parameters.
 * This ensures strategies are reusable across players and games, and simplifies testing.
 * 
 * Strategies can implement "dynamic" behavior by evaluating relative game position
 * on each decision (e.g., comparing player.netWorth() to opponents' net worth to
 * determine if "winning" or "losing" and adjusting risk tolerance accordingly).
 * 
 * Random strategies (e.g., ImpulsiveStrategy, ChaoticStrategy) should accept a Random
 * instance in their constructor for deterministic testing, but should not maintain
 * mutable state based on previous decisions.
 */
interface PlayerStrategy {
    
    // ===== Property Acquisition =====
    
    /**
     * Decides whether to purchase an unowned property at full price.
     * 
     * @param deed The property being offered
     * @param player The player making the decision
     * @param bank Current bank state
     * @param board Current board state
     * @return true to purchase at full price, false to decline (triggers auction)
     */
    fun shouldBuyProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board
    ): Boolean
    
    /**
     * Calculates the next bid amount for a property at auction.
     * 
     * Called iteratively during auction as bidding progresses. Allows strategies
     * to incrementally increase bids without revealing their maximum upfront.
     * 
     * @param deed The property being auctioned
     * @param currentBid The current highest bid (or starting price if first round)
     * @param player The player making the decision
     * @param bank Current bank state
     * @param board Current board state
     * @return Next bid amount (must be > currentBid), or null to drop out of auction
     */
    fun calculateBidIncrease(
        deed: TitleDeed,
        currentBid: Int,
        player: Player,
        bank: Bank,
        board: Board
    ): Int?
    
    /**
     * Assesses the value of a property to this player.
     * 
     * Used for purchase decisions, auction bidding, and future trading.
     * 
     * @param deed The property to value
     * @param player The player making the assessment
     * @param bank Current bank state
     * @param board Current board state
     * @return PropertyValuation with strategic value and reasoning
     */
    fun valuateProperty(
        deed: TitleDeed,
        player: Player,
        bank: Bank,
        board: Board
    ): PropertyValuation
    
    // ===== Cash Management =====
    
    /**
     * Determines the minimum cash reserve this player wants to maintain.
     * 
     * Used to avoid over-developing or over-bidding.
     * 
     * @param player The player
     * @param board Current board state
     * @return Minimum cash to keep on hand
     */
    fun getMinimumCashReserve(
        player: Player,
        board: Board
    ): Int
    
    /**
     * Decides whether to pay the early jail release fee.
     * 
     * @param feeAmount The fee to pay (typically $50)
     * @param player The player in jail
     * @param board Current board state
     * @return true to pay and leave jail, false to stay and roll for doubles
     */
    fun shouldPayJailFee(
        feeAmount: Int,
        player: Player,
        board: Board
    ): Boolean
    
    // ===== Property Development =====
    
    /**
     * Selects which property to develop next, if any.
     * 
     * Called after each dice roll. Can return null to skip development this turn.
     * 
     * @param developableProperties Properties eligible for development (monopolies, affordable, even-building legal)
     * @param player The player making the decision
     * @param bank Current bank state (house/hotel availability)
     * @param board Current board state
     * @return The property to develop, or null to skip
     */
    fun selectPropertyToDevelop(
        developableProperties: List<Property>,
        player: Player,
        bank: Bank,
        board: Board
    ): Property?
    
    // ===== Asset Liquidation =====
    
    /**
     * Decides whether to unmortgage a property when cash is available.
     * 
     * @param deed The mortgaged property
     * @param unmortgageCost The cost to unmortgage (110% of mortgage value)
     * @param player The player making the decision
     * @param board Current board state
     * @return true to unmortgage, false to keep mortgaged
     */
    fun shouldUnmortgageProperty(
        deed: TitleDeed,
        unmortgageCost: Int,
        player: Player,
        board: Board
    ): Boolean
    
    /**
     * Prioritizes which properties to mortgage first when liquidating assets.
     * 
     * @param mortgageableProperties Properties eligible for mortgaging (owned, unmortgaged, undeveloped)
     * @param player The player liquidating
     * @param board Current board state
     * @return Properties in order of preference to mortgage (first = mortgage first)
     */
    fun prioritizeMortgages(
        mortgageableProperties: List<TitleDeed>,
        player: Player,
        board: Board
    ): List<TitleDeed>
    
    /**
     * Prioritizes which buildings to sell first when liquidating assets.
     * 
     * @param developedProperties Properties with houses/hotels that can be sold
     * @param player The player liquidating
     * @param board Current board state
     * @return Properties in order of preference to sell from (first = sell first)
     */
    fun prioritizeBuildingSales(
        developedProperties: List<Property>,
        player: Player,
        board: Board
    ): List<Property>
}
```

### 3.4 PropertyValuation Data Class

```kotlin
package ca.jonathanfritz.monopoly.strategy

import ca.jonathanfritz.monopoly.deed.TitleDeed

/**
 * Represents a player's assessment of a property's strategic value.
 */
data class PropertyValuation(
    val deed: TitleDeed,
    val strategicValue: Int,
    val reasoning: String
) {
    companion object {
        // Cache for traffic multipliers (immutable, can be computed once)
        private val trafficMultiplierCache = mutableMapOf<TitleDeed, Double>()
        
        /**
         * Calculates base value using simple heuristics.
         * 
         * Factors:
         * - Price-to-rent ratio (fundamental value)
         * - Traffic multiplier (high-traffic positions get boost)
         * - Monopoly proximity (how close to completing set)
         * - Current development state
         * 
         * High-traffic positions (cards/rules drive extra landings):
         * - Properties just after GO (Mediterranean, Baltic, Oriental, etc.)
         * - Properties just after Jail (St. Charles Place, States Ave, Virginia Ave)
         * - Properties near Railroads (Chance/CC cards send players to railroads)
         * 
         * Note: This method caches traffic multipliers (immutable) but recalculates
         * monopoly and development values (depend on current game state).
         */
        fun calculateBaseValue(
            deed: TitleDeed,
            player: Player,
            board: Board
        ): Int {
            // Start with deed price as baseline
            var value = deed.price
            
            // Apply traffic multiplier based on board position (cached)
            val trafficMultiplier = getTrafficMultiplier(deed)
            value = (value * trafficMultiplier).toInt()
            
            // Adjust for monopoly proximity (game state dependent)
            val monopolyBonus = calculateMonopolyBonus(deed, player)
            value += monopolyBonus
            
            // Adjust for current development potential (game state dependent)
            val developmentValue = calculateDevelopmentValue(deed, player)
            value += developmentValue
            
            return value
        }
        
        /**
         * Returns traffic multiplier for a property based on its position.
         * 
         * High-traffic zones:
         * - 1.2x: Properties 1-9 (post-GO, includes Mediterranean through Connecticut)
         * - 1.3x: Properties 11-14 (post-Jail, includes St. Charles through Virginia)
         * - 1.1x: Properties near railroads (Reading, Pennsylvania, B&O, Short Line)
         * - 1.0x: All other properties
         * 
         * Cached because traffic multipliers are immutable (based only on board position).
         */
        private fun getTrafficMultiplier(deed: TitleDeed): Double {
            return trafficMultiplierCache.getOrPut(deed) {
                // Implementation based on board position
                // This is computed once per deed and cached forever
            }
        }
    }
}
```

### 3.5 Player Class Modifications

```kotlin
// In Player.kt

open class Player(
    val name: String,
    var money: Int = 0,
    var position: Int = 0,
    val deeds: MutableMap<TitleDeed, Development> = mutableMapOf(),
    private var isBankrupt: Boolean = false,
    private val getOutOfJailFreeCards: MutableList<Card.GetOutOfJailFreeCard> = mutableListOf(),
    private val eventBus: EventBus? = null,
    
    // NEW: Strategy instance (defaults to current behavior)
    private val strategy: PlayerStrategy = DefaultStrategy()
) {
    
    // MODIFIED: Delegate to strategy
    fun isBuying(deed: TitleDeed, bank: Bank, board: Board): Boolean = 
        strategy.shouldBuyProperty(deed, this, bank, board)
    
    // MODIFIED: Remove 'open', delegate to strategy
    fun isPayingGetOutOfJailEarlyFee(amount: Int, board: Board): Boolean =
        strategy.shouldPayJailFee(amount, this, board)
    
    // MODIFIED: Remove 'open', delegate to strategy
    fun shouldUnmortgageProperty(
        deed: TitleDeed,
        mortgageValue: Int,
        board: Board
    ): Boolean {
        val unmortgageCost = ceil(mortgageValue * 1.1).toInt()
        return strategy.shouldUnmortgageProperty(deed, unmortgageCost, this, board)
    }
    
    // MODIFIED: Delegate to strategy for property selection
    fun developProperties(bank: Bank, board: Board) {
        // Existing logic to filter developable properties
        val developableDeeds = /* ... existing filtering ... */
        
        // NEW: Delegate to strategy for selection
        val propertyToDevelop = strategy.selectPropertyToDevelop(
            developableDeeds, this, bank, board
        )
        
        propertyToDevelop?.let { property ->
            // Existing logic to build house or hotel
            when (getDevelopment(property::class).numHouses) {
                4 -> bank.sellHotelToPlayer(property::class, this, board)
                else -> bank.sellHouseToPlayer(property::class, this, board)
            }
        }
    }
    
    // MODIFIED: Delegate to strategy for prioritization
    fun liquidateAssets(
        requiredAmount: Int,
        bank: Bank,
        board: Board
    ) {
        // Phase 1: Mortgage non-monopolies
        val nonMonopolyDeeds = deeds
            .filterNot { hasMonopoly(it.key.colourGroup) }
            .keys.toList()
        
        val mortgagePriority = strategy.prioritizeMortgages(
            nonMonopolyDeeds, this, board
        )
        
        for (deed in mortgagePriority) {
            if (money >= requiredAmount) return
            bank.mortgageDeed(deed::class, this)
        }
        
        // Phase 2: Sell buildings
        val developedProperties = deeds
            .filter { it.value.hasHotel || it.value.numHouses > 0 }
            .keys.filterIsInstance<Property>()
        
        val sellPriority = strategy.prioritizeBuildingSales(
            developedProperties, this, board
        )
        
        // ... continue with existing even-building logic ...
        
        // Phase 3: Mortgage monopolies
        // ... similar delegation to strategy ...
    }
}
```

---

## 4. Persona Specifications

Each persona will be implemented as a concrete `PlayerStrategy` with distinct behavior patterns.

### 4.1 DefaultStrategy (Baseline)

**Purpose**: Matches current `Player` behavior exactly. Used for regression testing.

**Behavior**:
- **shouldBuyProperty**: `money > deed.price`
- **calculateMaxBid**: Not applicable (auctions not yet implemented)
- **valuateProperty**: Base value = deed price
- **getMinimumCashReserve**: 0 (no reserve)
- **shouldPayJailFee**: `money > feeAmount && remainingTurnsInJail > 0`
- **selectPropertyToDevelop**: Highest current rent, respects even-building
- **shouldUnmortgageProperty**: `money >= unmortgageCost * 2.0`
- **prioritizeMortgages**: Farthest from monopoly, then lowest mortgage value
- **prioritizeBuildingSales**: Lowest current rent first

### 4.2 SlumlordStrategy (Oscar the Grouch)

**Archetype**: Low-cost, high-volume returns

**Behavior**:
- **shouldBuyProperty**: 
  - Prefers Brown/Light Blue (cheap sets)
  - Only buy if `money > deed.price * 1.5` (maintains buffer)
  - Reject expensive properties (Green/Dark Blue) unless completing monopoly
  
- **calculateBidIncrease**: 
  - Internal max: 80% of deed price for cheap properties, 50% for expensive
  - Increment by $10-20 per round (small increments to avoid overpaying)
  - Drop out if currentBid exceeds internal max
  - +20% to internal max if completes monopoly
  
- **valuateProperty**:
  - Cheap properties: base value × 1.5
  - Expensive properties: base value × 0.5
  - Monopoly completion: +50%
  
- **getMinimumCashReserve**: $200 (one GO salary)

- **selectPropertyToDevelop**:
  - Prioritize cheap color groups
  - Build to exactly 4 houses (avoid hotels)
  - Stop if cash < reserve + highest rent on board
  
- **shouldUnmortgageProperty**: Only if `money > unmortgageCost * 3.0` (very conservative)

- **prioritizeMortgages**: Expensive properties first (Green, Dark Blue, Red)

- **prioritizeBuildingSales**: Sell hotels first (convert to 4 houses), then expensive properties

### 4.3 HighRentStrategy (Count von Count)

**Archetype**: Big rents, big numbers

**Behavior**:
- **shouldBuyProperty**:
  - Prioritize Green/Dark Blue (expensive sets)
  - Buy if `money > deed.price * 1.2`
  - Always buy if completes monopoly
  
- **calculateBidIncrease**:
  - Internal max: 120% of deed price, 150% if completes monopoly
  - Aggressive increments: $20-50 per round (intimidate opponents)
  - Will bid even if exceeds current cash (forces liquidation)
  - Drop out only when currentBid exceeds internal max
  
- **valuateProperty**:
  - Expensive properties: base value × 1.5
  - Cheap properties: base value × 0.7
  - Hotel rent potential: +30%
  
- **getMinimumCashReserve**: $300 (medium buffer)

- **selectPropertyToDevelop**:
  - Prioritize expensive color groups
  - Rush to hotels (build 4 houses then hotel ASAP)
  - Willing to spend down to reserve
  
- **shouldUnmortgageProperty**: `money > unmortgageCost * 1.8`

- **prioritizeMortgages**: Cheap properties first (Brown, Light Blue)

- **prioritizeBuildingSales**: Cheap properties first (preserve expensive developments)

### 4.4 ImpulsiveStrategy (Elmo)

**Archetype**: Fun over strategy, random decisions

**Constructor**: `ImpulsiveStrategy(rng: Random = Random.Default)`

**Behavior**:
- **shouldBuyProperty**: 
  - 90% chance to buy if affordable
  - 10% random decline (even if good deal)
  
- **calculateBidIncrease**:
  - Internal max: random between 50% and 150% of deed price (set at auction start)
  - Random increments: $5-100 per round (wildly inconsistent)
  - No consideration of monopoly or strategic value
  - Sometimes drops out early, sometimes bids to the max
  
- **valuateProperty**:
  - Random value between 0.5× and 2.0× base value
  - Changes each time (inconsistent)
  
- **getMinimumCashReserve**: $50 (minimal)

- **selectPropertyToDevelop**:
  - Random selection from affordable properties
  - No preference for ROI or color group
  
- **shouldUnmortgageProperty**: 50% chance if affordable

- **prioritizeMortgages**: Random order (using `rng.shuffle()`)

- **prioritizeBuildingSales**: Random order (using `rng.shuffle()`)

### 4.5 ConservativeStrategy (Big Bird)

**Archetype**: Safety and stability

**Behavior**:
- **shouldBuyProperty**:
  - Prefer mid-priced properties (Orange, Red, Yellow)
  - Only buy if `money > deed.price * 2.0` (high buffer)
  - If losing (net worth < average opponent net worth): slightly more aggressive (1.8x multiplier)
  - Avoid auctions (never bid above 70% of price)
  
- **calculateBidIncrease**:
  - Internal max: 70% of deed price
  - Conservative increments: $10 per round (minimal commitment)
  - Drop out early if bidding gets competitive
  - Will not bid if it reduces cash below reserve
  
- **valuateProperty**:
  - Mid-priced properties: base value × 1.3
  - Extreme properties (Brown, Dark Blue): base value × 0.8
  - Safety factor: -10% if risky position
  
- **getMinimumCashReserve**: 
  - Base: $500 (high buffer)
  - If winning (net worth > all opponents): increase to $600 (play it safe)
  - If losing badly (net worth < 50% of leader): reduce to $400 (take more risks)

- **selectPropertyToDevelop**:
  - Only develop if `money > reserve + buildingCost * 3`
  - Prefer 2-3 houses (moderate rent, lower risk)
  - Never rush to hotels
  
- **shouldUnmortgageProperty**: `money > unmortgageCost * 4.0` (very conservative)

- **prioritizeMortgages**: Farthest from monopoly, lowest value

- **prioritizeBuildingSales**: Sell early and often to maintain buffer

### 4.6 GamblerStrategy (Cookie Monster)

**Archetype**: Spend cash fast, grow assets

**Behavior**:
- **shouldBuyProperty**:
  - Buy everything affordable
  - Loves railroads (always buy)
  - Will liquidate to buy if close to monopoly
  
- **calculateBidIncrease**:
  - Internal max: 150-200% of deed price
  - Aggressive increments: $50-100 per round (go big or go home)
  - Will bid entire cash reserve
  - Extra aggressive on railroads and monopoly completion
  
- **valuateProperty**:
  - Railroads: base value × 2.0
  - Monopoly completion: base value × 2.5
  - Everything else: base value × 1.2
  
- **getMinimumCashReserve**: $0 (no reserve)

- **selectPropertyToDevelop**:
  - Build on everything possible
  - No cash reserve consideration
  - Rush to hotels
  
- **shouldUnmortgageProperty**: Always if `money >= unmortgageCost`

- **prioritizeMortgages**: Mortgage everything (only as last resort before selling buildings)

- **prioritizeBuildingSales**: Sell buildings only as absolute last resort

### 4.7 CalculatingStrategy (Bert)

**Archetype**: Mathematical optimization

**Behavior**:
- **shouldBuyProperty**:
  - Calculate expected ROI based on:
    - Landing probability (position on board)
    - Rent-to-price ratio
    - Monopoly completion probability
  - Buy if ROI > 15% and `money > deed.price * 1.5`
  
- **calculateBidIncrease**:
  - Internal max: calculated strategic value (strict)
  - Efficient increments: exactly $10 per round (no waste)
  - Never exceed 110% of base value unless monopoly
  - Factor in opportunity cost
  - Drop out immediately if currentBid exceeds calculated value
  
- **valuateProperty**:
  - Complex calculation:
    - Base rent × landing probability × game length estimate
    - Monopoly potential × completion probability
    - Building efficiency (rent per $100 building cost)
  - Orange/Red get +20% (optimal ROI from analysis)
  
- **getMinimumCashReserve**: 
  - Dynamic: highest rent on board × 2
  - Minimum $300
  
- **selectPropertyToDevelop**:
  - Build to 3 houses first (rent efficiency sweet spot)
  - Prioritize Orange/Red (best ROI)
  - Only develop if ROI > 20%
  
- **shouldUnmortgageProperty**: 
  - Calculate payback period
  - Unmortgage if payback < 10 turns
  
- **prioritizeMortgages**: Lowest ROI properties first

- **prioritizeBuildingSales**: Lowest ROI developments first

### 4.8 ChaoticStrategy (Ernie)

**Archetype**: Disruption and unpredictability

**Constructor**: `ChaoticStrategy(rng: Random = Random.Default)`

**Behavior**:
- **shouldBuyProperty**:
  - Examine `board.players` to check if property completes opponent monopoly
  - Buy to block if any opponent needs this property for monopoly
  - Random otherwise (60% buy rate using `rng`)
  - Prefer properties that break up opponent monopolies
  
- **calculateBidIncrease**:
  - Check `board.players` to identify if property blocks opponent monopoly
  - If blocking opponent: internal max 200% of price, aggressive increments ($30-80)
  - If opponent is cash-rich (check `opponent.money`): bid aggressively to drain cash
  - Otherwise: internal max random 30-130% of price (using `rng`)
  - Chaotic increments: sometimes $5, sometimes $100 (unpredictable)
  
- **valuateProperty**:
  - Check if property blocks opponent monopoly (examine `board.players` deeds)
  - Blocking value: base value × 3.0
  - Chaos value: random multiplier 0.5-2.0 (using `rng`)
  - Monopoly completion (for self): base value × 2.0
  
- **getMinimumCashReserve**: Random $0-$500 (using `rng`, changes each call)

- **selectPropertyToDevelop**:
  - Build unevenly when possible (within even-building rules)
  - Prioritize intimidation (visible hotels)
  - Random selection otherwise (using `rng`)
  
- **shouldUnmortgageProperty**: Random 40% chance if affordable (using `rng`)

- **prioritizeMortgages**: Random (using `rng.shuffle()`), with slight preference for opponent-blocking properties last

- **prioritizeBuildingSales**: Random (using `rng.shuffle()`), emotional (might keep favorite properties developed longer)

---

## 5. Implementation Phases

### Phase 1: Foundation (Strategy Interface & Default)

**Goal**: Establish architecture without changing behavior

**Tasks**:
1. Create `strategy` package and `PlayerStrategy` interface
2. Create `PropertyValuation` data class
3. Implement `DefaultStrategy` matching current behavior exactly
4. Add `strategy` parameter to `Player` constructor with `DefaultStrategy()` default
5. Update `Player` methods to delegate to strategy
6. Remove `open` modifiers from `isPayingGetOutOfJailEarlyFee` and `shouldUnmortgageProperty` (not used)

**Tests**:
- All existing tests must pass (regression)
- New tests: `DefaultStrategyTest` verifying each method matches old behavior
- Integration test: Game with `DefaultStrategy` produces identical results to current code

**Acceptance Criteria**:
- Zero behavior changes
- All 344+ existing tests pass
- New tests: ~15 for `DefaultStrategy`

### Phase 2: Property Valuation System ✅ **COMPLETE**

**Goal**: Build valuation logic used by all strategies

**Tasks**:
1. ~~Implement `PropertyValuation.calculateBaseValue()` using simple heuristics~~:
   - ~~Start with deed price as baseline~~
   - ~~Apply traffic multipliers by color group~~:
     - ~~1.2x for Brown/LightBlue (post-GO)~~
     - ~~1.15x for Pink/Orange (post-Jail)~~
     - ~~1.1x for Railroads (cards send players there)~~
   - ~~Add monopoly proximity bonus (game state dependent)~~
   - ~~Add development potential value (game state dependent)~~
2. ~~Add helper methods~~:
   - ~~`getTrafficMultiplier(deed)`: Returns color-group-based multiplier~~
   - ~~`calculateMonopolyBonus(deed, player)`: Value of completing monopoly (10-25%)~~
   - ~~`calculateDevelopmentValue(deed, player)`: Building cost efficiency~~
3. ~~Create `PropertyValuationTest` with comprehensive coverage~~

**Tests**: ✅ **19 tests, all passing**
- ~~Unit tests for traffic multipliers (post-GO, post-Jail, railroads)~~
- ~~Unit tests for monopoly bonus calculation (1 away, 2 away, no bonus cases)~~
- ~~Unit tests for development value (properties, railroads, utilities)~~
- ~~Edge cases: utilities, railroads, combined bonuses~~
- ~~All tests include expected value assertions with clear calculation comments~~

**Acceptance Criteria**: ✅ **All met**
- ~~`PropertyValuation` can assess any deed~~
- ~~Valuation is deterministic (same inputs = same output)~~
- ~~High-traffic properties have measurably higher values~~
- ~~19 tests with expected value assertions~~

**Implementation Notes**:
- Used color groups instead of board positions for traffic multipliers (simpler, equally effective)
- No caching needed - `when` statement on enum is fast enough
- Removed unused `board` parameter from `calculateBaseValue` (not needed for base calculation)
- All calculations return `Int` values in dollars with human-readable reasoning strings

### Phase 3: Conservative Persona (Big Bird)

**Goal**: First non-default persona to validate architecture

**Tasks**:
1. Implement `ConservativeStrategy`
2. Create `BigBird` test player using `ConservativeStrategy`
3. Write comprehensive unit tests for each strategy method
4. Write integration test: game with 4 Big Birds

**Tests**:
- Unit tests: Each `ConservativeStrategy` method
- Integration test: Verify conservative behavior (high cash reserves, slow development)
- Comparison test: Big Bird vs. Default strategy in same game scenario

**Acceptance Criteria**:
- `ConservativeStrategy` fully implemented
- Big Bird maintains higher cash reserves than default
- Big Bird develops more slowly than default
- New tests: ~25 (15 unit, 10 integration)

### Phase 4: Aggressive Personas (Oscar, Count, Cookie)

**Goal**: Implement personas with distinct property preferences

**Tasks**:
1. Implement `SlumlordStrategy` (Oscar)
2. Implement `HighRentStrategy` (Count)
3. Implement `GamblerStrategy` (Cookie)
4. Create test players for each
5. Write unit and integration tests

**Tests**:
- Unit tests for each strategy
- Integration tests: 
  - Oscar prefers cheap properties
  - Count prefers expensive properties
  - Cookie buys everything
- Comparison test: Oscar vs. Count vs. Cookie in same game

**Acceptance Criteria**:
- All three strategies fully implemented
- Observable differences in property acquisition patterns
- New tests: ~60 (20 per persona)

### Phase 5: Calculated & Chaotic Personas (Bert, Ernie)

**Goal**: Implement most complex strategies

**Tasks**:
1. Implement `CalculatingStrategy` (Bert)
   - ROI calculations
   - Dynamic cash reserve
   - Optimal development (3 houses)
2. Implement `ChaoticStrategy` (Ernie)
   - Constructor accepts `Random` parameter (default `Random.Default`)
   - Opponent blocking logic
   - Random elements use injected RNG
3. Create test players for each
4. Write unit and integration tests

**Tests**:
- Unit tests for each strategy
- `ChaoticStrategy` tests use seeded RNG (e.g., `Random(42)`) for deterministic behavior
- `ChaoticStrategy` tests verify opponent blocking:
  - Create scenario where opponent needs one property for monopoly
  - Verify Ernie buys/bids aggressively to block
- Integration tests:
  - Bert makes mathematically optimal decisions
  - Ernie blocks opponents and behaves unpredictably (but deterministically with seed)
- Comparison test: Bert vs. Ernie (order vs. chaos)

**Acceptance Criteria**:
- Both strategies fully implemented
- Bert's ROI calculations are accurate
- Ernie's randomness is deterministic with seeded RNG
- Tests verify same seed produces same decisions
- New tests: ~50 (25 per persona)

### Phase 6: Impulsive Persona & Refinement (Elmo)

**Goal**: Complete persona set and polish

**Tasks**:
1. Implement `ImpulsiveStrategy` (Elmo)
   - Constructor accepts `Random` parameter (default `Random.Default`)
   - All random decisions use injected RNG
2. Refactor common strategy logic into helper utilities
3. Add `toString()` to strategies for debugging
4. Update `TechnicalAnalysis.md` with strategy system documentation
5. Create `PlayerPersonasGuide.md` for users

**Tests**:
- Unit tests for `ImpulsiveStrategy` with seeded RNG
- Integration test: 7-player game with all personas (seeded for determinism)
- Performance test: Ensure strategy overhead is minimal
- Regression test: Verify all existing tests still pass
- Verify random strategies produce same results with same seed

**Acceptance Criteria**:
- All 7 personas implemented
- Random strategies (`ImpulsiveStrategy`, `ChaoticStrategy`) accept RNG parameter
- Tests use seeded RNG for deterministic behavior
- Documentation complete
- All tests pass (400+ total)
- New tests: ~30

---

## 6. Testing Strategy

### 6.1 Unit Tests

**Per Strategy Class** (~15-25 tests each):
- Each method tested in isolation
- Edge cases: no money, no properties, monopolies, etc.
- Boundary conditions: exact cash amounts, reserve thresholds
- Determinism: Same inputs always produce same outputs

**Valuation Tests** (~20 tests):
- Base value calculation accuracy
- Monopoly completion bonuses
- ROI calculations
- Landing probability estimates

### 6.2 Integration Tests

**Per Persona** (~10 tests each):
- Full game with 4 identical personas
- Verify characteristic behavior emerges:
  - Oscar ends with cheap properties
  - Count ends with expensive properties
  - Big Bird maintains high cash
  - Cookie develops aggressively
  - Bert makes optimal choices
  - Ernie blocks opponents
  - Elmo behaves randomly
  
**Cross-Persona Tests** (~15 tests):
- Mixed games: different personas compete
- Verify strategies interact correctly
- No crashes or illegal moves
- Deterministic with seeded RNG

### 6.3 Regression Tests

- All existing 344+ tests must pass
- No behavior changes for `DefaultStrategy`
- Performance: Strategy overhead < 5% of game execution time

### 6.4 Test Utilities

Create `StrategyTestUtils.kt`:
```kotlin
object StrategyTestUtils {
    fun createTestPlayer(
        name: String,
        strategy: PlayerStrategy,
        money: Int = 1500,
        deeds: Map<TitleDeed, Player.Development> = emptyMap()
    ): Player
    
    fun createTestBoard(
        players: List<Player>,
        seed: Long = 42
    ): Board
    
    fun assertStrategyBehavior(
        strategy: PlayerStrategy,
        scenario: GameScenario,
        expectedBehavior: BehaviorAssertion
    )
}
```

---

## 7. Migration Path

### 7.1 Backward Compatibility

- Existing `Player` constructor remains unchanged (defaults to `DefaultStrategy`)
- All existing tests pass without modification
- Remove unused `open` modifiers (no subclasses exist in codebase)

### 7.3 User Migration

**Old Code**:
```kotlin
val player = Player("Alice")
```

**New Code (Phase 1-6)**:
```kotlin
// Still works, uses DefaultStrategy
val player = Player("Alice")

// Or explicitly specify strategy
val player = Player("Alice", strategy = ConservativeStrategy())
```

**Future**:
```kotlin
// Strategy required
val player = Player("Alice", strategy = ConservativeStrategy())
```

---

## 8. Integration with Future Auctions

### 8.1 Auction Prerequisites (Provided by This Plan)

1. **Property Decline**: `shouldBuyProperty()` can return `false`
2. **Bid Calculation**: `calculateMaxBid()` provides max bid amount
3. **Valuation**: `valuateProperty()` informs bidding strategy

### 8.2 Auction Implementation (Future Work)

When auctions are implemented, they will:

1. Call `player.isBuying(deed, bank, board)` on landing
2. If `false`, trigger auction:
   ```kotlin
   fun conductAuction(deed: TitleDeed, players: List<Player>, bank: Bank, board: Board): Player? {
       var currentBid = 10 // Starting bid per official rules
       var currentWinner: Player? = null
       val activeBidders = players.toMutableList()
       
       while (activeBidders.size > 1) {
           val bidsThisRound = activeBidders.mapNotNull { player ->
               val nextBid = player.strategy.calculateBidIncrease(deed, currentBid, player, bank, board)
               if (nextBid != null && nextBid > currentBid) {
                   player to nextBid
               } else {
                   activeBidders.remove(player) // Player drops out
                   null
               }
           }
           
           if (bidsThisRound.isEmpty()) break
           
           // Highest bid wins this round
           val (winner, bid) = bidsThisRound.maxBy { it.second }
           currentBid = bid
           currentWinner = winner
       }
       
       // Winner pays final bid
       currentWinner?.let { winner ->
           bank.sellDeedToPlayer(deed::class, winner, board, price = currentBid)
       }
       
       return currentWinner
   }
   ```
3. Winner pays final bid amount via `Bank.sellDeedToPlayer()` (may need to add price override parameter)

### 8.3 Strategy Considerations for Auctions

Strategies must consider:
- **Liquidity**: Can I afford my next bid?
- **Opportunity Cost**: Is this better than waiting for other properties?
- **Opponent Blocking**: Should I bid high to deny opponents?
- **Bid Pacing**: Small increments to avoid overpaying vs. large jumps to intimidate
- **Drop-Out Timing**: When to stop bidding (internal max, cash constraints, better opportunities)
- **Opponent Behavior**: (Future) Track opponent bidding patterns to predict their limits

---

## 9. Performance Considerations

### 9.1 Strategy Overhead

- Strategies are stateless: no memory overhead per player
- Single strategy instance can be shared across multiple players
- Method calls are simple delegations: negligible CPU overhead
- Valuation calculations may be expensive but are deterministic

### 9.2 Optimization Opportunities

1. **Strategy Instance Reuse**: Share single strategy instance across multiple players
   ```kotlin
   val conservativeStrategy = ConservativeStrategy()
   val players = listOf(
       Player("Big Bird 1", strategy = conservativeStrategy),
       Player("Big Bird 2", strategy = conservativeStrategy),
       Player("Big Bird 3", strategy = conservativeStrategy)
   )
   ```
2. **Caching Expensive Calculations**: Cache computed values with proper invalidation
   - Property valuations: Cache per game state hash
   - Net worth rankings: Cache and invalidate on property/cash changes
   - Monopoly proximity: Cache per player's deed set
   - Example pattern:
     ```kotlin
     private var cachedValuations: Map<TitleDeed, PropertyValuation>? = null
     private var cacheGameStateHash: Int = 0
     
     fun valuateProperty(deed: TitleDeed, player: Player, board: Board): PropertyValuation {
         val currentHash = computeGameStateHash(player, board)
         if (cachedValuations == null || cacheGameStateHash != currentHash) {
             cachedValuations = computeAllValuations(player, board)
             cacheGameStateHash = currentHash
         }
         return cachedValuations!![deed]!!
     }
     ```
3. **Lazy Evaluation**: Only calculate values when needed (e.g., don't value all properties on every turn)
4. **Parallel Strategies**: (Future) Evaluate multiple strategies in parallel for Monte Carlo

### 9.3 Performance Tests

- Benchmark: 1000 games with DefaultStrategy vs. current code (target: within 10% slowdown)
- Benchmark: 1000 games with all personas (should complete in reasonable time)
- Memory: Verify strategy instances can be reused across games without state leakage
- Cache effectiveness: Verify caching reduces redundant calculations (measure cache hit rate)
- Cache correctness: Verify cached values are invalidated when game state changes

---

## 10. Documentation Deliverables

### 10.1 Code Documentation

- KDoc comments on all `PlayerStrategy` methods
- Examples in KDoc showing typical usage
- `@see` references to persona implementations

### 10.2 User Guides

**`PlayerPersonasGuide.md`**:
- Overview of each persona
- Character descriptions and motivations
- Strategy behavior summaries
- Usage examples
- Customization guide (creating new personas)

**`TechnicalAnalysis.md` Updates**:
- Section 2.2: Add strategy pattern to design patterns
- Section 3.3: Update `Player` documentation
- Section 7.2: Remove "Strategy Abstraction" from technical debt
- Section 8.1: Update "Strategy Abstraction" future direction (mark as complete)

### 10.3 Migration Guide

**`MIGRATION.md`** (new file):
- How to update existing code
- Strategy selection guide
- Custom strategy tutorial
- Troubleshooting common issues

---

## 11. Risks & Mitigations

### 11.1 Risk: Behavior Divergence

**Risk**: New strategies behave illegally or crash the game

**Mitigation**:
- Comprehensive unit tests for each strategy
- Integration tests catch illegal moves
- Validation layer in `Player` to catch strategy errors
- Fail-fast assertions in development mode

### 11.2 Risk: Performance Degradation

**Risk**: Strategy overhead slows down simulations

**Mitigation**:
- Performance benchmarks in CI
- Profiling during development
- Caching for expensive calculations
- Lazy evaluation where possible

### 11.3 Risk: Test Maintenance Burden

**Risk**: 100+ new tests are hard to maintain

**Mitigation**:
- Test utilities reduce duplication
- Clear test naming conventions
- Parameterized tests for similar scenarios
- Integration tests catch regressions

### 11.4 Risk: Incomplete Abstraction

**Risk**: Strategy interface doesn't cover all decision points

**Mitigation**:
- Start with known decision points (purchase, development, liquidation)
- Design for extensibility (easy to add methods)
- Review with auctions and trading in mind
- Iterative refinement based on implementation experience

---

## 12. Success Criteria

### 12.1 Functional Requirements

- ✅ Players can decline property purchases
- ✅ Players have distinct property preferences
- ✅ Players maintain different cash reserves
- ✅ Players develop properties differently
- ✅ Players liquidate assets differently
- ✅ All 7 personas implemented and tested
- ✅ Backward compatibility maintained

### 12.2 Quality Requirements

- ✅ All existing tests pass (344+)
- ✅ New tests: 200+ covering all personas and integration
- ✅ Code coverage: >90% for strategy package
- ✅ Performance: <5% overhead vs. current implementation
- ✅ Documentation: Complete user and technical docs

### 12.3 Readiness for Auctions

- ✅ `shouldBuyProperty()` can return false (triggers auction)
- ✅ `calculateBidIncrease()` provides incremental bidding with dynamic back-and-forth
- ✅ `valuateProperty()` supports bidding decisions (internal max calculation)
- ✅ Strategies are deterministic (testable auctions)
- ✅ Incremental bidding supports future visualizations and playable interfaces

---

## 13. Open Questions

1. ~~**Valuation Complexity**: How sophisticated should `calculateBaseValue()` be? Should it use actual landing probability data from statistics, or simplified heuristics?~~
   - **RESOLVED**: Start with simple heuristics using price/rent ratios. Apply traffic multipliers to high-traffic positions (post-GO, post-Jail, near Railroads) based on card/rule effects.

2. ~~**Random Strategies**: How do we test `ImpulsiveStrategy` and `ChaoticStrategy` randomness? Should they accept a seeded RNG?~~
   - **RESOLVED**: Yes, accept seeded RNG following existing pattern used by `Dice` and `Deck`. Strategies with randomness will accept `Random` parameter in constructor for deterministic testing.

3. ~~**Strategy State**: Should strategies be completely stateless, or can they maintain lightweight state (e.g., "I've been burned by Boardwalk before")?~~
   - **RESOLVED**: Keep strategies stateless for now. All decisions based purely on current game state passed to methods. Future work could add property preferences/aversions if needed.

4. ~~**Opponent Awareness**: Should strategies have access to opponent states (cash, properties) for blocking decisions? This is public information in Monopoly.~~
   - **RESOLVED**: Yes. Strategies receive `Board` parameter which contains all players. Strategies can examine opponent cash, properties, and developments for blocking/competitive decisions.

5. ~~**Dynamic Strategies**: Should strategies be able to change behavior mid-game (e.g., become more conservative when losing)?~~
   - **RESOLVED**: Yes, but only through stateless evaluation of current game state. Strategies can compare player's net worth to opponents' to determine relative position and adjust decisions accordingly. No mutable state needed - "losing" is determined by comparing `player.netWorth()` to `board.players.map { it.netWorth() }` on each decision.

6. ~~**Performance Targets**: What's an acceptable slowdown for strategy overhead? 5%? 10%?~~
   - **RESOLVED**: Either 5% or 10% is acceptable. Use caching for expensive computed values (e.g., property valuations, net worth calculations) with proper cache invalidation when game state changes.

7. ~~**Auction Integration**: Should `calculateMaxBid()` return a single max bid, or a bidding strategy (e.g., "increment by $10 up to max")?~~
   - **RESOLVED**: Use incremental bidding with `calculateBidIncrease(deed, currentBid, player, bank, board)` that returns next bid amount (or null to drop out). This allows dynamic back-and-forth bidding without revealing max bid upfront. Useful for simulations and future visualizations/playable interfaces.

---

## 14. Next Steps

1. ~~**Review this plan** with Jonathan~~ ✅ **COMPLETE**
2. ~~**Answer open questions** and refine specifications~~ ✅ **COMPLETE** - All 7 questions resolved
3. ~~**Phase 1: Strategy interface and DefaultStrategy**~~ ✅ **COMPLETE**
   - ~~Create `strategy` package structure~~
   - ~~Implement `PlayerStrategy` interface~~
   - ~~Implement `PropertyValuation` data class~~
   - ~~Implement `DefaultStrategy` matching current behavior~~
   - ~~Update `Player` class to accept strategy parameter~~
   - ~~Remove `open` modifiers from unused methods~~
   - ~~Write comprehensive tests (19 tests for PropertyValuation)~~
4. ~~**Phase 2: Property Valuation System**~~ ✅ **COMPLETE**
   - ~~Implement `PropertyValuation.calculateBaseValue()` with simple heuristics~~
   - ~~Traffic multipliers by color group (Brown/LightBlue 1.2x, Pink/Orange 1.15x, Railroads 1.1x)~~
   - ~~Monopoly proximity bonus (10-25% based on ownership)~~
   - ~~Development potential value (building cost efficiency)~~
   - ~~Helper methods: `getTrafficMultiplier`, `calculateMonopolyBonus`, `calculateDevelopmentValue`~~
   - ~~Comprehensive test suite (19 tests with expected value assertions)~~
5. **Iterate** through Phases 3-6 with testing and review at each step

---

## Appendix A: Strategy Method Call Sites

| Method | Called From | Frequency | Notes |
|--------|-------------|-----------|-------|
| `shouldBuyProperty` | `Tile.Buyable.onLanding` | Per unowned property landing | Triggers auction if false |
| `calculateMaxBid` | (Future) Auction logic | Per auction | Not yet implemented |
| `valuateProperty` | Various strategy methods | As needed | Helper for other decisions |
| `getMinimumCashReserve` | `selectPropertyToDevelop`, `calculateMaxBid` | Per decision | Informs spending limits |
| `shouldPayJailFee` | `Board.attemptToGetOutOfJail` | Per jail turn | Before rolling for doubles |
| `selectPropertyToDevelop` | `Player.developProperties` | After each dice roll | Can return null |
| `shouldUnmortgageProperty` | `Player.unmortgageProperties`, bankruptcy | After development, on transfer | Per mortgaged property |
| `prioritizeMortgages` | `Player.liquidateAssets` | When liquidating | Phase 1 & 3 |
| `prioritizeBuildingSales` | `Player.liquidateAssets` | When liquidating | Phase 2 |

---

## Appendix B: Persona Comparison Matrix

| Persona | Buy Threshold | Max Bid | Cash Reserve | Development Style | Liquidation Priority |
|---------|---------------|---------|--------------|-------------------|---------------------|
| Default | `money > price` | N/A | $0 | Greedy (highest rent) | Non-monopoly first |
| Oscar (Slumlord) | `money > price * 1.5` | 80% (cheap) | $200 | 4 houses, no hotels | Expensive first |
| Count (High Rent) | `money > price * 1.2` | 120-150% | $300 | Rush to hotels | Cheap first |
| Elmo (Impulsive) | 90% chance | Random 50-150% | $50 | Random | Random |
| Big Bird (Conservative) | `money > price * 2.0` | 70% | $500 | Slow, 2-3 houses | Sell early |
| Cookie (Gambler) | Always | 150-200% | $0 | Max builds | Buildings last |
| Bert (Calculating) | ROI > 15% | Strategic value | Dynamic | 3 houses (optimal) | Lowest ROI first |
| Ernie (Chaotic) | 60% or blocking | 30-200% | Random | Uneven, intimidation | Random |

---

## Appendix C: Property Valuation Factors

### Base Value Components

1. **Rent Potential**
   - Current rent (if monopoly exists)
   - Potential rent (if monopoly could be completed)
   - Hotel rent (maximum development)

2. **Monopoly Proximity**
   - Number of properties needed to complete
   - Probability of acquiring missing properties
   - Opponent ownership of blocking properties

3. **Development Efficiency**
   - Building cost per rent dollar
   - Houses to hotel ratio
   - Even-building constraints

4. **Position Value**
   - Landing probability (based on board position)
   - Traffic patterns (post-jail, post-GO)
   - Chance/Community Chest card effects

5. **Liquidity Value**
   - Mortgage value (safety net)
   - Resale potential (to other players in future trading)

6. **Strategic Value**
   - Blocking opponent monopolies
   - Completing own monopolies
   - Railroad/utility set completion

### Valuation Formula (Simplified Heuristic)

```
strategicValue = deedPrice × trafficMultiplier
                 + monopolyBonus
                 + developmentValue
```

Where:
- `deedPrice`: Base property price
- `trafficMultiplier`: Position-based multiplier (1.0 - 1.3x)
  - 1.3x: Post-Jail (St. Charles, States, Virginia)
  - 1.2x: Post-GO (Mediterranean through Connecticut)
  - 1.1x: Near railroads
  - 1.0x: All others
- `monopolyBonus`: Extra value if completes monopoly
  - Based on number of properties needed to complete
  - Considers opponent ownership of blocking properties
- `developmentValue`: Potential rent with buildings
  - For properties: hotel rent - current rent
  - For railroads/utilities: set completion value
  
**Traffic Multiplier Rationale**:
- Post-GO: Players always pass GO, plus "Advance to GO" card
- Post-Jail: Players leave jail frequently (3 turns max, doubles, fee, card)
- Near Railroads: Multiple Chance cards send players to nearest railroad
- These effects are well-known in Monopoly strategy and don't require statistical analysis

---

**End of Plan**
