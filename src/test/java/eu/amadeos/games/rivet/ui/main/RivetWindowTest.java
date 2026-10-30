package eu.amadeos.games.rivet.ui.main;

import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.repository.RivetSettings;
import eu.amadeos.games.rivet.ui.game.RivetGame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window, game and loop: keys on the canvas reach the menu, the graphics menu reaches the canvas. */
class RivetWindowTest {
    @TempDir
    Path dir;

    private RivetWindow window;
    private RivetGame game;
    private RivetSettings settings;

    @BeforeEach
    void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        Messages msg = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH);
        settings = new RivetSettings(dir.resolve("rivet.properties"));
        HighScores scores = new HighScores(dir.resolve("scores"), Ranking.HIGHER_WINS);
        Keyboard keyboard = new Keyboard();
        Mouse mouse = new Mouse();
        MouseLook look = new MouseLook();
        onEdt(() -> {
            game = new RivetGame(keyboard, mouse, look, Gamepads.none(), msg, settings, new Mixer(),
                    new AppDirectories(dir.resolve("config"), dir.resolve("cache")));
            window = new RivetWindow(msg, scores, new PlayerName(dir, "tester"), game, keyboard, mouse, look);
            RivetPresenter presenter = new RivetPresenter(window, game, msg, settings, scores);
            window.bind(presenter);
            presenter.start();
            window.setVisible(true);
            game.start(window.canvas());
        });
    }

    @AfterEach
    void close() throws Exception {
        if (game != null) game.stop();
        if (window != null) onEdt(window::dispose);
    }

    private static void onEdt(Runnable code) throws Exception {
        try {
            SwingUtilities.invokeAndWait(code);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) throw error;
            throw (Exception) e.getCause();
        }
    }

    private void key(int code) throws Exception {
        onEdt(() -> window.canvas().dispatchEvent(new KeyEvent(window.canvas(), KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, code, KeyEvent.CHAR_UNDEFINED)));
    }

    private JMenuItem item(String name) {
        return find(window.getJMenuBar(), name);
    }

    private static JMenuItem find(MenuElement element, String name) {
        for (MenuElement child : element.getSubElements()) {
            if (child instanceof JMenuItem item && name.equals(item.getName())) return item;
            JMenuItem found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void theSavedRenderModeIsUsedAndTheMenuChangesIt() throws Exception {
        onEdt(() -> {
            assertEquals(RenderMode.PIXEL, window.canvas().renderMode());
            assertTrue(item("game-graphics-pixel").isSelected());
            JMenuItem direct = item("game-graphics-direct");
            assertNotNull(direct);
            direct.doClick(0);
            assertEquals(RenderMode.DIRECT, window.canvas().renderMode());
        });
        assertEquals(RenderMode.DIRECT, settings.renderMode());
    }

    @Test
    void theMouseSpeedMenuKeepsTheSetting() throws Exception {
        onEdt(() -> {
            assertTrue(item("game-mouse-5").isSelected());
            item("game-mouse-8").doClick(0);
        });
        assertEquals(8, settings.mouseSpeed());
    }

    @Test
    void tabStaysWithTheCanvasForTheMap() throws Exception {
        onEdt(() -> assertFalse(window.canvas().getFocusTraversalKeysEnabled()));
    }

    @Test
    void theSoundMenuKeepsTheSetting() throws Exception {
        onEdt(() -> {
            JMenuItem sound = item("game-sound");
            assertTrue(sound.isSelected());
            sound.doClick(0);
        });
        assertFalse(settings.sound());
    }

    @Test
    void f12ShowsTheFrameTimesAndF11SwitchesToFullScreen() throws Exception {
        key(KeyEvent.VK_F12);
        onEdt(() -> assertTrue(window.canvas().showsFrameTimes()));
        key(KeyEvent.VK_F11);
        onEdt(() -> assertTrue(window.fullScreen().active(), "full screen"));
        key(KeyEvent.VK_F11);
        onEdt(() -> assertFalse(window.fullScreen().active(), "back in the window"));
        long deadline = System.currentTimeMillis() + 3000;
        while (window.canvas().frameTimes().framesPerSecond() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(window.canvas().frameTimes().framesPerSecond() > 0, "frames are drawn and measured");
    }
}
