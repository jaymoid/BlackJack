package uk.co.pittendreigh.blackjack

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.co.pittendreigh.blackjack.Rank.*
import uk.co.pittendreigh.blackjack.Suit.*
import uk.co.pittendreigh.blackjack.GameFinish.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.random.Random
import java.io.ByteArrayInputStream
import java.lang.System.lineSeparator

private val dummyDealerHand = hand(JACK of SPADES, SEVEN of CLUBS)
private val dummyPlayerHand = hand(ACE of HEARTS, EIGHT of DIAMONDS)
private val playerAndDealerHands = dummyDealerHand + dummyPlayerHand
private val dummyDeck = createDeckOfCards()
    .filterNot { playerAndDealerHands.contains(it) }
    .shuffled(Random(123))

val playerHaslessThan21 = PlayerHas21OrLower(createCardState(), setOf(8, 18))

private fun createCardState(
    playerHand: List<Card> = dummyPlayerHand,
    dealerHand: List<Card> = dummyDealerHand,
    deck: List<Card> = dummyDeck
) =
    CardsState(playerHand, dealerHand, deck)

private fun typeInput(vararg inputCommands: String) =
    inputCommands.joinToString(separator = lineSeparator())


class TerminalBlackJackInterfaceTest {

    private val outputStream = ByteArrayOutputStream()
    private val printStream = PrintStream(outputStream, true)
    private val output: PrintStream = printStream
    private val blackJackGame: BlackJackGame = mockk()
    private val stickToPreventInfiniteRecursion = "stick" + lineSeparator()

    private fun startGame(preparedInput: String = stickToPreventInfiniteRecursion) =
        TerminalBlackJackInterface(blackJackGame, output, ByteArrayInputStream(preparedInput.toByteArray()))
            .startGame()

    @BeforeEach
    private fun stubGameOverToPreventInfiniteRecursion() {
        every { blackJackGame.stick(any()) } returns GameOver(createCardState(), PlayerWins)
    }

    @BeforeEach
    private fun stubDealToReturnLessThan21() {
        every { blackJackGame.deal() } returns playerHaslessThan21
    }

    @Nested
    inner class `Deal and print cards` {
        @Test
        fun `When the apps starts, then deal the cards and print the dealt hands`() {
            startGame()

            val output = outputStream.toString().lines()
            assertEquals("Welcome to One Eyed Jack's...", output[0])
            assertEquals("Dealer Cards: [? ?] [7 ♧]", output[1])
            assertEquals("Your Cards:   [A ♡] [8 ♢]", output[2])
            assertEquals("Your hand is worth: 8 or 18", output[3])
        }
    }

    @Nested
    inner class `Stick or Twist prompt and input validation` {

        @Test
        fun `Given player gets less than 21, when dealt, then print hand value and ask Stick or Twist?`() {
            startGame()

            val outputLines = outputStream.toString().lines()
            assertEquals("[S]tick or [T]wist?:", outputLines[4])
        }

        @ParameterizedTest
        @ValueSource(strings = ["S", "stick", "s", "StIcK"])
        fun `Given player types "stick", when prompted, then invoke stick on the game`(stickStr: String) {
            every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), PlayerWins)

            startGame(stickStr + lineSeparator())

            verify { blackJackGame.stick(playerHaslessThan21) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["twist", "t", "T", "TwIsT"])
        fun `Given player types "twist", when prompted, then invoke twist on the game`(twistStr: String) {
            every { blackJackGame.twist(playerHaslessThan21) } returns GameOver(createCardState(), PlayerWins)

            startGame(twistStr + lineSeparator())

            verify { blackJackGame.twist(playerHaslessThan21) }
        }


        @ParameterizedTest
        @ValueSource(strings = ["not", "stik", "OR", "TwIzzed", "", " "])
        fun `Given player cannot type stick or twist, when prompted, then ask to stick or twist again`(fatFingers: String) {

            startGame(fatFingers + lineSeparator() + stickToPreventInfiniteRecursion)

            val outputLines = outputStream.toString().lines()
            assertEquals("[S]tick or [T]wist?:", outputLines[4])
            assertEquals("[S]tick or [T]wist?:", outputLines[5])
        }
    }

    @Nested
    inner class `Game will loop when player twists n times` {
        @Test
        fun `Given the player has less than 21, when prompted stick or twist, then enter twist 4 times in a row`() {
            every {
                blackJackGame.twist(playerHaslessThan21)
            } returnsMany listOf(
                playerHaslessThan21,
                playerHaslessThan21,
                playerHaslessThan21,
                GameOver(createCardState(), GameFinish.PlayerWins)
            )

            val preparedInput = (1..4).map { "twist" }.joinToString(separator = lineSeparator())

            startGame(preparedInput)

            val timesAskedStickOrTwist = outputStream.toString().lines().filter { it == "[S]tick or [T]wist?:" }.size
            assertThat(timesAskedStickOrTwist).isEqualTo(4)
        }
    }

    @Nested
    inner class `Player or Dealer get Blackjack when dealt` {
        @Test
        fun `Given player will get blackjack, when dealt, then print player wins with blackjack`() {
            every { blackJackGame.deal() } returns GameOver(createCardState(), PlayerIsBlackJack)

            startGame()

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("You win with blackjack!")
        }

        @Test
        fun `Given player and dealer get blackjack, when dealt, then print draw`() {
            every { blackJackGame.deal() } returns GameOver(createCardState(), PlayerAndDealerBlackJack)

            startGame()

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("DRAW! You and the dealer got blackjack.")
        }

        @Test
        fun `Given dealer gets blackjack, when dealt, then print dealer wins with blackjack`() {
            // Dealer blackjack is revealed when the player plays one round (stick/twist)
            every { blackJackGame.deal() } returns playerHaslessThan21
            every { blackJackGame.twist(playerHaslessThan21) } returns playerHas21With3CardsDealerHasBlackJack
            every {
                blackJackGame.stick(playerHas21With3CardsDealerHasBlackJack)
            } returns GameOver(playerHas21With3CardsDealerHasBlackJack.state, DealerIsBlackJack)

            startGame(typeInput("twist", "stick"))

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("The dealer wins with blackjack.")
        }

        private val playerHas21With3CardsDealerHasBlackJack = PlayerHas21OrLower(
            createCardState(
                playerHand = hand(ACE of HEARTS, EIGHT of CLUBS, THREE of CLUBS),
                dealerHand = hand(ACE of CLUBS, KING of SPADES),
                deck = listOf()
            ),
            setOf(12, 21)
        )
    }

    @Nested
    inner class `Non Blackjack game Over scenarios` {

        @Test
        fun `Given player will win after one twist, when dealt, then print new cards and player wins`() {
            val winning21Hand = hand(ACE of SPADES, TWO of HEARTS, EIGHT of CLUBS)
            val playerTwistsToGet21 = PlayerHas21OrLower(createCardState(playerHand = winning21Hand), setOf(11, 21))
            val playerWinsWith21 = GameOver(createCardState(playerHand = winning21Hand), PlayerWins)
            every { blackJackGame.deal() } returns playerHaslessThan21
            every { blackJackGame.twist(playerHaslessThan21) } returns playerTwistsToGet21
            every { blackJackGame.stick(playerTwistsToGet21) } returns playerWinsWith21

            startGame(typeInput("twist", "stick"))

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("You win!")
        }

        @Test
        fun `Given the player will go bust, when they twist, then print player went bust`() {
            every { blackJackGame.twist(playerHaslessThan21) } returns GameOver(createCardState(), PlayerIsBust)

            startGame(typeInput("twist"))

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("You went bust!")
        }

        @Test
        fun `Given the dealer will go bust, when the player sticks, then print dealer is bust`() {
            every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), DealerIsBust)

            startGame(typeInput("stick"))

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("The dealer went bust!")
        }

        @Test
        fun `Given the dealer will win, when the player sticks, then print dealer wins`() {
            every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), DealerWins)

            startGame(typeInput("stick"))

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("The dealer wins!")
        }

        @Test
        fun `Given the game will be a draw, when the player sticks, then print draw game`() {
            every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), DrawGame)

            startGame(typeInput("stick"))

            val outputLines = outputStream.toString().lines().filterNot { it == ""}
            assertThat(outputLines.last()).contains("It's a draw!")
        }
    }

    @ParameterizedTest
    @EnumSource(GameFinish::class)
    fun `Given the game will finish, when it does, then full cards are printed at the end of the game`(gameFinish: GameFinish) {
        every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), gameFinish)

        startGame(typeInput("stick"))

        val outputLines = outputStream.toString().lines()
        assertEquals("Dealer Cards: [J ♤] [7 ♧]", outputLines[outputLines.size - 4])
        assertEquals("Your Cards:   [A ♡] [8 ♢]", outputLines[outputLines.size - 3])
    }

    @ParameterizedTest
    @EnumSource(GameFinish::class)
    fun `Given the game will finish, when it does, then prompt the user if they would like to play again`(gameFinish: GameFinish) {
        every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), gameFinish)

        startGame(typeInput("stick"))

        val outputLines = outputStream.toString().lines().filter { it != ""}
        assertThat(outputLines.last()).contains("... Would you like to play again? [y/n]:")
    }

    @ParameterizedTest
    @EnumSource(GameFinish::class)
    fun `Given the user wants to play again, when the game ends, then new cards are dealt`(gameFinish: GameFinish) {
        val nextHand = PlayerHas21OrLower(createCardState(
            playerHand = hand(QUEEN of SPADES, SEVEN of CLUBS),
            dealerHand = hand(JACK of DIAMONDS, TWO of HEARTS )
        ), setOf(17))
        every { blackJackGame.deal() } returnsMany listOf(playerHaslessThan21, nextHand)
        every { blackJackGame.stick(playerHaslessThan21) } returns GameOver(createCardState(), gameFinish)

        startGame(typeInput("stick", "y", "stick", "no"))

        val outputLines = outputStream.toString().lines()
        assertThat(outputLines).contains("Dealer Cards: [? ?] [2 ♡]")
        assertThat(outputLines).contains("Your Cards:   [Q ♤] [7 ♧]")
        assertThat(outputLines).contains("Your hand is worth: 17")
    }

}
