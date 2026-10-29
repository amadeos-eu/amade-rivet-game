package eu.amadeos.games.rivet.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.repository.RivetSettings;
import eu.amadeos.games.rivet.ui.game.Arena;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Everything around the station, without Swing: the menu commands, the settings, pausing when the window
 * loses the focus, and the best runs. All methods run on the Swing thread.
 */
public final class RivetPresenter {
    /** The one table: the points of the runs, won or given up. */
    static final String TABLE = "runs";

    private final RivetView view;
    private final Arena arena;
    private final Messages msg;
    private final RivetSettings settings;
    private final HighScores scores;
    private final Supplier<Instant> now;

    @Inject
    public RivetPresenter(RivetView view, Arena arena, Messages msg, RivetSettings settings, HighScores scores) {
        this(view, arena, msg, settings, scores, Instant::now);
    }

    RivetPresenter(RivetView view, Arena arena, Messages msg, RivetSettings settings, HighScores scores, Supplier<Instant> now) {
        this.view = view;
        this.arena = arena;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.now = now;
    }

    public void start() {
        boolean sound = settings.sound();
        RenderMode mode = settings.renderMode();
        view.showOptions(sound, mode, settings.mouseSpeed());
        view.useRenderMode(mode);
        arena.setSound(sound);
        arena.setMouseSpeed(settings.mouseSpeed());
        arena.onRunOver(this::runOver);
        arena.showTitle();
    }

    /** Opens the card where the difficulty and the section are chosen. */
    public void newGame() {
        arena.newGame();
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the keys, so a running level waits and the mouse is let go. */
    public void focusLost() {
        arena.pause();
    }

    public void setSound(boolean on) {
        settings.setSound(on);
        arena.setSound(on);
    }

    public void setRenderMode(RenderMode mode) {
        settings.setRenderMode(mode);
        view.useRenderMode(mode);
    }

    public void setMouseSpeed(int speed) {
        settings.setMouseSpeed(speed);
        arena.setMouseSpeed(settings.mouseSpeed());
    }

    public void showScores() {
        view.showScores(-1);
    }

    public void showAbout() {
        view.showAbout(arena.gamepads());
    }

    /** A run ended: its points enter the table if they are good enough. */
    void runOver(Run.Result result) {
        if (result.score() <= 0 || !scores.qualifies(TABLE, result.score())) return;
        String key = result.won() ? "scores.won" : "scores.reached";
        view.askName(msg.format(key, result.score(), result.levelId(), msg.get(result.difficulty().key()))).ifPresent(name -> {
            int place = scores.add(TABLE, new Score(name, result.score(), now.get()));
            view.showScores(place);
        });
    }
}
