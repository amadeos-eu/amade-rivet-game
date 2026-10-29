package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.games.rivet.domain.Run;

import java.util.function.Consumer;

/**
 * What the window and the presenter may ask of the running game, from the Swing thread: the game carries it
 * out on its own loop thread.
 */
public interface Arena {
    void showTitle();

    /** Opens the card where the difficulty and the section are chosen. */
    void newGame();

    /** Pauses a level being played, and lets the mouse go. */
    void pause();

    void setSound(boolean on);

    /** How fast the mouse turns the view, 1 to 10. */
    void setMouseSpeed(int speed);

    /** A line on the gamepads found, for the About dialog. */
    String gamepads();

    /** Hears every run that ends, won or given up, on the Swing thread. */
    void onRunOver(Consumer<Run.Result> listener);
}
