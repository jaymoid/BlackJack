package uk.co.pittendreigh.blackjack

fun main() =
    TerminalBlackJackInterface(
        BlackJackGame(::createDeckOfCards, List<Card>::shuffleCards),
        System.out,
        System.`in`
    ).startGame()

