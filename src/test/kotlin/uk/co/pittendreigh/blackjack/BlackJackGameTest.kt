package uk.co.pittendreigh.blackjack

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll
import uk.co.pittendreigh.blackjack.GameFinish.*
import uk.co.pittendreigh.blackjack.Rank.*
import uk.co.pittendreigh.blackjack.Suit.*

private val cardProviderFunction: () -> List<Card> = mockk()
private val cardShufflerFunction: (List<Card>) -> List<Card> = { it }

private val game = BlackJackGame(cardProviderFunction, cardShufflerFunction)

class `BlackJackGame - Deal functionality` {

    @Nested
    inner class `given some cards, when the starting deal is performed` {

        private val initialCards = listOf(
            ACE of HEARTS,
            TWO of HEARTS,
            ACE of CLUBS,
            TWO of CLUBS,
            JACK of DIAMONDS
        )

        @BeforeEach
        fun stubInitialCards() {
            every { cardProviderFunction() } returns initialCards
        }

        @Test
        fun `then the cards from the provider are shuffled`() {
            val cardShufflerFunction: (List<Card>) -> List<Card> = { cards -> cards.reversed() }

            val shuffledGame = BlackJackGame(cardProviderFunction, cardShufflerFunction)
            val gameState = shuffledGame.deal()

            assertAll(
                { assertEquals(listOf(ACE of HEARTS), gameState.state.deck) },
                { assertEquals(listOf(JACK of DIAMONDS, ACE of CLUBS), gameState.state.playerHand) },
                { assertEquals(listOf(TWO of CLUBS, TWO of HEARTS), gameState.state.dealerHand) }
            )
        }

        @Test
        fun `then the player has the first card, and the third card`() {
            val gameState = game.deal()
            assertThat(gameState.state.playerHand).containsOnly(ACE of HEARTS, ACE of CLUBS)
        }

        @Test
        fun `then the dealer has the second and fourth card`() {
            val gameState = game.deal()
            assertThat(gameState.state.dealerHand).containsOnly(TWO of HEARTS, TWO of CLUBS)
        }

        @Test
        fun `then the dealer and player cards are no longer in the deck`() {
            val gameState = game.deal()
            assertThat(gameState.state.deck).containsOnly(JACK of DIAMONDS)
        }
    }

    @Nested
    inner class `given the player has a hand lower than blackjack, when starting deal is performed` {

        val tooLowHands = listOf(
            hand(ACE of HEARTS, TWO of DIAMONDS) to setOf(3, 13),
            hand(THREE of CLUBS, FOUR of DIAMONDS) to setOf(7),
            hand(FIVE of CLUBS, SIX of DIAMONDS) to setOf(11),
            hand(SEVEN of HEARTS, EIGHT of DIAMONDS) to setOf(15),
            hand(NINE of CLUBS, TEN of DIAMONDS) to setOf(19),
            hand(JACK of SPADES, QUEEN of HEARTS) to setOf(20),
            hand(KING of CLUBS, KING of DIAMONDS) to setOf(20)
        )

        @TestFactory
        fun `then the returned game state is PlayerHas21OrLower`(): List<DynamicTest> =
            tooLowHands.map { (hand: List<Card>, expectedScores: Set<Int>) ->
                dynamicTest("Starting hand of $hand is reported as 21 or lower, with $expectedScores points") {

                    every { cardProviderFunction() } returns createPreparedDeckOf(playerHand = hand)
                    val gameState = game.deal()

                    assertAll(
                        { assertTrue(gameState is PlayerHas21OrLower) },
                        { assertEquals(expectedScores, (gameState as PlayerHas21OrLower).possibleScores) }
                    )
                }
            }
    }

    fun createPreparedDeckOf(
        playerHand: List<Card>,
        dealerHand: List<Card> = listOf(TWO of SPADES, TWO of CLUBS),
        remainingDeckCards: List<Card> = listOf(TEN of HEARTS)
    ): List<Card> =
        playerHand.zip(dealerHand)
            .flatMap { (playerCard, dealerCard) -> listOf(playerCard, dealerCard) }
            .plus(remainingDeckCards)

    @Nested
    inner class `given the player has blackjack hand but dealer does not, when starting deal is performed` {

        val blackJackHands = listOf(
            hand(ACE of HEARTS, TEN of DIAMONDS),
            hand(JACK of CLUBS, ACE of DIAMONDS),
            hand(QUEEN of SPADES, ACE of SPADES),
            hand(KING of HEARTS, ACE of HEARTS)
        )

        @TestFactory
        fun `then the returned game state is PlayerIsBlackJack`(): List<DynamicTest> =
            blackJackHands.map { blackJackHand ->
                dynamicTest("Starting hand of $blackJackHand is reported as PlayerIsBlackJack") {

                    every { cardProviderFunction() } returns createPreparedDeckOf(
                        playerHand = blackJackHand,
                        dealerHand = listOf(TWO of SPADES, TWO of CLUBS)
                    )
                    val gameState = game.deal()

                    assertAll(
                        { assertTrue(gameState is GameOver) },
                        { assertTrue((gameState as GameOver).result == PlayerIsBlackJack) }
                    )
                }
            }
    }

    @Nested
    inner class `given player & dealer have blackjack, when starting deal is performed` {

        val playerBlackJackHands = listOf(
            hand(ACE of HEARTS, TEN of DIAMONDS),
            hand(JACK of CLUBS, ACE of DIAMONDS),
            hand(QUEEN of SPADES, ACE of SPADES),
            hand(KING of HEARTS, ACE of HEARTS)
        )

        val dealerBlackJackHands = listOf(
            hand(KING of HEARTS, ACE of HEARTS),
            hand(QUEEN of SPADES, ACE of SPADES),
            hand(JACK of CLUBS, ACE of DIAMONDS),
            hand(ACE of HEARTS, TEN of DIAMONDS)
        )

        @TestFactory
        fun `then the returned game state is BothBlackJack`(): List<DynamicTest> =

            playerBlackJackHands.zip(dealerBlackJackHands)
                .map { (playerBlackJackHand, dealerBlackJackHand) ->
                    dynamicTest("player hand of $playerBlackJackHand and dealer hand of $dealerBlackJackHand is PlayerAndDealerBlackJack") {

                        every {
                            cardProviderFunction()
                        } returns createPreparedDeckOf(playerBlackJackHand, dealerBlackJackHand)

                        val gameState = game.deal()

                        assertEquals(PlayerAndDealerBlackJack, (gameState as GameOver).result)
                    }
                }
    }
}

class `BlackJackGame - Twist functionality` {

    @Nested
    inner class `given the player has less than 21, when the player twists` {

        private val playerHand = hand(TWO of HEARTS, THREE of DIAMONDS)
        private val deck = listOf(ACE of HEARTS, JACK of CLUBS)

        private val gameState = PlayerHas21OrLower(
            state = CardsState(
                playerHand = playerHand,
                dealerHand = hand(THREE of CLUBS, TWO of SPADES),
                deck = deck
            ),
            possibleScores = setOf()
        )

        val expectedNewPlayerCard = deck.first()

        @Test
        internal fun `then deal the player another card`() {
            val stateAfterTwist = game.twist(gameState)

            assertEquals(playerHand + expectedNewPlayerCard, stateAfterTwist.state.playerHand)
        }

        @Test
        internal fun `then the deck reduces by one card`() {
            val stateAfterTwist = game.twist(gameState)

            assertEquals(deck - expectedNewPlayerCard, stateAfterTwist.state.deck)
        }
    }

    @Nested
    inner class `given the player will be bust, when the player twists` {
        val playerHandTooLowAndNextCard = listOf(
            hand(TEN of HEARTS, TEN of DIAMONDS) to (NINE of SPADES),
            hand(EIGHT of CLUBS, EIGHT of DIAMONDS) to (EIGHT of DIAMONDS),
            hand(KING of SPADES, NINE of SPADES) to (THREE of HEARTS)
        )

        @TestFactory
        fun `then the returned game state is PlayerIsBust`(): List<DynamicTest> =
            playerHandTooLowAndNextCard.map { (hand, nextCard) ->
                dynamicTest("Hand of $hand + twist $nextCard is reported as PlayerIsBust") {

                    val priorGameState = PlayerHas21OrLower(
                        state = CardsState(
                            playerHand = hand,
                            dealerHand = hand(ACE of CLUBS, NINE of SPADES),
                            deck = listOf(nextCard)
                        ),
                        possibleScores = setOf()
                    )

                    val stateAfterTwist = game.twist(priorGameState)

                    assertEquals(PlayerIsBust, (stateAfterTwist as GameOver).result)
                }
            }
    }

    @Nested
    inner class `given the player will not be bust, when the player twists` {

        val currentHandNextCardAndExpectedVal = listOf(
            Triple(hand(TEN of HEARTS, FOUR of DIAMONDS), (TWO of SPADES), 16),
            Triple(hand(EIGHT of CLUBS, FIVE of DIAMONDS), (TWO of DIAMONDS), 15),
            Triple(hand(KING of SPADES, QUEEN of SPADES), (ACE of HEARTS), 21)
        )

        @TestFactory
        fun `then the returned game state is PlayerIsBust`(): List<DynamicTest> =
            currentHandNextCardAndExpectedVal.map { (hand, nextCard, expectedHandValue) ->
                dynamicTest("Hand of $hand + twist: $nextCard is reported as PlayerHas21OrLower") {

                    val priorGameState = PlayerHas21OrLower(
                        state = CardsState(
                            playerHand = hand,
                            dealerHand = hand(ACE of CLUBS, NINE of SPADES),
                            deck = listOf(nextCard)
                        ),
                        possibleScores = setOf()
                    )

                    val stateAfterTwist = game.twist(priorGameState)

                    assertAll(
                        { assertTrue(stateAfterTwist is PlayerHas21OrLower) },
                        {
                            assertEquals(
                                setOf(expectedHandValue),
                                (stateAfterTwist as PlayerHas21OrLower).possibleScores
                            )
                        }
                    )
                }
            }
    }
}

class `BlackJackGame - Stick functionality` {

    @Nested
    inner class `Given dealerHand is blackjack and the player has 21 with 3 cards, when the dealer wins with blackjack` {

        @Test
        fun `then the Dealer Wins`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(ACE of HEARTS, EIGHT of CLUBS, THREE of CLUBS),
                    dealerHand = hand(ACE of CLUBS, KING of SPADES),
                    deck = listOf()
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)

            assertEquals(DealerIsBlackJack, stateAfterStick.result)
        }
    }

    @Nested
    inner class `Given dealerHand greater than playerHand, when the player sticks` {

        @Test
        fun `then the Dealer Wins`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(ACE of HEARTS, EIGHT of CLUBS),
                    dealerHand = hand(ACE of CLUBS, NINE of SPADES),
                    deck = listOf()
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)

            assertEquals(DealerWins, (stateAfterStick as GameOver).result)
        }
    }

    @Nested
    inner class `Given dealerHand == playerHand and dealerHand is 17 or over, when the player sticks` {

        @Test
        fun `then the game is a draw`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(SEVEN of DIAMONDS, ACE of SPADES),
                    dealerHand = hand(EIGHT of CLUBS, TEN of SPADES),
                    deck = listOf()
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)

            assertEquals(DrawGame, stateAfterStick.result)
        }
    }

    @Nested
    inner class `Given playerHand greater than dealer hand and dealerHand is 17 or over, when the player sticks` {

        @Test
        fun `then the game is a draw`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(EIGHT of DIAMONDS, ACE of SPADES),
                    dealerHand = hand(EIGHT of HEARTS, TEN of HEARTS),
                    deck = listOf()
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)

            assertEquals(PlayerWins, stateAfterStick.result)
        }
    }

    @Nested
    inner class `Given dealerHand is lessThan 17 and playerHand, and deck causes them to go bust, when the player sticks` {

        @Test
        fun `then the dealer takes another card but goes bust (player wins!)`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(EIGHT of DIAMONDS, ACE of SPADES),
                    dealerHand = hand(SIX of HEARTS, TEN of HEARTS),
                    deck = listOf(TEN of DIAMONDS)
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)

            assertAll(
                { assertEquals(DealerIsBust, stateAfterStick.result) },
                { assertThat(stateAfterStick.state.dealerHand).contains(TEN of DIAMONDS) }
            )
        }
    }

    @Nested
    inner class `Given dealerHand is lessThan 17 and playerHand, and deck causes them to win, when the player sticks` {

        @Test
        fun `then the dealer takes another card and wins`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(EIGHT of DIAMONDS, ACE of SPADES),
                    dealerHand = hand(SIX of HEARTS, TEN of HEARTS),
                    deck = listOf(FOUR of DIAMONDS)
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)
            assertAll(
                { assertEquals(DealerIsBust, stateAfterStick.result) },
                { assertThat(stateAfterStick.state.dealerHand).contains(FIVE of DIAMONDS) }
            )
        }
    }

    @Nested
    inner class `Given dealerHand is lessThan 17 and playerHand, and deck causes them to draw, when the player sticks` {

        @Test
        fun `then the dealer takes another card and draws`() {
            val priorGameState = PlayerHas21OrLower(
                state = CardsState(
                    playerHand = hand(EIGHT of DIAMONDS, ACE of SPADES),
                    dealerHand = hand(FIVE of HEARTS, TEN of HEARTS),
                    deck = listOf(FOUR of DIAMONDS)
                ),
                possibleScores = setOf()
            )

            val stateAfterStick = game.stick(priorGameState)

            assertAll(
                { assertEquals(DrawGame, stateAfterStick.result) },
                { assertThat(stateAfterStick.state.dealerHand).contains(FOUR of DIAMONDS) }
            )
        }
    }
}
