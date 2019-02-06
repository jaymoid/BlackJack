package uk.co.pittendreigh.blackjack

fun main() =
    TerminalBlackJackInterface(
        BlackJackGame(::createDeckOfCards, ::shuffleCards),
        System.out,
        System.`in`
    ).startGame()

