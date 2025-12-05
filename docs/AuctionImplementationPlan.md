# Auction Implementation Plan

**Feature:** Property Auctions  
**Version:** 1.0  
**Date:** December 5, 2025  
**Status:** Planning

---

## 1. Overview

### 1.1 Purpose

Implement property auctions per the official 2001 Monopoly rules. When a player lands on an unowned property and declines to purchase it at list price, the Banker conducts an auction where all players (including the declining player) can bid on the property starting at $10 with minimum increments of $1.

### 1.2 Goals

- **Rule compliance:** Implement auctions exactly as specified in `MonopolyRules2001.md` lines 169-172
- **Configurability:** Add a boolean flag to enable/disable auctions for comparative analysis
- **Strategy integration:** Leverage existing `PlayerStrategy.calculateBidIncrease()` implementations
- **Statistics integration:** Emit rich auction events for analysis via the existing event/statistics system
- **Console logging:** Clear, formatted output showing auction progression and outcomes
- **Testability:** Comprehensive test coverage with deterministic behavior

### 1.3 Success Criteria

- [ ] Auctions trigger when a player declines to purchase an unowned property
- [ ] All non-bankrupt players can participate in auctions (including players in jail per rules line 215)
- [ ] Bidding starts at $10 and increments by at least $1
- [ ] Highest bidder wins and pays their bid amount
- [ ] Auction can be disabled via `Config.enableAuctions` flag
- [ ] Auction events are emitted and captured by statistics system
- [ ] Console output clearly shows auction flow
- [ ] All existing tests pass
- [ ] New comprehensive auction tests pass

---

## 2. Architecture

### 2.1 New Components

#### 2.1.1 `Auction` Class

**Location:** `ca.jonathanfritz.monopoly.board.Auction`

**Responsibilities:**
- Orchestrate the auction process
- Track active bidders and current highest bid
- Determine auction winner
- Emit auction-related events

**Key State:**
- `deed: TitleDeed` - property being auctioned
- `participants: List<Player>` - all eligible players (non-bankrupt)
- `currentBid: Int` - current highest bid (starts at $10)
- `currentWinner: Player?` - player with current highest bid
- `activeBidders: MutableSet<Player>` - players still in the auction
- `bank: Bank` - reference for final sale
- `board: Board` - game state for strategy decisions
- `eventBus: EventBus?` - for emitting events

**Key Methods:**
- `conduct(): Player?` - main auction loop, returns winning player or null if no bids
- `private collectBids(): Map<Player, Int?>` - collect one round of bids from all active bidders
- `private processRound(bids: Map<Player, Int?>)` - update state based on bids, eliminate dropouts
- `private finalize(winner: Player)` - complete sale and emit final event

#### 2.1.2 New `GameEvent` Types

**Location:** `ca.jonathanfritz.monopoly.event.GameEvent`

Add to the sealed class hierarchy:

```kotlin
// Auction Events
data class AuctionStarted(
    val deed: TitleDeed,
    val participants: List<Player>,
    val startingBid: Int
) : GameEvent()

data class AuctionBid(
    val player: Player,
    val deed: TitleDeed,
    val bidAmount: Int,
    val previousBid: Int
) : GameEvent()

data class AuctionPlayerDropped(
    val player: Player,
    val deed: TitleDeed,
    val finalBid: Int
) : GameEvent()

data class AuctionEnded(
    val deed: TitleDeed,
    val winner: Player?,
    val winningBid: Int?,
    val participantCount: Int,
    val totalRounds: Int
) : GameEvent()
```

### 2.2 Modified Components

#### 2.2.1 `Config` Data Class

Add new configuration field:

```kotlin
// whether to conduct auctions when players decline to purchase properties
val enableAuctions: Boolean = true
```

**Default:** `true` (auctions enabled per official rules)

#### 2.2.2 `Tile.Buyable.onLanding()`

**Current behavior (line 72-77):**
```
if (player.isBuying(deed, bank, board)) {
    bank.sellDeedToPlayer(deedClass, player, board)
} else {
    // TODO: a wild auction appears!
    println("\t\t${player.name} declines to purchase the property")
}
```

**New behavior:**
```
if (player.isBuying(deed, bank, board)) {
    bank.sellDeedToPlayer(deedClass, player, board)
} else {
    println("\t\t${player.name} declines to purchase the property")
    
    if (board.config.enableAuctions) {
        println("\t\tAuction begins for ${deedClass.simpleName}!")
        val auction = Auction(deed, board.players, bank, board, eventBus)
        auction.conduct()
    }
}
```

#### 2.2.3 `Board` Class

**Modification:** Pass `config` reference to `Board` constructor

**Rationale:** `Board` needs access to `config.enableAuctions` flag. Currently `Board` doesn't have a `config` reference.

**Change:**
```kotlin
class Board(
    val players: List<Player>,
    val bank: Bank,
    val config: Config,  // NEW
    eventBus: EventBus? = null,
    rng: Random = Random.Default
)
```

**Impact:** Update all `Board` construction sites (primarily in `Monopoly.kt` and test files)

#### 2.2.4 `GameStatistics` Listener

**New tracking structures:**

```kotlin
// Auction statistics
private val auctionEvents = mutableListOf<GameEvent.AuctionStarted>()
private val auctionBids = mutableListOf<GameEvent.AuctionBid>()
private val auctionDropouts = mutableListOf<GameEvent.AuctionPlayerDropped>()
private val auctionResults = mutableListOf<GameEvent.AuctionEnded>()
```

**New event handlers in `onEvent()`:**
- Handle `AuctionStarted` - record auction initiation
- Handle `AuctionBid` - track all bids
- Handle `AuctionPlayerDropped` - track dropouts
- Handle `AuctionEnded` - record final outcome

#### 2.2.5 `StatisticsReport` Data Model

**New section:** `AuctionStatistics`

```kotlin
data class AuctionStatistics(
    val totalAuctions: Int,
    val auctionsWithWinner: Int,
    val auctionsWithNoBids: Int,
    val averageBidsPerAuction: Double,
    val averageParticipantsPerAuction: Double,
    val averageRoundsPerAuction: Double,
    val totalRevenue: Int,  // sum of all winning bids
    val averageWinningBid: Double,
    val highestBid: Int?,
    val lowestBid: Int?,
    val winsByPlayer: Map<String, Int>,  // player name -> auction wins
    val bidsByPlayer: Map<String, Int>,  // player name -> total bids placed
    val averageDiscountFromListPrice: Double,  // (list price - winning bid) / list price
    val propertiesSoldAtDiscount: Int,  // winning bid < list price
    val propertiesSoldAtPremium: Int,   // winning bid > list price
)
```

#### 2.2.6 `StatisticsFormatter`

**Console output:** Add new section "Auction Statistics" with formatted auction metrics

**JSON output:** Add `auctionStatistics` object to JSON structure

---

## 3. Detailed Algorithms

### 3.1 Auction Orchestration Algorithm

**Entry point:** `Auction.conduct(): Player?`

**Pseudocode:**

```
FUNCTION conduct() -> Player?
    // Initialize
    currentBid = STARTING_BID (10)
    currentWinner = null
    activeBidders = all non-bankrupt participants
    roundNumber = 0
    
    // Emit start event
    EMIT AuctionStarted(deed, participants, STARTING_BID)
    PRINT "Auction for {deed.name} begins! Starting bid: $10"
    
    // Main auction loop
    WHILE activeBidders.size > 1 OR (activeBidders.size == 1 AND currentWinner == null)
        roundNumber++
        PRINT "Round {roundNumber}: Current bid ${currentBid}, {activeBidders.size} bidders remaining"
        
        // Collect bids from all active bidders
        bids = collectBids()
        
        // Process bids
        hasNewBid = false
        FOR EACH (player, bidAmount) IN bids
            IF bidAmount == null
                // Player drops out
                EMIT AuctionPlayerDropped(player, deed, currentBid)
                PRINT "{player.name} drops out at ${currentBid}"
                activeBidders.remove(player)
            ELSE IF bidAmount > currentBid
                // Valid bid
                IF bidAmount < currentBid + MINIMUM_INCREMENT (1)
                    THROW IllegalArgumentException("Bid must be at least $1 higher")
                END IF
                
                previousBid = currentBid
                currentBid = bidAmount
                currentWinner = player
                hasNewBid = true
                
                EMIT AuctionBid(player, deed, bidAmount, previousBid)
                PRINT "{player.name} bids ${bidAmount}"
            ELSE
                // Invalid bid (not higher than current)
                PRINT "WARNING: {player.name} bid ${bidAmount} is not higher than current ${currentBid}"
                activeBidders.remove(player)
            END IF
        END FOR
        
        // Check termination conditions
        IF activeBidders.isEmpty()
            BREAK  // No one left
        END IF
        
        IF activeBidders.size == 1 AND currentWinner IN activeBidders AND NOT hasNewBid
            BREAK  // Only winner remains and they didn't bid this round
        END IF
        
        // Safety: prevent infinite loops
        IF roundNumber > MAX_ROUNDS (100)
            PRINT "WARNING: Auction exceeded maximum rounds"
            BREAK
        END IF
    END WHILE
    
    // Finalize auction
    IF currentWinner != null
        finalize(currentWinner)
        EMIT AuctionEnded(deed, currentWinner, currentBid, participants.size, roundNumber)
        PRINT "Auction won by {currentWinner.name} for ${currentBid}"
        RETURN currentWinner
    ELSE
        EMIT AuctionEnded(deed, null, null, participants.size, roundNumber)
        PRINT "Auction ended with no winner - property remains with bank"
        RETURN null
    END IF
END FUNCTION
```

### 3.2 Bid Collection Algorithm

**Entry point:** `Auction.collectBids(): Map<Player, Int?>`

**Pseudocode:**

```
FUNCTION collectBids() -> Map<Player, Int?>
    bids = empty map
    
    FOR EACH player IN activeBidders
        // Delegate to player's strategy
        bidAmount = player.strategy.calculateBidIncrease(
            deed,
            currentBid,
            player,
            bank,
            board
        )
        
        bids[player] = bidAmount
    END FOR
    
    RETURN bids
END FUNCTION
```

**Note:** This leverages the existing `PlayerStrategy.calculateBidIncrease()` method. All 8 strategies already implement this method with varying behaviors:
- `DefaultStrategy`: returns `null` (never bids)
- `CalculatingStrategy`: bids up to 110% of strategic value, $10 increments
- `GamblerStrategy`: aggressive bidding, $50-100 increments, up to 200% on railroads
- `ConservativeStrategy`: cautious bidding, respects high cash reserves
- Others: various strategies already implemented

### 3.3 Auction Finalization Algorithm

**Entry point:** `Auction.finalize(winner: Player)`

**Pseudocode:**

```
FUNCTION finalize(winner: Player)
    // Validate winner can afford their bid
    IF winner.money < currentBid
        // Attempt liquidation
        TRY
            winner.liquidateAssets(currentBid, bank, board)
        CATCH BankruptcyException
            // Winner cannot afford - this shouldn't happen if strategies are correct
            PRINT "ERROR: Auction winner {winner.name} cannot afford bid ${currentBid}"
            winner.declareBankruptcy(bank, board)
            RETURN  // Property remains with bank
        END TRY
    END IF
    
    // Complete the sale
    bank.sellDeedToPlayer(deed::class, winner, board, auctionPrice = currentBid)
    
    PRINT "{winner.name} purchased {deed.name} at auction for ${currentBid} (list price: ${deed.price})"
END FUNCTION
```

**Note:** This requires a modification to `Bank.sellDeedToPlayer()` to accept an optional `auctionPrice` parameter (defaults to `deed.price` for normal purchases).

### 3.4 Bank Sale Modification

**Current signature:**
```kotlin
fun sellDeedToPlayer(deedClass: KClass<out TitleDeed>, player: Player, board: Board)
```

**New signature:**
```kotlin
fun sellDeedToPlayer(
    deedClass: KClass<out TitleDeed>,
    player: Player,
    board: Board,
    auctionPrice: Int? = null  // NEW: override price for auctions
)
```

**Logic change:**
```
val price = auctionPrice ?: deed.price

// Existing affordability check and charge logic uses `price` instead of `deed.price`
```

**Event emission:**
```
PropertyPurchased(player, deed, price)  // price reflects auction or list price
```

---

## 4. Configuration & Feature Flags

### 4.1 Config Field

**Field:** `enableAuctions: Boolean = true`

**Usage:**
- `Tile.Buyable.onLanding()` checks this flag before creating `Auction`
- When `false`, behavior matches current implementation (property remains with bank, no auction)
- When `true`, auction is conducted

### 4.2 Statistics Interaction

**When auctions are disabled:**
- No auction events are emitted
- `AuctionStatistics` section shows all zeros/nulls
- Console/JSON output includes note: "Auctions disabled"

**When auctions are enabled:**
- Full auction event stream captured
- Rich statistics available for analysis

---

## 5. Event System Integration

### 5.1 Event Flow

**Typical auction event sequence:**

1. `PropertyOffered` (existing, emitted when player lands on unowned property)
2. `PurchaseDecision` (existing, emitted when player declines)
3. `AuctionStarted` (NEW)
4. `AuctionBid` (NEW, multiple per round)
5. `AuctionPlayerDropped` (NEW, as players drop out)
6. `AuctionEnded` (NEW)
7. `PropertyPurchased` (existing, if auction has winner)

### 5.2 Event Data Completeness

Each auction event includes:
- **Property identification:** `deed: TitleDeed`
- **Player context:** which players are involved
- **Bid amounts:** current and previous bids for comparison
- **Outcome data:** winner, winning bid, participation metrics

This enables rich statistical analysis:
- Average discount/premium from list price
- Player bidding aggressiveness by strategy
- Auction participation rates
- Correlation between auction outcomes and game outcomes

---

## 6. Console Output Format

### 6.1 Auction Start

```
		Oscar landed on Mediterranean Avenue. It can be purchased for $60
		Oscar declines to purchase the property
		Auction begins for Mediterranean Avenue!
	
	AUCTION: Mediterranean Avenue (List Price: $60)
	Starting Bid: $10
	Participants: Oscar, Count, Big Bird, Cookie Monster
```

### 6.2 Auction Rounds

```
	Round 1: Current bid $10, 4 bidders remaining
		Cookie Monster bids $60
		Oscar drops out at $10
		Count drops out at $10
		Big Bird bids $70
	
	Round 2: Current bid $70, 2 bidders remaining
		Cookie Monster bids $80
		Big Bird drops out at $70
	
	Round 3: Current bid $80, 1 bidder remaining
		Cookie Monster passes (no increase)
```

### 6.3 Auction End

```
	AUCTION RESULT: Cookie Monster wins Mediterranean Avenue for $80
	Cookie Monster purchased Mediterranean Avenue at auction for $80 (list price: $60)
```

**Alternative (no winner):**
```
	AUCTION RESULT: No bids received - Mediterranean Avenue remains with the bank
```

### 6.4 Formatting Consistency

- Use same indentation as existing tile landing output (`\t\t` for main actions)
- Use `\t` for auction-specific output to visually nest it
- Include blank lines before/after auction block for readability
- Match existing money formatting (`$XX` format)

---

## 7. Testing Strategy

### 7.1 Unit Tests

**New test file:** `AuctionTest.kt`

**Test cases:**

1. **Basic auction mechanics:**
   - Single bidder wins at starting price ($10)
   - Multiple bidders, highest wins
   - All players drop out (no winner)
   - Bidder drops out mid-auction

2. **Bid validation:**
   - Bid must exceed current bid
   - Bid must be at least $1 higher
   - Invalid bids cause player to drop out

3. **Strategy integration:**
   - Each strategy's `calculateBidIncrease()` is called correctly
   - Strategy can return null to drop out
   - Strategy receives correct game state

4. **Edge cases:**
   - Only one eligible player (bankrupt players excluded)
   - Winner cannot afford bid (triggers liquidation/bankruptcy)
   - All players bankrupt during auction (shouldn't happen, but defensive)

5. **Event emission:**
   - `AuctionStarted` emitted with correct participants
   - `AuctionBid` emitted for each valid bid
   - `AuctionPlayerDropped` emitted when players drop
   - `AuctionEnded` emitted with correct outcome
   - `PropertyPurchased` emitted with auction price (not list price)

### 7.2 Integration Tests

**Modified test file:** `MonopolyTest.kt` or new `AuctionIntegrationTest.kt`

**Test cases:**

1. **End-to-end auction flow:**
   - Player lands on property, declines, auction runs, winner purchases
   - Verify property ownership transfers correctly
   - Verify winner's money decreases by bid amount

2. **Config flag behavior:**
   - `enableAuctions = true`: auction occurs
   - `enableAuctions = false`: no auction, property stays with bank

3. **Statistics collection:**
   - Auction events captured by `GameStatistics`
   - `StatisticsReport` includes auction metrics
   - Console/JSON output includes auction section

4. **Multi-property auctions:**
   - Multiple auctions in same game
   - Same player wins multiple auctions
   - Different strategies compete in auctions

5. **Deterministic behavior:**
   - With seeded RNG, auction outcomes are reproducible
   - Random strategies (Chaotic, Impulsive) produce consistent results with same seed

### 7.3 Strategy-Specific Tests

**Modified test files:** `*StrategyTest.kt` files

**Test cases for each strategy:**

1. **Bidding behavior verification:**
   - Verify `calculateBidIncrease()` returns expected values
   - Verify strategy respects cash reserves
   - Verify strategy drops out at appropriate price points

2. **Monopoly completion premium:**
   - Strategies bid higher when auction would complete monopoly
   - Verify `wouldCompleteMonopoly()` helper is used correctly

3. **Property type preferences:**
   - `GamblerStrategy` bids aggressively on railroads
   - `SlumlordStrategy` focuses on cheap properties
   - `HighRentStrategy` prioritizes expensive properties

### 7.4 Regression Tests

**Ensure existing tests pass:**

- All 411 existing tests must continue to pass
- No changes to existing game mechanics (except auction trigger point)
- Backward compatibility: `enableAuctions = false` produces identical behavior to pre-auction implementation

---

## 8. Implementation Phases

### Phase 1: Core Auction Infrastructure
**Goal:** Implement basic auction mechanics without statistics

**Tasks:**
1. Add `enableAuctions` field to `Config` (default `true`)
2. Add `config` parameter to `Board` constructor
3. Update all `Board` construction sites (Monopoly, tests)
4. Create `Auction` class with core orchestration logic
5. Add new `GameEvent` types for auctions
6. Modify `Tile.Buyable.onLanding()` to trigger auctions
7. Modify `Bank.sellDeedToPlayer()` to accept `auctionPrice` parameter
8. Add console output for auction flow

**Deliverables:**
- `Auction.kt` with full implementation
- Modified `Config.kt`, `Board.kt`, `Tile.kt`, `Bank.kt`
- Modified `GameEvent.kt` with 4 new event types
- Console output shows auction progression
- Auctions can be disabled via config flag

**Tests:**
- `AuctionTest.kt` with basic mechanics tests (15-20 tests)
- Update existing tests to pass `config` to `Board`
- Verify all existing tests still pass

**Acceptance:**
- Auctions trigger when player declines purchase
- Bidding proceeds correctly with multiple players
- Winner purchases property at bid price
- Console output is clear and formatted correctly
- Config flag works (auctions can be disabled)

---

### Phase 2: Statistics Integration
**Goal:** Capture auction events and generate statistics

**Tasks:**
1. Add auction event tracking to `GameStatistics`
2. Create `AuctionStatistics` data class
3. Implement auction metrics calculation in `GameStatistics.generateReport()`
4. Add auction section to `StatisticsFormatter.formatConsole()`
5. Add auction section to `StatisticsFormatter.formatJson()`

**Deliverables:**
- Modified `GameStatistics.kt` with auction event handlers
- New `AuctionStatistics` data class in `StatisticsReport.kt`
- Modified `StatisticsFormatter.kt` with auction output

**Tests:**
- `GameStatisticsTest.kt` additions for auction event handling (5-10 tests)
- `GameStatisticsIntegrationTest.kt` additions for end-to-end auction stats (2-3 tests)
- Verify auction metrics are calculated correctly
- Verify console/JSON output includes auction section

**Acceptance:**
- Auction events are captured by statistics system
- `StatisticsReport` includes comprehensive auction metrics
- Console output shows auction statistics section
- JSON output includes `auctionStatistics` object
- Statistics show zeros when auctions are disabled

---

### Phase 3: Strategy Testing & Refinement
**Goal:** Verify all strategies behave correctly in auctions

**Tasks:**
1. Add auction-specific tests to each `*StrategyTest.kt` file
2. Create `AuctionIntegrationTest.kt` for multi-strategy auction scenarios
3. Test deterministic behavior with seeded RNG
4. Verify strategy bidding logic matches documented behavior
5. Test edge cases (bankruptcy during auction, liquidation, etc.)

**Deliverables:**
- Auction tests in all 8 strategy test files (3-5 tests each = 24-40 tests)
- New `AuctionIntegrationTest.kt` with cross-strategy tests (10-15 tests)
- Deterministic test cases with seeded RNG

**Tests:**
- Strategy-specific bidding behavior
- Monopoly completion premium
- Property type preferences
- Cash reserve respect
- Dropout conditions

**Acceptance:**
- All strategies participate in auctions correctly
- Bidding behavior matches strategy documentation
- Random strategies are deterministic with seeded RNG
- Edge cases are handled gracefully
- All tests pass (existing + new)

---

### Phase 4: Documentation & Polish
**Goal:** Complete documentation and final refinements

**Tasks:**
1. Update `TechnicalAnalysis.md` with auction implementation details
2. Update `README.md` with auction configuration examples
3. Add auction examples to `PlayerPersonasGuide.md` showing strategy behaviors
4. Review and refine console output formatting
5. Code review and cleanup
6. Final regression testing

**Deliverables:**
- Updated `TechnicalAnalysis.md` (Section 4.6: Auction Algorithm)
- Updated `README.md` with auction examples
- Updated `PlayerPersonasGuide.md` with auction strategy behaviors
- Polished console output
- Clean, documented code

**Tests:**
- Full regression suite (all 411+ existing tests)
- All new auction tests (50-70 new tests)
- Manual testing with various configurations

**Acceptance:**
- Documentation is comprehensive and accurate
- Console output is polished and consistent
- Code is clean and well-commented
- All tests pass
- Feature is ready for production use

---

## 9. Open Questions

### 9.1 Bankruptcy During Auction

**Question:** What happens if a player goes bankrupt during an auction (e.g., due to a Chance card drawn by another player)?

**Proposed answer:** Remove bankrupt player from `activeBidders` immediately. If they were the current winner, reset `currentWinner` to null and continue auction.

**Needs confirmation:** Is this the correct interpretation of the rules?

---

### 9.2 Auction Timeout

**Question:** Should there be a maximum number of auction rounds to prevent infinite loops?

**Proposed answer:** Yes, implement a safety limit (e.g., 100 rounds). If reached, log a warning and end auction with current winner.

**Rationale:** Defensive programming. Strategies should naturally converge, but this prevents bugs from hanging the game.

---

### 9.3 Minimum Bid Increment

**Question:** Rules say "raises as small as $1" - should we enforce exactly $1 minimum, or allow strategies to bid any amount above current bid?

**Proposed answer:** Allow any amount > current bid, but strategies typically use $1+ increments. Validate that new bid > current bid, but don't enforce a specific increment.

**Rationale:** Gives strategies flexibility (e.g., GamblerStrategy uses $50-100 increments for aggressive bidding).

---

### 9.4 Auction Price in PropertyPurchased Event

**Question:** Should `PropertyPurchased` event distinguish between list price purchases and auction purchases?

**Proposed answer:** Use the `price` field to reflect actual price paid (list price or auction price). Statistics can calculate discount/premium by comparing to `deed.price`.

**Alternative:** Add an `isAuction: Boolean` field to `PropertyPurchased` event.

**Needs decision:** Which approach is preferred?

---

### 9.5 Building Auctions

**Question:** The TODO in `Bank.kt` mentions auctions for houses/hotels when multiple players want to build simultaneously. Should this be included in this implementation?

**Proposed answer:** No, this is a separate feature. Focus on property auctions only. Building auctions can be a future enhancement.

**Rationale:** Property auctions are more common and impactful. Building auctions are a rare edge case (requires simultaneous building attempts when stock is limited).

---

## 10. Risk Assessment

### 10.1 High Risk Items

1. **Board constructor changes:** Adding `config` parameter to `Board` requires updating many test files
   - **Mitigation:** Use IDE refactoring tools, compile after change to catch all sites

2. **Strategy bidding logic:** Existing `calculateBidIncrease()` implementations are untested
   - **Mitigation:** Phase 3 focuses entirely on strategy testing

3. **Bankruptcy during auction:** Complex edge case with cascading effects
   - **Mitigation:** Defensive programming, comprehensive edge case tests

### 10.2 Medium Risk Items

1. **Event emission order:** Auction events must interleave correctly with existing events
   - **Mitigation:** Integration tests verify event sequences

2. **Statistics calculation:** New metrics must be calculated correctly
   - **Mitigation:** Unit tests for each metric calculation

3. **Console output formatting:** Must match existing style and be readable
   - **Mitigation:** Manual review, compare to existing output

### 10.3 Low Risk Items

1. **Config flag:** Simple boolean, well-tested pattern
2. **Auction class isolation:** New class with minimal dependencies
3. **Event types:** Sealed class additions are type-safe

---

## 11. Success Metrics

### 11.1 Functional Metrics

- [ ] 100% of existing tests pass
- [ ] 50+ new auction-specific tests pass
- [ ] Auctions trigger in 100% of decline-to-purchase scenarios (when enabled)
- [ ] All 8 strategies participate in auctions correctly
- [ ] Statistics capture 100% of auction events

### 11.2 Code Quality Metrics

- [ ] Zero compiler warnings
- [ ] Zero linter violations
- [ ] All public methods have KDoc comments
- [ ] Test coverage for auction code > 90%

### 11.3 Documentation Metrics

- [ ] TechnicalAnalysis.md updated with auction algorithms
- [ ] README.md includes auction configuration examples
- [ ] PlayerPersonasGuide.md includes auction strategy behaviors
- [ ] All open questions resolved and documented

---

## 12. Future Enhancements

**Not included in this implementation, but documented for future consideration:**

1. **Building auctions:** Implement auctions for houses/hotels when multiple players want to build simultaneously (per TODO in `Bank.kt`)

2. **Auction strategies:** Add dedicated auction strategy methods beyond `calculateBidIncrease()`:
   - `shouldParticipateInAuction()` - opt out of specific auctions
   - `calculateMaximumBid()` - reveal max upfront for analysis

3. **Auction history:** Track auction outcomes per property to enable learning strategies

4. **Auction visualization:** Generate charts showing bid progression over rounds

5. **Monte Carlo analysis:** Compare game outcomes with auctions enabled vs. disabled across thousands of games

---

## 13. Appendix: Existing Strategy Bidding Behaviors

**Summary of `calculateBidIncrease()` implementations:**

| Strategy | Bidding Behavior | Max Bid | Increment |
|----------|------------------|---------|-----------|
| **DefaultStrategy** | Never bids | N/A | N/A |
| **SlumlordStrategy** | Bids on cheap properties (Brown/Light Blue) | 120% of price | $5 |
| **ConservativeStrategy** | Cautious, high reserves | 90% of price | $5 |
| **HighRentStrategy** | Aggressive on expensive properties | 150% of price | $20 |
| **GamblerStrategy** | Very aggressive, especially railroads | 200-250% of price | $50-100 |
| **CalculatingStrategy** | ROI-based, efficient | 110% of strategic value (150% for monopoly) | $10 |
| **ChaoticStrategy** | Random, opponent blocking | 80-150% of price | $1-50 |
| **ImpulsiveStrategy** | Random, inconsistent | 50-200% of price | $1-100 |

**Note:** All strategies respect their cash reserves and drop out when bid exceeds their maximum.
