package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Run;

/**
 * The card before a run, without drawing: how hard, and from which section (only those already reached).
 * Up and down choose a row, left and right change it, Enter starts.
 */
final class SetupCard {
    enum Row { DIFFICULTY, SECTION, START }

    private Difficulty difficulty;
    private int section;
    private final int reached;
    private int selected = Row.START.ordinal();

    SetupCard(Difficulty difficulty, int section, int reached) {
        this.difficulty = difficulty;
        this.reached = Math.max(1, Math.min(Run.SECTIONS, reached));
        this.section = Math.max(1, Math.min(this.reached, section));
    }

    Row selected() {
        return Row.values()[selected];
    }

    void select(int row) {
        selected = Math.max(0, Math.min(Row.values().length - 1, row));
    }

    void move(int direction) {
        select(Math.floorMod(selected + direction, Row.values().length));
    }

    /** Changes the selected row one step: to the right {@code 1}, to the left {@code -1}. */
    void change(int direction) {
        switch (selected()) {
            case DIFFICULTY -> difficulty = Difficulty.values()[Math.floorMod(difficulty.ordinal() + direction, Difficulty.values().length)];
            case SECTION -> section = Math.floorMod(section - 1 + direction, reached) + 1;
            case START -> {
            }
        }
    }

    Difficulty difficulty() {
        return difficulty;
    }

    int section() {
        return section;
    }

    int reached() {
        return reached;
    }
}
