package uk.co.pittendreigh.blackjack

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource


class CardDeckTest {

    @Nested
    inner class `when the cards are provided` {

        @Test
        fun `then there are 52 cards`() {
            assertThat(createDeckOfCards()).hasSize(52)
        }

        @ParameterizedTest
        @EnumSource(Suit::class)
        fun `then the deck has 13 of each suit`(suit: Suit) {
            assertThat(createDeckOfCards().filter { it.suit == suit }).hasSize(13)
        }

        @ParameterizedTest
        @EnumSource(Rank::class)
        fun `then the deck has 4 of each rank`(rank: Rank) {
            assertThat(createDeckOfCards().filter { it.rank == rank }).hasSize(4)
        }
    }

    @Nested
    inner class `given unshuffled cards, when they are shuffled` {

        private val unshuffledCards = createDeckOfCards()
        private val shuffledCards = unshuffledCards.shuffleCards()

        @Test
        fun `then the list of shuffled cards contain all of the provided unshuffled cards`() {
            assertThat(shuffledCards).containsAll(unshuffledCards)
            assertThat(unshuffledCards).containsAll(shuffledCards)
        }

        @Test
        fun `then the shuffled cards will be in a different order than the unshuffled cards`() {
            Assertions.assertTrue(shuffledCards != unshuffledCards)
        }
    }
}

