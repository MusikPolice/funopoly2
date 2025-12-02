# Statistics Collection Feature - Implementation Plan

**Date:** December 2, 2025  
**Purpose:** Enable quantitative analysis of rule changes, config modifications, and feature additions through comprehensive game statistics collection.

---

## 1. Executive Summary

This feature will implement a statistics collection system that captures key game events and metrics during Monopoly gameplay. The initial implementation will support single-game statistics with a design that naturally extends to Monte Carlo simulations.

**Core Principle:** Minimal, focused implementation that captures essential metrics without over-engineering. Event-based architecture for clean separation of concerns.

---

## 2. Metrics to Collect

### 2.1 Core Game Metrics (from TechnicalAnalysis.md)

**Game Duration & Flow:**
- Total rounds played
- Winner identity (or no winner if max rounds reached)
- Reason for game end (bankruptcy vs. round limit)

**Landing Frequency:**
- Count of landings per tile (all 40 tiles)
- Breakdown by tile type (property, railroad, utility, tax, jail, etc.)
- Most/least landed tiles

**Net Worth Tracking:**
- Per-player net worth at end of each round
- Starting vs. ending net worth
- Maximum net worth achieved
- Net worth at bankruptcy (if applicable)

**Property Ownership:**
- Time-to-first-monopoly (rounds)
- Which color groups form monopolies and when
- Property ownership duration (rounds owned)
- Most profitable properties (rent collected)

**Development Patterns:**
- Houses/hotels built per color group
- Round when first house/hotel was built
- Total development spending per player
- Development distribution across color groups

### 2.2 Additional Metrics (from MonopolyRules2001.md analysis)

**Financial Transactions:**
- Total rent paid (per player, per property)
- Total rent collected (per player, per property)
- Bank payments received (GO, cards, tax refunds)
- Bank charges paid (taxes, fees, purchases)
- Largest single rent payment

**Property Transactions:**
- Properties purchased and when
- Properties mortgaged/unmortgaged and when
- Property sale prices (to bank or players)

**Bankruptcy Events:**
- Player bankruptcy count
- Bankruptcy creditor (bank vs. player)
- Round of bankruptcy
- Net worth at bankruptcy
- Assets transferred in bankruptcy

**Jail Statistics:**
- Times sent to jail (per player)
- Jail escape method (doubles, payment, card)
- Turns spent in jail
- Total jail fees paid

**Dice & Movement:**
- Total doubles rolled (per player)
- Consecutive doubles leading to jail
- Average dice roll value
- Total spaces moved

**Cards:**
- Card draw count by deck (Chance, Community Chest)
- Cards drawn by type
- Get Out of Jail Free card usage

**Building Activity:**
- Houses sold back to bank (liquidation pressure indicator)
- Hotels sold back to bank
- Mortgages forced by debt vs. strategic mortgages

### 2.3 Derived Metrics (Calculated Post-Game)

These are computed from collected data:
- Average rent per landing
- ROI per property (rent collected / purchase price)
- Liquidity pressure events (forced liquidations)
- Monopoly formation rate
- Development aggressiveness (% of income spent on buildings)
- Survival time (rounds until bankruptcy)

---

## 3. Architecture Options

### Option A: Event-Based System with Observer Pattern

**Description:**  
Create a `GameEvent` sealed class hierarchy and `GameStatistics` listener that observes and records events.

**Pseudocode:**
```
sealed class GameEvent {
    data class PlayerMoved(player, from, to, passedGo)
    data class RentPaid(payer, recipient, amount, property)
    data class PropertyPurchased(player, property, price)
    data class PlayerBankrupted(player, creditor, round)
    data class HouseBuilt(player, property, houseCount)
    // ... etc
}

interface GameEventListener {
    fun onEvent(event: GameEvent)
}

class GameStatistics : GameEventListener {
    // mutable collections to accumulate data
    private val landingCounts = mutableMapOf<Tile, Int>()
    private val rentPayments = mutableListOf<RentPayment>()
    // ... etc
    
    override fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.PlayerMoved -> landingCounts.merge(event.to, 1, Int::plus)
            is GameEvent.RentPaid -> rentPayments.add(...)
            // ... etc
        }
    }
    
    fun generateReport(): StatisticsReport { ... }
}

class EventBus {
    private val listeners = mutableListOf<GameEventListener>()
    
    fun register(listener: GameEventListener)
    fun emit(event: GameEvent) {
        listeners.forEach { it.onEvent(event) }
    }
}
```

**Integration:**
- Add `EventBus` as optional parameter to `Monopoly`, `Board`, `Bank`, `Player`
- Emit events at key points (movement, transactions, developments)
- `GameStatistics` registered as listener at game start
- Generate report at game end

**Pros:**
- Clean separation of concerns (statistics don't pollute game logic)
- Extensible: easy to add new event types or listeners
- Multiple listeners possible (statistics, logging, replay recording)
- Game code remains testable without stats dependency
- Follows Observer pattern idiomatically

**Cons:**
- Requires threading event bus through multiple classes
- More classes/files to create and maintain
- Slight performance overhead (event object allocation)
- Need to ensure all relevant code emits events (easy to miss spots)

**Cost Estimate:**
- ~15-20 new classes (event types + listeners)
- ~50-100 emit() calls scattered through existing code
- Moderate complexity: well-understood pattern
- High testability: can test event emission independently

---

### Option B: Direct Statistics Integration

**Description:**  
Pass `GameStatistics` object directly to game components, call recording methods inline.

**Pseudocode:**
```
class GameStatistics {
    private val landingCounts = mutableMapOf<Tile, Int>()
    private val rentPayments = mutableListOf<RentPayment>()
    // ... etc
    
    fun recordLanding(player, tile) { ... }
    fun recordRentPayment(payer, recipient, amount, property) { ... }
    fun recordPropertyPurchase(player, property, price) { ... }
    // ... etc
    
    fun generateReport(): StatisticsReport { ... }
}
```

**Integration:**
- Add `stats: GameStatistics?` parameter to `Monopoly`, `Board`, `Bank`, `Player`
- Call `stats?.recordXXX()` at key points
- Nullable to support backward compatibility

**Pros:**
- Simpler: fewer abstractions and classes
- Direct: no event object allocation
- Easy to find: recording calls are inline with actions
- Less code to write initially

**Cons:**
- Couples game logic to statistics (violates Single Responsibility)
- Makes game classes harder to test (need to mock/ignore stats)
- Every method needs stats parameter threaded through
- Harder to extend (new stat requires finding all call sites)
- Violates Open/Closed principle (can't add new listeners without changing code)

**Cost Estimate:**
- ~1 main class (GameStatistics) + data classes
- ~50-100 recording calls scattered through existing code
- Lower initial complexity
- Lower testability: game logic coupled to stats

---

### Option C: Wrapper/Proxy Pattern

**Description:**  
Create wrapper classes that intercept method calls and record statistics transparently.

**Pseudocode:**
```
class StatisticsBank(private val bank: Bank, private val stats: GameStatistics) : Bank by bank {
    override fun charge(amount, player, board, reason) {
        stats.recordBankCharge(player, amount, reason)
        bank.charge(amount, player, board, reason)
    }
    // ... wrap other methods
}
```

**Pros:**
- Minimal changes to existing code
- Clean separation via decoration
- Can be toggled on/off easily

**Cons:**
- Requires all components to be interface-based (significant refactor)
- Only captures public method calls (misses internal state changes)
- Complex delegation chains
- Kotlin's `by` delegation has limitations

**Cost Estimate:**
- High: requires extracting interfaces from all game classes
- Significant refactoring of existing code
- Over-engineered for current needs

**Verdict:** Rejected - violates KISS and YAGNI principles

---

## 4. Recommended Architecture: Option A (Event-Based)

**Rationale:**
1. **Extensibility:** Natural fit for future Monte Carlo aggregation
2. **Testability:** Can verify events without running full games
3. **Separation of Concerns:** Keeps game logic clean
4. **Industry Standard:** Observer pattern is well-understood
5. **Future-Proof:** Easy to add replay recording, debugging tools, visualization

**Trade-off Accepted:** Slightly higher initial complexity for long-term maintainability and extensibility.

---

## 5. Detailed Design

### 5.1 Core Components

**GameEvent.kt** - Sealed class hierarchy
```
sealed class GameEvent {
    // Movement
    data class RoundStarted(round: Int)
    data class TurnStarted(player: Player, round: Int)
    data class DiceRolled(player: Player, die1: Int, die2: Int, isDoubles: Boolean)
    data class PlayerMoved(player: Player, from: Int, to: Int, passedGo: Boolean)
    data class TileLanded(player: Player, tile: Tile)
    data class TurnEnded(player: Player, round: Int)
    data class RoundEnded(round: Int)
    
    // Financial
    data class BankPaidPlayer(player: Player, amount: Int, reason: String)
    data class PlayerChargedByBank(player: Player, amount: Int, reason: String)
    data class RentPaid(payer: Player, recipient: Player, amount: Int, property: TitleDeed)
    
    // Property
    data class PropertyPurchased(player: Player, deed: TitleDeed, price: Int)
    data class PropertyMortgaged(player: Player, deed: TitleDeed, mortgageValue: Int)
    data class PropertyUnmortgaged(player: Player, deed: TitleDeed, cost: Int)
    
    // Development
    data class HousePurchased(player: Player, property: Property, houseCount: Int, cost: Int)
    data class HotelPurchased(player: Player, property: Property, cost: Int)
    data class HouseSold(player: Player, property: Property, houseCount: Int, proceeds: Int)
    data class HotelSold(player: Player, property: Property, proceeds: Int)
    
    // Jail
    data class PlayerSentToJail(player: Player, reason: String)
    data class PlayerLeftJail(player: Player, method: String) // "rolled doubles", "paid fee", "used card"
    
    // Cards
    data class CardDrawn(player: Player, deck: String, card: Card) // deck: "Chance" or "CommunityChest"
    
    // Bankruptcy
    data class PlayerBankrupted(player: Player, creditor: Any, round: Int, netWorth: Int) // creditor: Player or Bank
    data class AssetTransferred(from: Player, to: Any, asset: String, value: Int) // to: Player or Bank
    
    // Game End
    data class GameEnded(winner: Player?, rounds: Int, reason: String)
}
```

**EventBus.kt** - Event distribution
```
class EventBus {
    private val listeners: MutableList<GameEventListener> = mutableListOf()
    
    fun register(listener: GameEventListener): Unit
    fun unregister(listener: GameEventListener): Unit
    fun emit(event: GameEvent): Unit // calls onEvent() on all listeners
}
```

**GameEventListener.kt** - Listener interface
```
interface GameEventListener {
    fun onEvent(event: GameEvent)
}
```

**GameStatistics.kt** - Primary statistics collector
```
class GameStatistics : GameEventListener {
    // Internal data structures
    private val landingCounts: MutableMap<String, Int> // tile name -> count
    private val rentPayments: MutableList<RentPayment>
    private val netWorthByRound: MutableMap<Player, MutableList<Int>>
    private val propertyTransactions: MutableList<PropertyTransaction>
    // ... etc
    
    override fun onEvent(event: GameEvent): Unit {
        // Update internal state based on event type
    }
    
    fun snapshot(): StatisticsSnapshot {
        // Return immutable snapshot of current statistics
    }
    
    fun generateReport(): StatisticsReport {
        // Calculate derived metrics and format for output
    }
}
```

**StatisticsReport.kt** - Report data class
```
data class StatisticsReport(
    val gameSummary: GameSummary,
    val tileLandingFrequency: Map<String, Int>,
    val playerStatistics: List<PlayerStatistics>,
    val propertyStatistics: List<PropertyStatistics>,
    val financialSummary: FinancialSummary,
    // ... etc
)

data class GameSummary(
    val rounds: Int,
    val winner: String?,
    val endReason: String
)

data class PlayerStatistics(
    val playerName: String,
    val finalNetWorth: Int,
    val maxNetWorth: Int,
    val totalRentPaid: Int,
    val totalRentCollected: Int,
    val propertiesOwned: Int,
    val monopoliesFormed: Int,
    val housesBuilt: Int,
    val hotelsBuilt: Int,
    val timesBankrupted: Int,
    val roundOfBankruptcy: Int?,
    val jailVisits: Int,
    val doublesRolled: Int,
    // ... etc
)

data class PropertyStatistics(
    val propertyName: String,
    val landingCount: Int,
    val totalRentCollected: Int,
    val ownedByPlayer: String?,
    val roundsPurchased: Int?,
    val developmentLevel: Int, // 0-5 (0=none, 1-4=houses, 5=hotel)
    // ... etc
)

// ... etc
```

### 5.2 Integration Points

**Monopoly.kt:**
- Check `config.collectStatistics` flag
- If enabled: create `EventBus` and `GameStatistics`, register listener
- Pass `EventBus` (or null) to `Board`, `Bank`
- Emit `GameEvent.GameEnded` at game completion
- If enabled: generate and output `StatisticsReport` based on `config.statisticsOutputFormat`

**Board.kt:**
- Accept `eventBus: EventBus?` parameter
- Emit `RoundStarted`, `RoundEnded`
- Emit `TurnStarted`, `TurnEnded`
- Emit `DiceRolled`, `PlayerMoved`, `TileLanded`
- Emit `PlayerSentToJail`, `PlayerLeftJail`

**Bank.kt:**
- Accept `eventBus: EventBus?` parameter
- Emit `BankPaidPlayer`, `PlayerChargedByBank`
- Emit `PropertyPurchased`, `PropertyMortgaged`, `PropertyUnmortgaged`
- Emit `HousePurchased`, `HotelPurchased`, `HouseSold`, `HotelSold`

**Player.kt:**
- Accept `eventBus: EventBus?` parameter
- Emit `PlayerBankrupted`, `AssetTransferred`

**Tile.kt:**
- Access `eventBus` via `Board` reference
- Emit `RentPaid` in `Buyable.onLanding()`
- Emit `CardDrawn` in `Chance.onLanding()` and `CommunityChest.onLanding()`

### 5.3 Backward Compatibility

All `eventBus` parameters will be nullable and default to `null`. This ensures:
- Existing tests continue to work without modification
- Statistics collection is opt-in
- Zero impact on performance when disabled

---

## 6. Implementation Phases

### Phase 1: Foundation (Event Infrastructure)
**Goal:** Create event system without disrupting existing functionality

**Tasks:**
1. Create `event/` package
2. Implement `GameEvent` sealed class with initial event types
3. Implement `EventBus` class
4. Implement `GameEventListener` interface
5. Write unit tests for EventBus (registration, emission, delivery)

**Test Strategy:**
- Test event bus can register/unregister listeners
- Test events are delivered to all listeners
- Test order of delivery is consistent
- Test null safety and error handling

**Deliverables:**
- `ca.jonathanfritz.monopoly.event` package
- Fully tested event infrastructure
- Zero changes to existing game code

---

### Phase 2: Event Emission (Game Integration)
**Goal:** Thread EventBus through game components and emit events

**Tasks:**
1. Add `eventBus: EventBus? = null` parameter to `Monopoly`, `Board`, `Bank`, `Player`
2. Emit movement events in `Board.executeRound()` and `Board.advancePlayerBy()`
3. Emit financial events in `Bank.pay()`, `Bank.charge()`
4. Emit property events in `Bank.sellDeedToPlayer()`, `Bank.mortgageDeed()`, etc.
5. Emit development events in `Bank.sellHouseToPlayer()`, etc.
6. Emit bankruptcy events in `Player.declareBankruptcy()`
7. Emit jail events in `Board.goToJail()`, `Board.attemptToGetOutOfJail()`
8. Emit card events in `Card.onDraw()` implementations
9. Emit game lifecycle events in `Monopoly.executeGame()` and `Board.executeRound()`

**Test Strategy:**
- For each emitting method, write tests that verify events are emitted
- Use a test listener that captures events for verification
- Ensure events contain correct data
- Verify events are emitted in correct order
- Verify nullability: game works when eventBus is null

**Deliverables:**
- Events emitted from all key game actions
- Tests verifying event emission
- No breaking changes to existing tests

---

### Phase 3: Statistics Collection
**Goal:** Implement GameStatistics listener and data accumulation

**Tasks:**
1. Create `statistics/` package
2. Implement `GameStatistics` class with event handlers
3. Implement data storage structures (maps, lists for accumulation)
4. Handle all event types defined in Phase 1
5. Implement `snapshot()` for mid-game inspection
6. Write comprehensive unit tests

**Test Strategy:**
- Test each event type updates appropriate statistics
- Test derived calculations (averages, sums, etc.)
- Test edge cases (empty games, single player, etc.)
- Test thread safety if needed (future: parallel Monte Carlo)
- Integration test: run a game with statistics enabled, verify counts

**Deliverables:**
- `GameStatistics` class that correctly tracks all metrics
- Comprehensive unit and integration tests
- No changes to game logic

---

### Phase 4: Reporting & Output
**Goal:** Format statistics into human-readable reports

**Tasks:**
1. Design `StatisticsReport` data classes
2. Implement `GameStatistics.generateReport()`
3. Calculate derived metrics (ROI, averages, etc.)
4. Implement `StatisticsFormatter` with console text output
5. Implement JSON output format
6. Add `collectStatistics` and `statisticsOutputFormat` to `Monopoly.Config`
7. Update `Monopoly.executeGame()` to conditionally generate and output report based on config

**Test Strategy:**
- Test report generation from known statistics
- Test report formatting is readable
- Test JSON serialization if implemented
- Verify accuracy of derived metrics
- Integration test: full game → statistics → report

**Deliverables:**
- `StatisticsReport` with comprehensive game summary
- Pretty-printed console output
- Optional JSON output

---

### Phase 5: Documentation & Polish
**Goal:** Document usage and finalize feature

**Tasks:**
1. Update README.md with statistics collection usage
2. Add example code showing how to enable statistics
3. Document event types in TechnicalAnalysis.md
4. Document statistics in TechnicalAnalysis.md
5. Add integration test demonstrating end-to-end usage
6. Update GenAIGuide.md if new patterns introduced

**Deliverables:**
- Complete documentation
- Example usage code
- Updated technical documentation

---

## 7. Future Enhancements (Out of Scope for Phase 1)

### Monte Carlo Simulation Support

Once single-game statistics are complete, extend to batch simulation:

**MonteCarloSimulator.kt:**
```
class MonteCarloSimulator(val config: SimulationConfig) {
    fun runSimulation(iterations: Int): AggregatedStatistics {
        val results = (1..iterations).map { i ->
            val game = Monopoly(...)
            val stats = GameStatistics()
            game.eventBus.register(stats)
            game.executeGame()
            stats.generateReport()
        }
        return AggregatedStatistics(results)
    }
}

data class AggregatedStatistics(
    val individualReports: List<StatisticsReport>,
    val meanRounds: Double,
    val medianRounds: Double,
    val propertyLandingDistribution: Map<String, LandingStats>,
    val winRateByPlayer: Map<String, Double>,
    // statistical distributions, confidence intervals, etc.
)
```

**Benefits:**
- Compare rule variants statistically
- Identify most/least balanced properties
- Quantify impact of strategy changes
- Generate probability distributions

**Cost:**
- Moderate complexity: aggregation logic
- Performance considerations: parallel execution, memory usage
- Statistical analysis: mean, median, std dev, confidence intervals

---

### Replay Recording

Extend event system to record full game state for playback:

**ReplayRecorder.kt:**
```
class ReplayRecorder : GameEventListener {
    private val events = mutableListOf<TimestampedEvent>()
    
    override fun onEvent(event: GameEvent) {
        events.add(TimestampedEvent(System.currentTimeMillis(), event))
    }
    
    fun saveToFile(path: String) { ... }
}

class ReplayPlayer(val events: List<TimestampedEvent>) {
    fun playback() { ... }
}
```

**Benefits:**
- Debug specific games
- Share interesting games
- Visualize game progression
- Training data for ML

---

### Visualization & Dashboards

Generate charts and graphs from statistics:

**StatisticsVisualizer.kt:**
```
class StatisticsVisualizer(val report: StatisticsReport) {
    fun generateLandingFrequencyChart(): Chart
    fun generateNetWorthOverTimeChart(): Chart
    fun generatePropertyROIChart(): Chart
    // ... etc
}
```

**Technologies:**
- ASCII charts for console (simple)
- HTML + Chart.js for browser (moderate)
- Integration with data science tools (Jupyter, R)

---

## 8. Risks & Mitigation

### Risk 1: Performance Overhead
**Concern:** Event emission and collection may slow down simulations  
**Likelihood:** Low  
**Impact:** Medium (slower Monte Carlo simulations)  
**Mitigation:**
- Event objects are lightweight data classes
- Event emission is simple method call (no complex logic)
- Make EventBus optional (nullable) for performance-critical runs
- Profile before optimizing

### Risk 2: Missing Events
**Concern:** Forgetting to emit events at key points  
**Likelihood:** Medium  
**Impact:** Medium (incomplete statistics)  
**Mitigation:**
- Comprehensive integration tests verify all expected events
- Code review checklist for event emission
- Start with most important events, expand iteratively

### Risk 3: Complexity Creep
**Concern:** Event system grows too complex  
**Likelihood:** Medium  
**Impact:** High (maintenance burden)  
**Mitigation:**
- Stick to YAGNI: only implement events we need now
- Keep event types simple (data classes, no logic)
- Regular refactoring to remove unused events
- Document event types and their purpose

### Risk 4: Test Maintenance
**Concern:** Event-based tests are fragile  
**Likelihood:** Low  
**Impact:** Medium (test maintenance overhead)  
**Mitigation:**
- Test outcomes, not event ordering (where possible)
- Use flexible event matching (verify presence, not exact order)
- Group related events for testing (e.g., "payment events" not specific order)

### Risk 5: Breaking Changes
**Concern:** Adding EventBus breaks existing code  
**Likelihood:** Low  
**Impact:** High (regression)  
**Mitigation:**
- Make EventBus nullable with default null
- Run full test suite after each phase
- No changes to public method signatures (add optional parameters only)

---

## 9. Success Criteria

### Phase 1-2 Success:
- [ ] All existing tests pass without modification
- [ ] Event bus can register listeners and deliver events
- [ ] Events are emitted from all key game actions
- [ ] Test coverage for event emission is >80%

### Phase 3-4 Success:
- [ ] GameStatistics correctly tracks all defined metrics
- [ ] Integration test runs a full game and verifies statistics
- [ ] Report output is human-readable and accurate
- [ ] Statistics can answer questions like:
  - What percentage of landings are on Boardwalk?
  - What is the average rent paid per round?
  - Which color group generates the most rent?
  - How long does it take to form the first monopoly?

### Overall Success:
- [ ] Can run game with statistics enabled via single parameter
- [ ] Statistics report provides actionable insights
- [ ] Zero performance impact when statistics disabled
- [ ] Code follows existing style and patterns
- [ ] Comprehensive documentation and examples

---

## 10. Design Decisions (Finalized)

**Decision 1: Statistics Collection Enablement**
- **Approach:** Opt-in via config flag
- **Rationale:** Core engine should remain flexible for future uses (playable game, alternative simulations). Statistics collection is a simulation concern, not a core engine requirement.
- **Implementation:** Add `collectStatistics: Boolean = false` to `Monopoly.Config`

**Decision 2: Output Format**
- **Approach:** Support both console text and JSON, controlled by config
- **Priority:** Console text first (human-readable for development)
- **Secondary:** JSON output (programmatic analysis for Monte Carlo)
- **Future:** CSV export for Excel analysis if needed
- **Implementation:** Add `statisticsOutputFormat: StatisticsOutputFormat = CONSOLE` to config
  - `enum class StatisticsOutputFormat { CONSOLE, JSON, BOTH }`

**Decision 3: Execution Time Tracking**
- **Approach:** Exclude from initial implementation
- **Rationale:** Execution time is an operational metric separate from game mechanics. Focus statistics on game state and player actions.
- **Future:** Can add as separate performance profiling feature if needed

**Decision 4: Event Type Coverage**
- **Approach:** Implement all ~30 event types upfront (comprehensive)
- **Rationale:** Goal is exploratory analysis to identify patterns that define "fun". Comprehensive data collection enables discovering unexpected correlations. YAGNI doesn't apply when we're explicitly trying to learn what we need.
- **Implementation:** All events listed in Section 5.1 will be implemented in Phase 2

**Decision 5: Parallel Execution**
- **Approach:** Sequential execution for initial implementation
- **Rationale:** 
  - Games execute in seconds, mostly console I/O bound
  - Removing console output (statistics-only mode) will be even faster
  - Parallel execution is whole-codebase refactor (thread safety, resource contention)
  - Premature optimization; profile first, parallelize only if necessary
- **Future:** Dedicated refactor for parallelization if/when statistical sampling requires it

---

## 11. Estimated Effort

**Phase 1 (Foundation):** 2-3 hours
- Event infrastructure is straightforward
- Well-understood patterns

**Phase 2 (Integration):** 4-6 hours
- Threading EventBus through ~10 classes
- Adding ~50-100 emit() calls
- Updating tests to verify emission

**Phase 3 (Statistics):** 3-4 hours
- Implementing data collection logic
- Handling ~20-30 event types
- Unit tests for each handler

**Phase 4 (Reporting):** 2-3 hours
- Report data structures
- Formatting logic
- Output generation

**Phase 5 (Documentation):** 1-2 hours
- README updates
- Technical documentation
- Example code

**Total Estimated Time:** 12-18 hours of focused work

---

## 12. Conclusion

This plan proposes an event-based architecture for statistics collection that balances simplicity with extensibility. The phased approach allows us to:

1. Build incrementally with tests at each step
2. Validate the design before full implementation
3. Maintain backward compatibility
4. Prepare for future Monte Carlo simulations

The event system follows established design patterns, keeps game logic clean, and provides a solid foundation for future enhancements like replay recording and visualization.

**Key Decisions:**
- Opt-in via config flag (flexible core engine)
- Comprehensive event coverage (enable exploratory analysis)
- Console + JSON output (development + automation)
- Sequential execution (simplicity over premature optimization)

**Status:** Design finalized and ready for implementation.

**Next Step:** Proceed with Phase 1 (Foundation) - event infrastructure implementation with TDD.
