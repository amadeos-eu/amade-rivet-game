package eu.amadeos.games.rivet.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;

import java.util.Optional;

/** What {@link RivetPresenter} needs from the window; tests replace it. The level itself is drawn by the game. */
public interface RivetView {
    /** Shows the settings in the menu. */
    void showOptions(boolean sound, RenderMode mode, int mouseSpeed);

    /** Switches the canvas to a render mode. */
    void useRenderMode(RenderMode mode);

    /** Asks for the player's name after a run; empty if cancelled. */
    Optional<String> askName(String message);

    /** Shows the best runs; {@code highlight} -1 for no new entry. */
    void showScores(int highlight);

    /** The About dialog, with a line on the gamepads. */
    void showAbout(String gamepads);
}
