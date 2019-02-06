package uk.co.pittendreigh.blackjack

import uk.co.pittendreigh.blackjack.Rank.*
import uk.co.pittendreigh.blackjack.GameFinish.*

fun hand(vararg cards: Card): List<Card> = cards.toList()

sealed class BlackJackGameState(open val state: CardsState)
data class PlayerHas21OrLower(override val state: CardsState, val possibleScores: Set<Int>) : BlackJackGameState(state)
data class GameOver(override val state: CardsState, val result: GameFinish) : BlackJackGameState(state)

enum class GameFinish {
    PlayerIsBlackJack,
    DealerIsBlackJack,
    PlayerAndDealerBlackJack,
    PlayerWins,
    DealerWins,
    DealerIsBust,
    PlayerIsBust,
    DrawGame
}

data class CardsState(val playerHand: List<Card>, val dealerHand: List<Card>, val deck: List<Card>)

private sealed class BlackJackResult
private object BlackJack : BlackJackResult()
private object Bust : BlackJackResult()
private data class EqualTo21OrLower(val possibleScores: Set<Int>) : BlackJackResult()

class BlackJackGame(
    val cardProviderFunction: () -> List<Card>,
    val cardShufflerFunction: (List<Card>) -> List<Card>
) {

    fun deal(): BlackJackGameState =
        cardShufflerFunction(cardProviderFunction()).let { deck ->
            getNewGameState(
                CardsState(
                    playerHand = listOf(deck.get(0), deck.get(2)),
                    dealerHand = listOf(deck.get(1), deck.get(3)),
                    deck = deck.drop(4)
                )
            )
        }

    fun twist(gameState: PlayerHas21OrLower): BlackJackGameState =
        getNewGameState(
            CardsState(
                playerHand = gameState.state.playerHand + gameState.state.deck.get(0),
                dealerHand = gameState.state.dealerHand,
                deck = gameState.state.deck.drop(1)
            )
        )

    private fun getNewGameState(cardState: CardsState): BlackJackGameState {
        val playerHandValue = calculateHandValue(cardState.playerHand)
        return when (playerHandValue) {
            is BlackJack ->
                if (calculateHandValue(cardState.dealerHand) == BlackJack)
                    GameOver(cardState, PlayerAndDealerBlackJack)
                else
                    GameOver(cardState, PlayerIsBlackJack)
            is EqualTo21OrLower -> PlayerHas21OrLower(cardState, playerHandValue.possibleScores)
            is Bust -> GameOver(cardState, PlayerIsBust)
        }
    }

    fun stick(game: PlayerHas21OrLower): GameOver = stick(game.state)

    private tailrec fun stick(state: CardsState): GameOver {
        val dealerHandMax: Int = calculatePossibleValues(state.dealerHand).max() ?: 0
        val playerHandMax: Int = calculatePossibleValues(state.playerHand).max() ?: 0

        val dealerHasBlackJack = dealerHandMax == 21 && state.dealerHand.size == 2

        return when {
            dealerHasBlackJack -> GameOver(state, DealerIsBlackJack)
            dealerHandMax == 0 -> GameOver(state, DealerIsBust)
            dealerHandMax > playerHandMax -> GameOver(state, DealerWins)
            dealerHandMax == playerHandMax -> GameOver(state, DrawGame)
            dealerHandMax < 17 -> stick(dealerTakesDeckCard(state))
            else -> GameOver(state, PlayerWins)
        }
    }

    private fun dealerTakesDeckCard(state: CardsState): CardsState =
        CardsState(
            playerHand = state.playerHand,
            dealerHand = state.dealerHand + state.deck.first(),
            deck = state.deck.drop(1)
        )

    private fun calculateHandValue(hand: List<Card>): BlackJackResult {
        val scores = calculatePossibleValues(hand)
        return when {
            scores.contains(21) && hand.size == 2 -> BlackJack
            scores.isEmpty() -> Bust
            else -> EqualTo21OrLower(scores)
        }
    }

    private fun calculatePossibleValues(hand: List<Card>): Set<Int> =
        calculatePossibleValues(hand.noOfAces(), setOf(hand.scoreOfCardsExcludingAces()))

    private tailrec fun calculatePossibleValues(noOfAces: Int, possibleValues: Set<Int>): Set<Int> =
        if (noOfAces < 1)
            possibleValues.filterNot { score -> score > 21 }.toSet()
        else
            calculatePossibleValues(
                noOfAces - 1,
                possibleValues
                    .flatMap { score -> setOf(score + 1, score + 11) }
                    .toSet()
            )

    private fun List<Card>.noOfAces() = filter { it.rank == ACE }.count()

    private fun List<Card>.scoreOfCardsExcludingAces(): Int =
        fold(0) { score, card -> score + nonAceScore(card) }

    private fun nonAceScore(card: Card): Int =
        when (card.rank) {
            ACE -> 0
            TWO -> 2
            THREE -> 3
            FOUR -> 4
            FIVE -> 5
            SIX -> 6
            SEVEN -> 7
            EIGHT -> 8
            NINE -> 9
            TEN, JACK, QUEEN, KING -> 10
        }
}

