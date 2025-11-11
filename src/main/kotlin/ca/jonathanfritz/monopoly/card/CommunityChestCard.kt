package ca.jonathanfritz.monopoly.card

import ca.jonathanfritz.monopoly.Player
import ca.jonathanfritz.monopoly.board.Bank
import ca.jonathanfritz.monopoly.board.Board
import ca.jonathanfritz.monopoly.exception.InsufficientTokenException

sealed class CommunityChestCard: Card() {

    // Bank error in your favor. Collect $200.
    object BankErrorInYourFavour: BankPaysYou(200, "in error")

    // Doctor's fees. {fee} Pay $50
    object DoctorsFees: YouPayBank(50, "in doctor's fees")

    // From sale of stock you get $50
    object SaleOfStock: BankPaysYou(50, "from sale of stock")

    // Get out of Jail Free - This card may be kept until needed or sold/traded
    object GetOutOfJailFree: GetOutOfJailFreeCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            if (board.communityChest.remove(this)) {
                println("\t\t${player.name} added a Get out of Jail Free card to their inventory")
                player.grantGetOutOfJailFreeCard(this)
            } else {
                throw InsufficientTokenException("Failed to grant ${player.name} Get Out of Jail Free card: Card is not present in Chance deck")
            }
        }
    }

    // Grand Opera Opening. Collect $50 from every player for opening night seats.
    object GrandOperaOpening: CommunityChestCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.players.filter { it != player }.forEach { other ->
                other.pay(50, player, bank, board, "for opera tickets on opening night")
            }
        }
    }

    // Holiday Fund matures. Receive $100
    object HolidayFundMatures: BankPaysYou(100, "for maturation of holiday fund")

    // Income tax refund. Collect $20
    object IncomeTaxRefund: BankPaysYou(20, "income tax refund")

    // It is your birthday. Collect $10 from every player
    object YourBirthday: CommunityChestCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            board.players.filter { it != player }.forEach { other ->
                other.pay(10, player, bank, board, "as a birthday gift")
            }
        }
    }

    // Life insurance matures – Collect $100
    object LifeInsurance: BankPaysYou(100, "for maturation of life insurance")

    // Hospital Fees. Pay $50
    object HospitalFees: YouPayBank(50, "hospital fees")

    // School fees. Pay $50
    object SchoolFees: YouPayBank(50, "school fees")

    // Receive $25 consultancy fee
    object ConsultancyFees: BankPaysYou(25, "consultancy fees")

    // You are assessed for street repairs: Pay $40 per house and $115 per hotel you own
    object StreetRepairs: CommunityChestCard() {
        override fun onDraw(player: Player, bank: Bank, board: Board) {
            val (houses, hotels) = player.countDevelopments()
            val fee = houses * 40 + hotels * 115
            bank.charge(fee, player, board, "for street repairs on $houses houses and $hotels hotels")
        }
    }

    // You have won second prize in a beauty contest. Collect $10
    object BeautyContest: BankPaysYou(10, "for placing 2nd in a beauty contest")

    // You inherit $100
    object Inheritance: BankPaysYou(100, "inheritance")
}
