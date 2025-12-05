# Player Personas Guide

This guide explains the different AI player strategies available in the Monopoly simulation and how to use them.

## Overview

The game includes 8 distinct player strategies, each with unique decision-making patterns inspired by Sesame Street characters. Players can be configured with any strategy to create interesting game dynamics.

## Available Strategies

### 1. DefaultStrategy

**Description**: Balanced, rule-following player with no special optimizations.

**Characteristics**:
- Buys properties when affordable
- Maintains $200 cash reserve
- Develops properties evenly
- Makes conservative decisions

**Best For**: Baseline comparison, testing game mechanics

**Usage**:
```kotlin
val player = Player("Default Player", strategy = DefaultStrategy())
```

---

### 2. SlumlordStrategy (Oscar the Grouch)

**Persona**: "The Cheap Property Hoarder"

**Characteristics**:
- Focuses on cheap properties (Brown, Light Blue)
- Builds to 4 houses maximum (no hotels)
- Maintains $200 cash reserve
- Aggressive on cheap monopolies

**Strategy**:
- Buys cheap properties aggressively
- Bids up to 150% on cheap properties
- Develops cheap monopolies quickly
- Avoids expensive properties

**Best For**: Games where cheap property strategy is viable

**Usage**:
```kotlin
val oscar = Player("Oscar", strategy = SlumlordStrategy())
```

---

### 3. ConservativeStrategy (Count von Count)

**Persona**: "The Cash Hoarder"

**Characteristics**:
- Maintains high cash reserves ($500)
- Only buys when cash > $700
- Cautious development
- Prioritizes expensive properties

**Strategy**:
- Buys expensive properties when affordable
- Bids conservatively (up to 80% of value)
- Develops slowly and carefully
- Keeps large cash buffer

**Best For**: Long games, risk-averse play

**Usage**:
```kotlin
val count = Player("Count", strategy = ConservativeStrategy())
```

---

### 4. HighRentStrategy (Big Bird)

**Persona**: "The High Rent Collector"

**Characteristics**:
- Focuses on expensive properties (Red, Yellow, Green, Dark Blue)
- Maintains $300 cash reserve
- Aggressive development on expensive properties
- Avoids cheap properties

**Strategy**:
- Buys expensive properties aggressively
- Bids up to 130% on expensive properties
- Develops expensive monopolies quickly
- Maximizes rent income

**Best For**: Games where expensive property strategy dominates

**Usage**:
```kotlin
val bigBird = Player("Big Bird", strategy = HighRentStrategy())
```

---

### 5. GamblerStrategy (Cookie Monster)

**Persona**: "The All-In Gambler"

**Characteristics**:
- Loves railroads (collects all 4)
- Minimal cash reserve ($100)
- Aggressive bidding
- High-risk, high-reward

**Strategy**:
- Buys railroads at any cost
- Bids aggressively (up to 200% on railroads)
- Develops properties quickly
- Takes big risks

**Best For**: Chaotic games, testing extreme strategies

**Usage**:
```kotlin
val cookie = Player("Cookie Monster", strategy = GamblerStrategy())
```

---

### 6. CalculatingStrategy (Bert)

**Persona**: "The Mathematical Optimizer"

**Characteristics**:
- ROI-based decisions (15% buy threshold, 20% development threshold)
- Dynamic cash reserves (2x highest opponent rent, min $300)
- Prioritizes Orange/Red properties
- Builds to 3 houses first (rent efficiency sweet spot)

**Strategy**:
- Calculates ROI for every property
- Bids in $10 increments (up to 110% value, 150% for monopoly completion)
- Develops properties with highest ROI
- Unmortgages when ROI > 10%

**Best For**: Optimal play, competitive games

**Usage**:
```kotlin
val bert = Player("Bert", strategy = CalculatingStrategy())
```

---

### 7. ChaoticStrategy (Ernie)

**Persona**: "The Chaotic Disruptor"

**Characteristics**:
- Blocks opponent monopolies aggressively
- Random cash reserves ($0-$500)
- Chaotic bidding ($5-$100 increments)
- Prioritizes hotels for intimidation
- Unpredictable behavior

**Strategy**:
- Buys to block opponents (60% random otherwise)
- Bids aggressively when blocking (up to 300% of value)
- Develops unevenly for psychological effect
- Random unmortgaging (40% chance)

**Best For**: Disruptive play, testing opponent-blocking strategies

**Usage**:
```kotlin
// Use seeded RNG for deterministic behavior in tests
val ernie = Player("Ernie", strategy = ChaoticStrategy(rng = Random(42)))

// Or use default random behavior
val ernie = Player("Ernie", strategy = ChaoticStrategy())
```

---

### 8. ImpulsiveStrategy (Elmo)

**Persona**: "The Impulsive Novice"

**Characteristics**:
- 90% chance to buy when affordable
- Minimal cash reserve ($50)
- Random decisions throughout
- Inconsistent property valuation

**Strategy**:
- Buys almost everything (90% rate)
- Random bidding (50-150% of price, $5-$100 increments)
- Random development selection
- Random unmortgaging (50% chance)

**Best For**: Simulating novice players, adding randomness

**Usage**:
```kotlin
// Use seeded RNG for deterministic behavior in tests
val elmo = Player("Elmo", strategy = ImpulsiveStrategy(rng = Random(42)))

// Or use default random behavior
val elmo = Player("Elmo", strategy = ImpulsiveStrategy())
```

---

## Strategy Comparison

| Strategy | Risk Level | Cash Reserve | Focus | Complexity |
|----------|-----------|--------------|-------|------------|
| Default | Low | $200 | Balanced | Simple |
| Slumlord (Oscar) | Medium | $200 | Cheap properties | Medium |
| Conservative (Count) | Very Low | $500 | Expensive properties | Medium |
| HighRent (Big Bird) | Medium | $300 | Expensive properties | Medium |
| Gambler (Cookie) | Very High | $100 | Railroads | Medium |
| Calculating (Bert) | Low-Medium | Dynamic | ROI optimization | High |
| Chaotic (Ernie) | High | Random | Opponent blocking | High |
| Impulsive (Elmo) | Very High | $50 | Random | Low |

---

## Creating Mixed Games

Mix strategies to create interesting dynamics:

```kotlin
val players = listOf(
    Player("Oscar", strategy = SlumlordStrategy()),
    Player("Count", strategy = ConservativeStrategy()),
    Player("Big Bird", strategy = HighRentStrategy()),
    Player("Cookie", strategy = GamblerStrategy()),
    Player("Bert", strategy = CalculatingStrategy()),
    Player("Ernie", strategy = ChaoticStrategy()),
    Player("Elmo", strategy = ImpulsiveStrategy())
)

val board = Board(players, Bank())
```

---

## Testing with Seeded Random

For deterministic testing, use seeded random number generators:

```kotlin
// All random strategies use the same seed for reproducibility
val seed = 42L
val players = listOf(
    Player("Ernie", strategy = ChaoticStrategy(rng = Random(seed))),
    Player("Elmo", strategy = ImpulsiveStrategy(rng = Random(seed)))
)
```

---

## Strategy Selection Guide

**For competitive play**: CalculatingStrategy (Bert)
- Mathematically optimal decisions
- Adapts to board state
- Consistent performance

**For aggressive play**: GamblerStrategy (Cookie Monster) or ChaoticStrategy (Ernie)
- High risk, high reward
- Disruptive to opponents
- Unpredictable

**For defensive play**: ConservativeStrategy (Count von Count)
- Maintains safety buffer
- Avoids bankruptcy
- Long-term survival

**For niche strategies**: SlumlordStrategy (Oscar) or HighRentStrategy (Big Bird)
- Focuses on specific property types
- Can dominate if strategy succeeds
- Vulnerable to counter-strategies

**For random/fun games**: ImpulsiveStrategy (Elmo)
- Unpredictable outcomes
- Simulates novice behavior
- Adds chaos to games

---

## Implementation Notes

### Random Strategies

ChaoticStrategy and ImpulsiveStrategy accept a `Random` parameter for deterministic testing:

```kotlin
// Deterministic (for tests)
val strategy = ChaoticStrategy(rng = Random(42))

// Non-deterministic (for gameplay)
val strategy = ChaoticStrategy() // Uses Random.Default
```

### Cash Reserves

Each strategy maintains different cash reserves:
- **Dynamic** (Calculating): 2x highest opponent rent, min $300
- **Random** (Chaotic): $0-$500
- **Fixed**: $50 (Impulsive), $100 (Gambler), $200 (Default/Slumlord), $300 (HighRent), $500 (Conservative)

### Property Valuation

Strategies use different valuation methods:
- **ROI-based** (Calculating): Calculates return on investment
- **Price-based** (Conservative, HighRent, Slumlord): Focuses on property price
- **Random** (Chaotic, Impulsive): Unpredictable valuations
- **Monopoly-focused** (All): Values monopoly completion highly

---

## Advanced Usage

### Custom Strategy

To create a custom strategy, implement the `PlayerStrategy` interface:

```kotlin
class CustomStrategy : PlayerStrategy {
    override fun shouldBuyProperty(deed: TitleDeed, player: Player, bank: Bank, board: Board): Boolean {
        // Your logic here
    }
    
    // Implement all other methods...
    
    override fun toString(): String = "CustomStrategy"
}
```

### Strategy Analytics

Use `toString()` to identify strategies in logs:

```kotlin
println("Player ${player.name} uses ${player.strategy}")
// Output: Player Bert uses CalculatingStrategy (Bert)
```

---

## Performance Considerations

All strategies are designed to have minimal overhead:
- Decision-making is O(n) or O(n log n) where n is number of properties
- No expensive calculations in hot paths
- Strategy overhead < 5% of game execution time

---

## See Also

- `PlayerPersonasPlan.md` - Detailed implementation plan
- `monopoly_personas.md` - High-level persona descriptions
- `TechnicalAnalysis.md` - Architecture and design decisions
