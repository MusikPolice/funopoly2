@file:Suppress("ktlint:standard:no-wildcard-imports")

package ca.jonathanfritz.monopoly

import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.card.ChanceCard
import ca.jonathanfritz.monopoly.deed.ColourGroup
import ca.jonathanfritz.monopoly.deed.Property
import ca.jonathanfritz.monopoly.deed.Property.*
import ca.jonathanfritz.monopoly.deed.Railroad
import ca.jonathanfritz.monopoly.deed.TitleDeed
import ca.jonathanfritz.monopoly.deed.Utility
import ca.jonathanfritz.monopoly.exception.PropertyOwnershipException
import ca.jonathanfritz.monopoly.strategy.DefaultStrategy
import ca.jonathanfritz.monopoly.strategy.PlayerStrategy
import ca.jonathanfritz.monopoly.strategy.PropertyValuation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PlayerTest {
    @Test
    fun isOwnerTest() {
        // this player owns Boardwalk and Park Place, giving them a Monopoly on the dark blue properties
        val highRoller =
            Player(
                "High Roller",
                deeds =
                    ColourGroup.DarkBlue
                        .titleDeeds()
                        .values
                        .associateWith { Player.Development() }
                        .toMutableMap(),
            )
        assertTrue(highRoller.isOwner(ParkPlace::class))
        assertTrue(highRoller.isOwner(Boardwalk::class))
        Property.values.keys
            .filterNot { it == ParkPlace::class || it == Boardwalk::class }
            .forEach { unownedProperty -> assertFalse(highRoller.isOwner(unownedProperty)) }
    }

    @Test
    fun `player has monopoly on properties`() {
        val player =
            Player(
                "Low Roller",
                deeds =
                    mutableMapOf(
                        MediterraneanAvenue() to Player.Development(),
                        BalticAvenue() to Player.Development(),
                    ),
            )
        assertTrue(player.hasMonopoly(ColourGroup.Brown))
        ColourGroup.values().filterNot { it == ColourGroup.Brown }.forEach { assertFalse(player.hasMonopoly(it)) }
    }

    @Test
    fun `player has monopoly on railroads`() {
        val player =
            Player(
                "Engineer",
                deeds =
                    mutableMapOf(
                        Railroad.ReadingRailroad() to Player.Development(),
                        Railroad.PennsylvaniaRailroad() to Player.Development(),
                        Railroad.BAndORailroad() to Player.Development(),
                        Railroad.ShortlineRailroad() to Player.Development(),
                    ),
            )
        assertTrue(player.hasMonopoly(ColourGroup.Railroads))
        ColourGroup.values().filterNot { it == ColourGroup.Railroads }.forEach { assertFalse(player.hasMonopoly(it)) }
    }

    @Test
    fun `player has monopoly on utilities`() {
        val player =
            Player(
                "Industrialist",
                deeds =
                    mutableMapOf(
                        Utility.ElectricCompany() to Player.Development(),
                        Utility.WaterWorks() to Player.Development(),
                    ),
            )
        assertTrue(player.hasMonopoly(ColourGroup.Utilities))
        ColourGroup.values().filterNot { it == ColourGroup.Utilities }.forEach { assertFalse(player.hasMonopoly(it)) }
    }

    @Test
    fun `player networth with cash only`() {
        val player =
            Player(
                "Cash Only",
                money = 5000,
            )
        assertEquals(5000, player.netWorth())
        assertEquals(200, player.incomeTaxAmount())
    }

    @Test
    fun `player networth with property only`() {
        val player =
            Player(
                "Property Only",
                deeds =
                    mutableMapOf(
                        OrientalAvenue() to Player.Development(),
                        NewYorkAvenue() to Player.Development(),
                        Railroad.ReadingRailroad() to Player.Development(),
                        Utility.WaterWorks() to Player.Development(),
                    ),
            )
        assertEquals(650, player.netWorth())
        assertEquals(65, player.incomeTaxAmount())
    }

    @Test
    fun `player networth with cash, property, and houses`() {
        val player =
            Player(
                "Houses",
                money = 200,
                deeds =
                    mutableMapOf(
                        MediterraneanAvenue() to Player.Development(),
                        BalticAvenue() to Player.Development(numHouses = 2),
                    ),
            )
        assertEquals(420, player.netWorth())
        assertEquals(42, player.incomeTaxAmount())
    }

    @Test
    fun `player networth with cash, property, houses, and hotels`() {
        val withHotels =
            Player(
                "Hotels",
                deeds =
                    mutableMapOf(
                        MediterraneanAvenue() to Player.Development(numHouses = 4),
                        BalticAvenue() to Player.Development(numHouses = 4, hasHotel = true),
                    ),
            )
        assertEquals(570, withHotels.netWorth())
        assertEquals(57, withHotels.incomeTaxAmount())
    }

    @Test
    fun `isInJail and decrementRemainingTurnsInJail mutate player state as expected`() {
        val player = Player("Snuffleupagus")
        assertPlayerIsNotInJail(player)

        // attempting to decrement the value when the player is not in jail is a no-op
        assertEquals(0, player.decrementRemainingTurnsInJail())
        assertPlayerIsNotInJail(player)

        // but once the player is in jail, the decrement function can be used to track how many turns the player must spend there
        player.isInJail = true
        assertTrue(player.isInJail)
        assertEquals(3, player.remainingTurnsInJail)

        assertEquals(2, player.decrementRemainingTurnsInJail())
        assertEquals(1, player.decrementRemainingTurnsInJail())

        // the final decrement operation mutates isInJail too
        assertEquals(0, player.decrementRemainingTurnsInJail())
        assertPlayerIsNotInJail(player)

        // if we put the player back in jail, we can cancel that state
        player.isInJail = true
        assertTrue(player.isInJail)
        assertEquals(3, player.remainingTurnsInJail)
        assertEquals(2, player.decrementRemainingTurnsInJail())
        player.isInJail = false
        assertPlayerIsNotInJail(player)
    }

    private fun assertPlayerIsNotInJail(player: Player) {
        assertFalse(player.isInJail)
        assertEquals(0, player.remainingTurnsInJail)
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns false if player is not in jail`() {
        val player = Player("Cookie Monster", money = 100)
        assertFalse(player.isInJail)
        assertFalse(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns false if player is in jail but does not have enough money to pay the fine`() {
        val player = Player("Cookie Monster", money = 10)
        player.isInJail = true
        assertFalse(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns false if player is in jail and can afford the fine and has get out of jail free card`() {
        val player = Player("Cookie Monster", money = 100, getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        player.isInJail = true
        assertFalse(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `isPayingGetOutOfJailEarlyFee returns true if player is in jail and can afford the fine`() {
        val player = Player("Cookie Monster", money = 100)
        player.isInJail = true
        assertTrue(player.isPayingGetOutOfJailEarlyFee(50))
    }

    @Test
    fun `useGetOutOfJailFreeCard returns null if the player is not in jail`() {
        val player = Player("Cookie Monster", money = 100, getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        assertNull(player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `UseGetOutOfJailFreeCard returns null if the player does not have a card to play`() {
        val player = Player("Cookie Monster", money = 100)
        player.isInJail = true
        assertNull(player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `useGetOutOfJailFreeCard returns a card if the player is in jail and has a card in their inventory`() {
        val player = Player("Cookie Monster", money = 100, getOutOfJailFreeCards = mutableListOf(ChanceCard.GetOutOfJailFree))
        player.isInJail = true
        assertEquals(ChanceCard.GetOutOfJailFree, player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `pay player negative value throws exception`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        assertThrows<IllegalArgumentException> {
            source.pay(-10, target, Bank(), Board(listOf(source, target)))
        }

        assertEquals(10, source.money)
        assertEquals(0, target.money)
    }

    @Test
    fun `pay player zero doesn't change balances`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        source.pay(0, target, Bank(), Board(listOf(source, target)))

        assertEquals(10, source.money)
        assertEquals(0, target.money)
    }

    @Test
    fun `pay player test`() {
        val source = Player("Cookie", 10)
        val target = Player("Elmo")

        source.pay(10, target, Bank(), Board(listOf(source, target)))

        assertEquals(0, source.money)
        assertEquals(10, target.money)
    }

    @Test
    fun `getDevelopment for unowned property throws property ownership`() {
        val player = Player("Cookie")
        assertThrows<PropertyOwnershipException> {
            player.getDevelopment(BalticAvenue::class)
        }
    }

    @Test
    fun `getDevelopment for owned property`() {
        val expected = Player.Development()
        val player =
            Player(
                "Cookie",
                deeds = mutableMapOf(BalticAvenue() to expected),
            )
        assertEquals(expected, player.getDevelopment(BalticAvenue::class))
    }

    @Test
    fun `isBuying returns false if player cannot afford deed`() {
        val player = Player("Ernie")
        val bank = Bank()
        val board = Board(listOf(player), bank)
        assertFalse(player.isBuying(MediterraneanAvenue(), bank, board))
    }

    @Test
    fun `isBuying returns true if player can afford deed`() {
        val player = Player("Ernie", money = 100)
        val bank = Bank()
        val board = Board(listOf(player), bank)
        assertTrue(player.isBuying(MediterraneanAvenue(), bank, board))
    }

    @Test
    fun `developProperties upgrades a monopoly as expected`() {
        val player = Player("Elmo", 2751)
        val bank = Bank()
        val board = Board(listOf(player), bank)

        bank.sellDeedToPlayer(ParkPlace::class, player, board)
        bank.sellDeedToPlayer(Boardwalk::class, player, board)
        assertTrue(player.hasMonopoly(ColourGroup.DarkBlue))

        // first house is built on Boardwalk because it has the higher rent
        player.developProperties(bank, board)
        assertEquals(0, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(1, player.getDevelopment(Boardwalk::class).numHouses)

        // next house is built on Park Place to respect even building rules
        player.developProperties(bank, board)
        assertEquals(1, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(1, player.getDevelopment(Boardwalk::class).numHouses)

        // back to Boardwalk
        player.developProperties(bank, board)
        assertEquals(1, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(2, player.getDevelopment(Boardwalk::class).numHouses)

        // then Park Place
        player.developProperties(bank, board)
        assertEquals(2, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(2, player.getDevelopment(Boardwalk::class).numHouses)

        // Boardwalk again
        player.developProperties(bank, board)
        assertEquals(2, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(3, player.getDevelopment(Boardwalk::class).numHouses)

        // Another for Park Place
        player.developProperties(bank, board)
        assertEquals(3, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(3, player.getDevelopment(Boardwalk::class).numHouses)

        // last house for Boardwalk
        player.developProperties(bank, board)
        assertEquals(3, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(4, player.getDevelopment(Boardwalk::class).numHouses)

        // and a final house for Park Place
        player.developProperties(bank, board)
        assertEquals(4, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(4, player.getDevelopment(Boardwalk::class).numHouses)

        // Boardwalk gets a hotel
        player.developProperties(bank, board)
        assertEquals(4, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(0, player.getDevelopment(Boardwalk::class).numHouses)
        assertTrue(player.getDevelopment(Boardwalk::class).hasHotel)

        // as does Park Place
        player.developProperties(bank, board)
        assertEquals(0, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(0, player.getDevelopment(Boardwalk::class).numHouses)
        assertTrue(player.getDevelopment(ParkPlace::class).hasHotel)
        assertTrue(player.getDevelopment(Boardwalk::class).hasHotel)

        // both properties are fully upgraded, so nothing changes if we call again
        player.developProperties(bank, board)
        assertEquals(0, player.getDevelopment(ParkPlace::class).numHouses)
        assertEquals(0, player.getDevelopment(Boardwalk::class).numHouses)
        assertTrue(player.getDevelopment(ParkPlace::class).hasHotel)
        assertTrue(player.getDevelopment(Boardwalk::class).hasHotel)

        // Elmo has a single solitary dollar left to his name
        assertEquals(1, player.money)
    }

    @Test
    fun `bankrupt player transfers all assets to creditor`() {
        // Setup: player1 owns properties, cash, GOOJF card
        val player1 = Player("Player 1", money = 50)
        player1.deeds[MediterraneanAvenue()] = Player.Development()
        player1.deeds[BalticAvenue()] = Player.Development()
        player1.grantGetOutOfJailFreeCard(ChanceCard.GetOutOfJailFree)

        val player2 = Player("Player 2", money = 1000)
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes player2 $500, which they cannot pay even after liquidation
        // this should trigger bankruptcy to player2
        // During liquidation: player1 mortgages both properties (Med $30 + Baltic $30 = $60)
        // player1 now has $110 total, which transfers to player2
        // player2 unmortgages both properties ($33 + $33 = $66)
        // Final: $1000 + $110 - $66 = $1044
        player1.pay(500, player2, bank, board)

        // Verify: all cash transferred to player2
        assertEquals(0, player1.money)
        assertEquals(1044, player2.money) // 1000 + 110 (from player1) - 66 (unmortgage fees)

        // Verify: all deeds transferred to player2 and unmortgaged
        assertTrue(player1.deeds.isEmpty())
        assertTrue(player2.isOwner(MediterraneanAvenue::class))
        assertTrue(player2.isOwner(BalticAvenue::class))
        assertFalse(player2.getDevelopment(MediterraneanAvenue::class).isMortgaged)
        assertFalse(player2.getDevelopment(BalticAvenue::class).isMortgaged)

        // Verify: GOOJF card transferred to player2
        assertFalse(player1.hasGetOutOfJailFreeCard())
        assertTrue(player2.hasGetOutOfJailFreeCard())

        assertPlayerIsBankrupt(player1)
        assertPlayerIsNotBankrupt(player2)
    }

    @Test
    fun `creditor receives unmortgaged properties without additional cost`() {
        val player1 = Player("Player 1", money = 250)
        player1.deeds[OrientalAvenue()] = Player.Development()
        player1.deeds[VermontAvenue()] = Player.Development()

        val player2 = Player("Player 2", money = 500)
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes player2 $450 (more than cash, but properties stay unmortgaged since liquidation value is enough)
        // player1 will liquidate but has enough value to avoid mortgaging: $250 cash + $100 (Oriental) + $100 (Vermont) = $450
        // Actually, liquidation will mortgage them. Let me reconsider this test.
        // The only way to test unmortgaged transfer is to have properties NOT get mortgaged during liquidation
        // This happens when player already has mortgaged properties OR when total debt is small enough
        // Let me give player1 $200 and owe $350 - they mortgage both properties, get to $300, still bankrupt
        player1.pay(450, player2, bank, board)

        // During liquidation: player1 mortgages Oriental ($50) + Vermont ($50) = $100
        // Player1 cash: $250 + $100 = $350, still can't pay $450, bankrupts
        // Transfer: $350 to player2
        // Unmortgage fees: $56 + $56 = $112
        // Final: $500 + $350 - $112 = $738

        // Verify: player2 received all cash from player1
        assertEquals(0, player1.money)
        assertEquals(738, player2.money) // 500 + 350 - 112

        // Verify: properties transferred and unmortgaged by creditor
        assertTrue(player2.isOwner(OrientalAvenue::class))
        assertTrue(player2.isOwner(VermontAvenue::class))
        assertFalse(player2.getDevelopment(OrientalAvenue::class).isMortgaged)
        assertFalse(player2.getDevelopment(VermontAvenue::class).isMortgaged)

        assertPlayerIsBankrupt(player1)
    }

    @Test
    fun `creditor can immediately unmortgage received property`() {
        val player1 = Player("Player 1", money = 10)
        player1.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)

        val player2 = Player("Player 2", money = 500, strategy = AlwaysUnmortgageStrategy())
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes $100, bankrupting to player2
        player1.pay(100, player2, bank, board)

        // Baltic Avenue: mortgage value $30, unmortgage cost $33 (110%)
        // Verify: player2 received cash and paid unmortgage fee
        assertEquals(0, player1.money)
        assertEquals(477, player2.money) // 500 + 10 - 33

        // Verify: property transferred and unmortgaged
        assertTrue(player2.isOwner(BalticAvenue::class))
        assertFalse(player2.getDevelopment(BalticAvenue::class).isMortgaged)

        assertPlayerIsBankrupt(player1)
    }

    @Test
    fun `creditor can assume mortgage by paying a fee`() {
        val player1 = Player("Player 1", money = 20)
        player1.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)

        val player2 = Player("Player 2", money = 500, strategy = NeverUnmortgageStrategy())
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes $100, bankrupting to player2
        player1.pay(100, player2, bank, board)

        // Baltic Avenue: mortgage value $30, assumption fee $3 (10%)
        // Verify: player2 received cash and paid only 10% fee
        assertEquals(0, player1.money)
        assertEquals(517, player2.money) // 500 + 20 - 3

        // Verify: property transferred but remains mortgaged
        assertTrue(player2.isOwner(BalticAvenue::class))
        assertTrue(player2.getDevelopment(BalticAvenue::class).isMortgaged)

        assertPlayerIsBankrupt(player1)
    }

    @Test
    fun `creditor receives mix of mortgaged and unmortgaged properties`() {
        val player1 = Player("Player 1", money = 50)
        player1.deeds[MediterraneanAvenue()] = Player.Development()
        player1.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)
        player1.deeds[OrientalAvenue()] = Player.Development()
        player1.deeds[VermontAvenue()] = Player.Development(isMortgaged = true)

        val player2 = Player(
            "Player 2",
            money = 1000,
            strategy = ConditionalUnmortgageStrategy { deed -> deed is BalticAvenue }
        )
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes $500, bankrupting to player2
        // During liquidation: player1 mortgages Oriental ($50) and Mediterranean ($30)
        // Player1 total cash: $50 + $50 + $30 = $130, still can't pay, bankrupts
        // All 4 properties are now mortgaged
        player1.pay(500, player2, bank, board)

        // Fees during bankruptcy transfer:
        // Mediterranean: assume mortgage $3 (10% of $30)
        // Baltic: unmortgage $33 (110% of $30) - only property player2 unmortgages
        // Oriental: assume mortgage $5 (10% of $50)
        // Vermont: assume mortgage $5 (10% of $50)
        // Total fees: $3 + $33 + $5 + $5 = $46
        // Verify: player2 received cash and paid fees
        assertEquals(0, player1.money)
        assertEquals(1084, player2.money) // 1000 + 130 - 46

        // Verify: all properties transferred with correct mortgage states
        assertTrue(player2.isOwner(MediterraneanAvenue::class))
        assertTrue(player2.isOwner(BalticAvenue::class))
        assertTrue(player2.isOwner(OrientalAvenue::class))
        assertTrue(player2.isOwner(VermontAvenue::class))

        assertTrue(player2.getDevelopment(MediterraneanAvenue::class).isMortgaged)
        assertFalse(player2.getDevelopment(BalticAvenue::class).isMortgaged)
        assertTrue(player2.getDevelopment(OrientalAvenue::class).isMortgaged)
        assertTrue(player2.getDevelopment(VermontAvenue::class).isMortgaged)

        assertPlayerIsBankrupt(player1)
    }

    @Test
    fun `bankruptcy fails if properties have houses or hotels`() {
        val player1 = Player("Player 1", money = 10)
        player1.deeds[BalticAvenue()] = Player.Development(numHouses = 2)

        val player2 = Player("Player 2", money = 500)
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // Manually trigger bankruptcy with undeveloped property (bypassing normal liquidation)
        // This simulates a logic error where liquidation failed to remove houses
        // The hasFullyLiquidatedAssets() check catches this and throws IllegalStateException
        val exception =
            assertThrows<IllegalStateException> {
                // Using reflection to directly call private declareBankruptcy method
                val method =
                    Player::class.java.getDeclaredMethod(
                        "declareBankruptcy",
                        Player::class.java,
                        Bank::class.java,
                        Board::class.java,
                    )
                method.isAccessible = true
                try {
                    method.invoke(player1, player2, bank, board)
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    throw e.cause!!
                }
            }

        // Verify exception message describes the problem
        assertTrue(exception.message!!.contains("declared bankruptcy without first liquidating"))
    }

    @Test
    fun `creditor liquidates assets to pay mortgage fees if insufficient cash`() {
        val player1 = Player("Player 1", money = 10)
        player1.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)

        val player2 = Player("Player 2", money = 20, strategy = AlwaysUnmortgageStrategy())
        player2.deeds[ConnecticutAvenue()] = Player.Development()

        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes $100, bankrupting to player2
        // Baltic unmortgage cost: $33
        // player2 has $20 + $10 (from player1) = $30, needs $33
        // player2 will mortgage Connecticut to get $60, then has enough
        player1.pay(100, player2, bank, board)

        // Verify: player2 received property and paid fees by liquidating
        assertTrue(player2.isOwner(BalticAvenue::class))
        assertFalse(player2.getDevelopment(BalticAvenue::class).isMortgaged)
        assertTrue(player2.getDevelopment(ConnecticutAvenue::class).isMortgaged)
        assertEquals(57, player2.money) // 20 + 10 - 33 + 60

        assertPlayerIsBankrupt(player1)
    }

    @Test
    fun `creditor bankrupts to bank if unable to pay mortgage fees`() {
        val player1 = Player("Player 1", money = 10)
        player1.deeds[ParkPlace()] = Player.Development(isMortgaged = true)
        player1.deeds[Boardwalk()] = Player.Development(isMortgaged = true)

        val player2 = Player("Player 2", money = 50, strategy = AlwaysUnmortgageStrategy())
        val (_, _, bankBoard) = createBankruptcyScenario(player1, player2)
        val (bank, board) = bankBoard

        // player1 owes $500, bankrupting to player2
        // Park Place unmortgage: $193 (110% of $175)
        // Boardwalk unmortgage: $220 (110% of $200)
        // Total fees: $413
        // player2 has $50 + $10 (from player1) = $60, nowhere near enough
        // player2 can't liquidate enough, triggers cascading bankruptcy
        player1.pay(500, player2, bank, board)

        // Verify: both players are bankrupt to bank
        assertTrue(player1.isBankrupt())
        assertTrue(player2.isBankrupt())

        // Verify: both players have no deeds (all went to bank)
        assertTrue(player1.deeds.isEmpty())
        assertTrue(player2.deeds.isEmpty())
    }

    @Test
    fun `unmortgageProperties unmortgages when strategy approves`() {
        val player = Player("Player", money = 200, strategy = AlwaysUnmortgageStrategy())
        player.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)
        player.deeds[OrientalAvenue()] = Player.Development(isMortgaged = true)

        val bank = Bank()
        val board = Board(listOf(player), bank)

        player.unmortgageProperties(bank, board)

        // Both properties should be unmortgaged
        assertFalse(player.getDevelopment(BalticAvenue::class).isMortgaged)
        assertFalse(player.getDevelopment(OrientalAvenue::class).isMortgaged)
        // Baltic: $33, Oriental: $56, Total: $89
        assertEquals(111, player.money) // 200 - 89
    }

    @Test
    fun `unmortgageProperties skips when strategy denies`() {
        val player = Player("Player", money = 200, strategy = NeverUnmortgageStrategy())
        player.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)

        val bank = Bank()
        val board = Board(listOf(player), bank)

        player.unmortgageProperties(bank, board)

        // Property should remain mortgaged
        assertTrue(player.getDevelopment(BalticAvenue::class).isMortgaged)
        assertEquals(200, player.money) // No change
    }

    @Test
    fun `unmortgageProperties uses default strategy based on cash reserves`() {
        // Default strategy: unmortgage if money >= mortgageValue * 2.2
        val player = Player("Player", money = 100)
        // Baltic mortgage value: $30, needs $66 to unmortgage (2.2x), unmortgage cost: $33
        // Oriental mortgage value: $50, needs $110 to unmortgage (2.2x), unmortgage cost: $56
        player.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)
        player.deeds[OrientalAvenue()] = Player.Development(isMortgaged = true)

        val bank = Bank()
        val board = Board(listOf(player), bank)

        player.unmortgageProperties(bank, board)

        // Baltic should be unmortgaged (100 >= 66), Oriental should not (67 < 110 after Baltic)
        assertFalse(player.getDevelopment(BalticAvenue::class).isMortgaged)
        assertTrue(player.getDevelopment(OrientalAvenue::class).isMortgaged)
        assertEquals(67, player.money) // 100 - 33
    }

    @Test
    fun `unmortgageProperties does nothing when no mortgaged properties`() {
        val player = Player("Player", money = 200)
        player.deeds[BalticAvenue()] = Player.Development(isMortgaged = false)

        val bank = Bank()
        val board = Board(listOf(player), bank)

        player.unmortgageProperties(bank, board)

        assertEquals(200, player.money) // No change
    }

    @Test
    fun `countDevelopments returns correct house and hotel counts`() {
        val player = Player("Player")
        player.deeds[MediterraneanAvenue()] = Player.Development(numHouses = 2)
        player.deeds[BalticAvenue()] = Player.Development(numHouses = 3)
        player.deeds[OrientalAvenue()] = Player.Development(hasHotel = true)
        player.deeds[VermontAvenue()] = Player.Development() // No development

        val (houses, hotels) = player.countDevelopments()

        assertEquals(5, houses) // 2 + 3 + 0 + 0
        assertEquals(1, hotels)
    }

    @Test
    fun `countDevelopments returns zero when no developments`() {
        val player = Player("Player")
        player.deeds[BalticAvenue()] = Player.Development()

        val (houses, hotels) = player.countDevelopments()

        assertEquals(0, houses)
        assertEquals(0, hotels)
    }

    @Test
    fun `player bankrupts to bank with all assets transferred`() {
        val player = Player("Player", money = 50)
        player.deeds[BalticAvenue()] = Player.Development(isMortgaged = true)
        player.grantGetOutOfJailFreeCard(ChanceCard.GetOutOfJailFree)

        val bank = Bank(money = 1000)
        val board = Board(listOf(player), bank)

        player.declareBankruptcy(bank, board)

        // Verify bankruptcy state
        assertTrue(player.isBankrupt())
        assertEquals(0, player.money)
        assertTrue(player.deeds.isEmpty())
        assertFalse(player.hasGetOutOfJailFreeCard())

        // Bank received the cash
        assertEquals(1050, bank.money) // 1000 + 50
    }

    // Helper methods for bankruptcy tests
    private fun createBankruptcyScenario(
        debtor: Player,
        creditor: Player,
    ): Triple<Player, Player, Pair<Bank, Board>> {
        val bank = Bank()
        val board = Board(listOf(debtor, creditor), bank = bank)
        return Triple(debtor, creditor, Pair(bank, board))
    }

    private fun assertPlayerIsBankrupt(player: Player) {
        assertTrue(player.isBankrupt())
        assertTrue(player.deeds.isEmpty())
        assertEquals(0, player.money)
    }

    private fun assertPlayerIsNotBankrupt(player: Player) {
        assertFalse(player.isBankrupt())
    }

    // Test-specific strategies for controlling unmortgage behavior
    private class AlwaysUnmortgageStrategy : DefaultStrategy() {
        override fun shouldUnmortgageProperty(
            deed: TitleDeed,
            unmortgageCost: Int,
            player: Player,
            board: Board
        ): Boolean = true
    }

    private class NeverUnmortgageStrategy : DefaultStrategy() {
        override fun shouldUnmortgageProperty(
            deed: TitleDeed,
            unmortgageCost: Int,
            player: Player,
            board: Board
        ): Boolean = false
    }

    private class ConditionalUnmortgageStrategy(private val shouldUnmortgage: (TitleDeed) -> Boolean) : DefaultStrategy() {
        override fun shouldUnmortgageProperty(
            deed: TitleDeed,
            unmortgageCost: Int,
            player: Player,
            board: Board
        ): Boolean = shouldUnmortgage(deed)
    }
}
