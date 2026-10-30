package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.games.rivet.domain.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetupCardTest {
    @Test
    void theCardOpensOnStartWithTheLastChoice() {
        SetupCard card = new SetupCard(Difficulty.EASY, 2, 3);
        assertEquals(SetupCard.Row.START, card.selected());
        assertEquals(Difficulty.EASY, card.difficulty());
        assertEquals(2, card.section());
    }

    @Test
    void onlySectionsReachedCanBeChosen() {
        SetupCard card = new SetupCard(Difficulty.NORMAL, 3, 2);
        assertEquals(2, card.section(), "a section never reached is not offered");
        card.select(SetupCard.Row.SECTION.ordinal());
        card.change(1);
        assertEquals(1, card.section(), "round again to the first");
        card.change(-1);
        assertEquals(2, card.section());
    }

    @Test
    void theDifficultyGoesRound() {
        SetupCard card = new SetupCard(Difficulty.HARD, 1, 1);
        card.move(1);
        assertEquals(SetupCard.Row.DIFFICULTY, card.selected(), "down from the last row is the first");
        card.change(1);
        assertEquals(Difficulty.EASY, card.difficulty());
        card.change(-1);
        assertEquals(Difficulty.HARD, card.difficulty());
    }
}
