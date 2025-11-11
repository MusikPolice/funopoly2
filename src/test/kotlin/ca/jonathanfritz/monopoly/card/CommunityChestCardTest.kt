package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.deed.Property
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CommunityChestCardTest {

    @Test
    fun `bank error in your favour test`() {
        assertBankPaysPlayer(CommunityChestCard.BankErrorInYourFavour, 200)
    }

    @Test
    fun `doctors fees test`() {
        assertPlayerPaysBank(CommunityChestCard.DoctorsFees, 50)
    }

    @Test
    fun `sale of stock test`() {
        assertBankPaysPlayer(CommunityChestCard.SaleOfStock, 50)
    }

    @Test
    fun `get out of jail free card test`() {
        val player = Player("Abbi")
        val bank = Bank()
        val board = Board(listOf(player))

        // grant the player a get out of jail free card
        CommunityChestCard.GetOutOfJailFree.onDraw(player, bank, board)
        assertFalse(board.communityChest.contains(CommunityChestCard.GetOutOfJailFree))

        // if the player is in jail, they will play the newly granted card
        player.isInJail = true
        assertEquals(CommunityChestCard.GetOutOfJailFree, player.useGetOutOfJailFreeCard())
    }

    @Test
    fun `grand opera opening test`() {
        val target = Player("Bert")
        val source1 = Player("Ernie", 50)
        val source2 = Player("Big Bird", 50)
        val board = Board(listOf(target, source1, source2))

        CommunityChestCard.GrandOperaOpening.onDraw(target, Bank(), board)
        assertEquals(100, target.money)
        assertEquals(0, source1.money)
        assertEquals(0, source2.money)
    }

    @Test
    fun `holiday fund test`() {
        assertBankPaysPlayer(CommunityChestCard.HolidayFundMatures, 100)
    }

    @Test
    fun `income tax refund test`() {
        assertBankPaysPlayer(CommunityChestCard.IncomeTaxRefund, 20)
    }

    @Test
    fun `birthday test`() {
        val target = Player("Bert")
        val source1 = Player("Ernie", 10)
        val source2 = Player("Big Bird", 10)
        val board = Board(listOf(target, source1, source2))

        CommunityChestCard.YourBirthday.onDraw(target, Bank(), board)
        assertEquals(20, target.money)
        assertEquals(0, source1.money)
        assertEquals(0, source2.money)
    }

    @Test
    fun `life insurance test`() {
        assertBankPaysPlayer(CommunityChestCard.LifeInsurance, 100)
    }

    @Test
    fun `hospital fees test`() {
        assertPlayerPaysBank(CommunityChestCard.HospitalFees, 50)
    }

    @Test
    fun `school fees test`() {
        assertPlayerPaysBank(CommunityChestCard.SchoolFees, 50)
    }

    @Test
    fun `consultancy fees test`() {
        assertBankPaysPlayer(CommunityChestCard.ConsultancyFees, 25)
    }

    @Test
    fun `street repairs test`() {
        val player = Player("Ernie", 5000)
        val bank = Bank()
        val board = Board(listOf(player))

        // player has partially developed the light blue properties
        bank.sellDeedToPlayer(Property.OrientalAvenue::class, player, board)
        bank.sellDeedToPlayer(Property.VermontAvenue::class, player, board)
        bank.sellDeedToPlayer(Property.ConnecticutAvenue::class, player, board)

        (1 .. 4).forEach { _ ->
            bank.sellHouseToPlayer(Property.OrientalAvenue::class, player, board)
            bank.sellHouseToPlayer(Property.VermontAvenue::class, player, board)
            bank.sellHouseToPlayer(Property.ConnecticutAvenue::class, player, board)
        }
        bank.sellHotelToPlayer(Property.ConnecticutAvenue::class, player, board)

        val playerBalance = player.money

        // 8 houses and a hotel costs the player a total of 435
        CommunityChestCard.StreetRepairs.onDraw(player, bank, board)
        assertEquals(playerBalance - 435, player.money)
    }

    @Test
    fun `beauty contest test`() {
        assertBankPaysPlayer(CommunityChestCard.BeautyContest, 10)
    }

    @Test
    fun `inheritance test`() {
        assertBankPaysPlayer(CommunityChestCard.Inheritance, 100)
    }
}