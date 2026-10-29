package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.domain.Ammo;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.domain.Weapon;
import eu.amadeos.games.rivet.repository.RivetSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys and calls the steps itself,
 * so everything happens in order on the test thread. The 3D pictures are left out (empty placeholders).
 */
class RivetGameTest {
    private static final double STEP = 1.0 / RivetGame.UPDATES_PER_SECOND;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final List<Run.Result> results = new ArrayList<>();
    private RivetSettings settings;
    private RivetGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        settings = new RivetSettings(dir.resolve("rivet.properties"));
        game = new RivetGame(keyboard, new Mouse(), new MouseLook(), Gamepads.none(), msg, settings, mixer, null, Runnable::run);
        game.onRunOver(results::add);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double s) {
        steps((int) Math.round(s * RivetGame.UPDATES_PER_SECOND));
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        steps(1);
        keyboard.released(key, now[0]);
        steps(4);
    }

    private void hold(int key, double s) {
        keyboard.pressed(key, now[0]);
        seconds(s);
        keyboard.released(key, now[0]);
        steps(1);
    }

    private BufferedImage draw() {
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        game.render(g, 1);
        game.renderOverlay(g, 1);
        g.dispose();
        return image;
    }

    @Test
    void theTitleWaitsForThePicturesBeforeACardOpens() {
        assertInstanceOf(RivetGame.TitleScene.class, game.top());
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(RivetGame.TitleScene.class, game.top());
        game.useArt(Art.placeholder());
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(RivetGame.SetupScene.class, game.top());
        tap(KeyEvent.VK_ESCAPE);
        assertInstanceOf(RivetGame.TitleScene.class, game.top());
    }

    @Test
    void theCardStartsTheRunAndRemembersTheChoice() {
        game.useArt(Art.placeholder());
        tap(KeyEvent.VK_ENTER);
        RivetGame.SetupScene setup = (RivetGame.SetupScene) game.top();
        tap(KeyEvent.VK_UP);
        tap(KeyEvent.VK_UP);
        tap(KeyEvent.VK_RIGHT);
        assertEquals(Difficulty.HARD, setup.card().difficulty());
        tap(KeyEvent.VK_ENTER);
        RivetGame.PlayScene play = assertInstanceOf(RivetGame.PlayScene.class, game.top());
        assertEquals("1-1", play.world().level().id());
        assertEquals(Difficulty.HARD, settings.difficulty());
    }

    @Test
    void theKeysWalkTurnAndFire() {
        game.useArt(Art.placeholder());
        RivetGame.PlayScene play = game.play(new Run(Difficulty.NORMAL, 1), 3);
        double x = play.world().player().x(), angle = play.world().player().angle();
        hold(KeyEvent.VK_RIGHT, 0.3);
        assertTrue(play.world().player().angle() > angle + 0.5, "turned right");
        hold(KeyEvent.VK_S, 0.3);
        assertNotEquals(x, play.world().player().x(), "walked");
        int rivets = play.world().player().ammo(Ammo.RIVETS);
        hold(KeyEvent.VK_CONTROL, 0.5);
        assertTrue(play.world().player().ammo(Ammo.RIVETS) < rivets, "fired");
        tap(KeyEvent.VK_1);
        assertEquals(Weapon.ICE_PICK, play.world().player().weapon());
        tap(KeyEvent.VK_Q);
        assertEquals(Weapon.RIVET_GUN, play.world().player().weapon());
    }

    @Test
    void tabShowsTheMapAndPPauses() {
        game.useArt(Art.placeholder());
        RivetGame.PlayScene play = game.play(new Run(Difficulty.NORMAL, 1), 3);
        tap(KeyEvent.VK_TAB);
        assertTrue(play.mapShown());
        draw();
        tap(KeyEvent.VK_TAB);
        assertFalse(play.mapShown());
        tap(KeyEvent.VK_P);
        assertInstanceOf(RivetGame.PauseScene.class, game.top());
        double time = play.world().time();
        seconds(1);
        assertEquals(time, play.world().time(), "the level waits");
        tap(KeyEvent.VK_P);
        assertInstanceOf(RivetGame.PlayScene.class, game.top());
    }

    @Test
    void givingUpInThePauseEndsTheRun() {
        game.useArt(Art.placeholder());
        game.play(new Run(Difficulty.NORMAL, 2), 3);
        game.pause();
        tap(KeyEvent.VK_Q);
        assertInstanceOf(RivetGame.TitleScene.class, game.top());
        assertEquals(1, results.size());
        assertEquals("2-1", results.getFirst().levelId());
        assertFalse(results.getFirst().won());
    }

    @Test
    void afterTheFallEnterTriesTheLevelAgain() {
        game.useArt(Art.placeholder());
        RivetGame.PlayScene play = game.play(new Run(Difficulty.HARD, 1), 3);
        // a shot wakes the yard; then the engineer waits for the machines
        hold(KeyEvent.VK_CONTROL, 0.1);
        for (int i = 0; i < 120 && !(game.top() instanceof RivetGame.DeadScene); i++) seconds(1);
        assertInstanceOf(RivetGame.DeadScene.class, game.top());
        draw();
        seconds(1.2);
        tap(KeyEvent.VK_ENTER);
        RivetGame.PlayScene again = assertInstanceOf(RivetGame.PlayScene.class, game.top());
        assertNotEquals(play, again);
        assertEquals("1-1", again.world().level().id());
        assertEquals(100, again.world().player().health());
    }

    @Test
    void reachingASectionMakesItAChoiceOnTheCard() {
        game.useArt(Art.placeholder());
        assertEquals(1, settings.reached());
        game.play(new Run(Difficulty.NORMAL, 2), 3);
        assertEquals(2, settings.reached());
    }

    @Test
    void everySceneDraws() {
        BufferedImage title = draw();
        assertTrue(title.getRGB(320, 100) != 0, "the sky of the landing pad");
        game.useArt(Art.placeholder());
        tap(KeyEvent.VK_ENTER);
        draw();
        game.play(new Run(Difficulty.NORMAL, 3), 5);
        steps(10);
        draw();
        game.pause();
        draw();
    }
}
