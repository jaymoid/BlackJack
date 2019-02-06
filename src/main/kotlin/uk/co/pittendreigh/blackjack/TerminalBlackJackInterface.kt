package uk.co.pittendreigh.blackjack

import java.io.InputStream
import java.io.PrintStream
import uk.co.pittendreigh.blackjack.GameFinish.*
import uk.co.pittendreigh.blackjack.Rank.*
import uk.co.pittendreigh.blackjack.Suit.*
import uk.co.pittendreigh.blackjack.TerminalBlackJackInterface.StickOrTwist.*

class TerminalBlackJackInterface(
    val blackJackGame: BlackJackGame,
    val output: PrintStream,
    input: InputStream
) {

    val bufferedInput = input.bufferedReader()

    fun startGame() {
        output.println("Welcome to One Eyed Jack's...")
        playBlackJack(blackJackGame.deal())
    }

    private tailrec fun playBlackJack(game: BlackJackGameState): Unit =
        when (game) {
            is GameOver -> printGameOverDetails(game)
            is PlayerHas21OrLower -> playBlackJack(stickOrTwist(game))
        }

    private fun stickOrTwist(game: PlayerHas21OrLower): BlackJackGameState {
        printCards(game)
        printHandScore(game)
        return when (promptStickOrTwist()) {
            STICK -> blackJackGame.stick(game)
            TWIST -> blackJackGame.twist(game)
        }
    }

    private fun printCards(game: BlackJackGameState, revealDealerCard: Boolean = false) {
        output.println(
            "Dealer Cards:" +
                    if (revealDealerCard) printHand(game.state.dealerHand)
                    else printHand(game.state.dealerHand, " [? ?]", 1)
        )
        output.println("Your Cards:  " + printHand(game.state.playerHand))
    }

    private fun printHand(hand: List<Card>, initialString: String = "", drop: Int = 0) =
        hand.drop(drop).fold(initialString) { acc, card -> acc + " " + card.prettyPrint() }

    private fun printHandScore(game: PlayerHas21OrLower) =
        output.println("Your hand is worth: " +
                game.possibleScores.sorted().map(Int::toString).reduce { a, b -> if (a != "") "$a or $b" else b }
        )

    private enum class StickOrTwist { STICK, TWIST }

    private tailrec fun promptStickOrTwist(): StickOrTwist {
        output.println("[S]tick or [T]wist?:")
        val answer = bufferedInput.readLine()?.toLowerCase()
        return when {
            (answer == "stick" || answer == "s") -> STICK
            (answer == "twist" || answer == "t") -> TWIST
            else -> promptStickOrTwist()
        }
    }

    private fun printGameOverDetails(game: GameOver) {
        printCards(game, true)
        output.print(getGameOverResultString(game))
        if (promptPlayAgain()) playBlackJack(blackJackGame.deal())
    }

    private fun getGameOverResultString(game: GameOver): String =
        when (game.result) {
            PlayerIsBlackJack -> "You win with blackjack!"
            DealerIsBlackJack -> "The dealer wins with blackjack."
            PlayerAndDealerBlackJack -> "DRAW! You and the dealer got blackjack."
            PlayerWins -> "You win!"
            DealerWins -> "The dealer wins!"
            PlayerIsBust -> "You went bust!"
            DealerIsBust -> "The dealer went bust!"
            DrawGame -> "It's a draw!"
        }

    private fun promptPlayAgain(): Boolean {
        output.println("... Would you like to play again? [y/n]:")
        val answer = bufferedInput.readLine()?.toLowerCase()
        return (answer == "yes" || answer == "y")
    }
}

private fun Card.prettyPrint(): String = "[${rank.shortName()} ${suit.symbol()}]"

private fun Suit.symbol() =
    when (this) {
        CLUBS -> "♧"
        HEARTS -> "♡"
        SPADES -> "♤"
        DIAMONDS -> "♢"
    }

private fun Rank.shortName() =
    when (this) {
        ACE -> "A"
        TWO -> "2"
        THREE -> "3"
        FOUR -> "4"
        FIVE -> "5"
        SIX -> "6"
        SEVEN -> "7"
        EIGHT -> "8"
        NINE -> "9"
        TEN -> "10"
        JACK -> "J"
        QUEEN -> "Q"
        KING -> "K"
    }
