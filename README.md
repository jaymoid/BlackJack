# Steve Mellor's blackjack challenge

The challenge: build a simple 1 player blackjack game that runs in a terminal. 

### The gameplay should do the following: 
* Shuffle the cards. 
* Deal two to the human, two to the dealer. 
* Human goes first with stick or twist options.
 
### Scoring 
* picture cards are 10, 
* ace is 1 or 11. 
* over 21 is bust. 
* once you stick, it’s over to the dealer who always sticks on 17+. 
* winner is closest to 21 without going over.

### Stick logic (as described by Anthony)
If the player score > dealer score, and the dealer score < 17, they have to take a card. 
If the player score > dealer score, and dealer score >= 17, the dealer sticks and the player wins. 

If the dealer score = player score, and this score < 17, the dealer has to hit again
If the dealer score = player score, and the score is >= 17, then the game is a draw 

If the player score < dealer score, the dealer stands and wins

## My implementation details
* Kotlin
* No mutable variables (vars)
* No mutable state/collections
* TDD all the things
* Functional core / functional shell 
* Derive meaning through types and type signatures
