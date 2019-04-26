package uk.co.pittendreigh.blackjack

fun createDeckOfCards(): List<Card> =
    Suit.values()
        .flatMap { suit ->
            Rank.values()
                .map { rank -> rank of suit }
        }

data class Card(val rank: Rank, val suit: Suit) {
    override fun toString() = "[$rank of $suit]"
}

fun Iterable<Card>.shuffleCards(): List<Card> = shuffled()

infix fun Rank.of(suit: Suit): Card = Card(this, suit)
enum class Suit {
    CLUBS,
    HEARTS,
    SPADES,
    DIAMONDS,
}

enum class Rank {
    ACE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING
}
