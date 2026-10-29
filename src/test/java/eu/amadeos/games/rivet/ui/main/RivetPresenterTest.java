package eu.amadeos.games.rivet.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.repository.RivetSettings;
import eu.amadeos.games.rivet.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RivetPresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private Consumer<Run.Result> runOver;
    private RivetPresenter presenter;
    private HighScores scores;
    private RivetSettings settings;

    private final RivetView view = new RivetView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode, int mouseSpeed) {
            log.add("options " + sound + " " + mode + " " + mouseSpeed);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public Optional<String> askName(String message) {
            log.add("ask " + message);
            return Optional.of("Ada");
        }

        @Override
        public void showScores(int highlight) {
            log.add("scores " + highlight);
        }

        @Override
        public void showAbout(String gamepads) {
            log.add("about " + gamepads);
        }
    };

    private final Arena arena = new Arena() {
        @Override
        public void showTitle() {
            log.add("title");
        }

        @Override
        public void newGame() {
            log.add("new");
        }

        @Override
        public void pause() {
            log.add("pause");
        }

        @Override
        public void setSound(boolean on) {
            log.add("sound " + on);
        }

        @Override
        public void setMouseSpeed(int speed) {
            log.add("mouse " + speed);
        }

        @Override
        public String gamepads() {
            return "Gamepads: none, 0 connected";
        }

        @Override
        public void onRunOver(Consumer<Run.Result> listener) {
            runOver = listener;
        }
    };

    @BeforeEach
    void create() {
        Messages msg = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH);
        settings = new RivetSettings(dir.resolve("rivet.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.HIGHER_WINS);
        presenter = new RivetPresenter(view, arena, msg, settings, scores, () -> Instant.EPOCH);
        presenter.start();
    }

    @Test
    void theSettingsAreAppliedAtStartAndKept() {
        assertEquals(List.of("options true PIXEL 5", "mode PIXEL", "sound true", "mouse 5", "title"), log);
        log.clear();
        presenter.setRenderMode(RenderMode.DIRECT);
        presenter.setSound(false);
        presenter.setMouseSpeed(8);
        assertEquals(List.of("mode DIRECT", "sound false", "mouse 8"), log);
        assertEquals(RenderMode.DIRECT, settings.renderMode());
        assertEquals(8, settings.mouseSpeed());
    }

    @Test
    void aRunEntersTheTableWonOrNot() {
        log.clear();
        runOver.accept(new Run.Result(1840, Difficulty.HARD, "2-2", false));
        runOver.accept(new Run.Result(9200, Difficulty.NORMAL, "3-3", true));
        assertEquals(List.of("ask Run ended at level 2-2: 1,840 points (Hard).", "scores 0",
                "ask Station cleared: 9,200 points (Normal).", "scores 0"), log);
        assertEquals(9200, scores.list(RivetPresenter.TABLE).getFirst().value());
    }

    @Test
    void aRunWithoutPointsEntersNothing() {
        log.clear();
        runOver.accept(new Run.Result(0, Difficulty.EASY, "1-1", false));
        assertEquals(List.of(), log);
    }

    @Test
    void losingTheFocusPausesTheLevel() {
        log.clear();
        presenter.focusLost();
        presenter.newGame();
        presenter.showAbout();
        assertEquals(List.of("pause", "new", "about Gamepads: none, 0 connected"), log);
    }
}
