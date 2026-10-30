package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.raycast.Texture;
import eu.amadeos.gameengine.raycast.Views;
import eu.amadeos.gameengine.raycast.model.ModelRenderer;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Input;
import eu.amadeos.games.rivet.domain.Machine;
import eu.amadeos.games.rivet.domain.MachineKind;
import eu.amadeos.games.rivet.domain.Player;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.domain.World;
import eu.amadeos.games.rivet.repository.RivetSettings;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

/**
 * The screenshots of chapter 60 and of the README, drawn without a screen: the title, the setup card, a
 * machine in the landing pad's yard under the giant planet, the pump hall, the workshops, the map and the drone from
 * its eight sides. {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    private static final double STEP = 1.0 / RivetGame.UPDATES_PER_SECOND;
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final RivetGame game;

    private Screenshots() throws Exception {
        Messages msg = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH);
        RivetSettings settings = new RivetSettings(Files.createTempDirectory("rivet").resolve("rivet.properties"));
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        game = new RivetGame(keyboard, new Mouse(), new MouseLook(), Gamepads.none(), msg, settings, mixer, null, Runnable::run);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots();
        shots.game.loadArt();
        shots.seconds(3);
        shots.save(new File(dir, "title.png"));
        shots.tap(KeyEvent.VK_ENTER);
        shots.seconds(0.5);
        shots.save(new File(dir, "setup.png"));

        // The landing pad: a drone over the yard, the planet behind it.
        RivetGame.PlayScene pad = shots.game.play(new Run(Difficulty.NORMAL, 1), 11);
        shots.face(pad, MachineKind.DRONE, 0.12);
        shots.fire(pad, 0.33);
        shots.save(new File(dir, "landing-pad.png"));
        shots.save(new File(dir, "../amade-rivet-game.png"));

        RivetGame.PlayScene hall = shots.game.play(level(1), 12);
        shots.face(hall, null, 0);
        shots.walk(hall, 1.0);
        shots.face(hall, null, 0.05);
        shots.fire(hall, 0.33);
        shots.save(new File(dir, "pump-hall.png"));
        shots.tap(KeyEvent.VK_TAB);
        shots.save(new File(dir, "map.png"));
        shots.tap(KeyEvent.VK_TAB);

        RivetGame.PlayScene workshops = shots.game.play(level(4), 13);
        shots.face(workshops, null, -0.1);
        shots.fire(workshops, 0.3);
        shots.save(new File(dir, "workshops.png"));

        drone(new File(dir, "drone-views.png"));
        System.exit(0);
    }

    /** The drone from its eight sides, as the game renders it for the billboards, on a dark strip. */
    private static void drone(File file) throws Exception {
        Views views = new ModelRenderer().views(Models::drone, 1, Art.DIRECTIONS, Art.MACHINE_SIZE, 12);
        int size = Art.MACHINE_SIZE;
        BufferedImage image = new BufferedImage(size * Art.DIRECTIONS, size, BufferedImage.TYPE_INT_RGB);
        for (int d = 0; d < Art.DIRECTIONS; d++) {
            Texture t = views.pick(0, 0, 2 * Math.PI * d / Art.DIRECTIONS);
            for (int v = 0; v < size; v++) for (int u = 0; u < size; u++) {
                image.setRGB(d * size + u, v, t.opaque(u, v) ? t.argb(u, v) : 0x1b222a);
            }
        }
        ImageIO.write(image, "png", file);
    }

    /** A run standing at the start of level {@code index} of {@link Run#LEVELS}. */
    private static Run level(int index) {
        Run run = new Run(Difficulty.NORMAL, index / 3 + 1);
        for (int i = index % 3; i > 0; i--) run.levelDone(run.start(new Textures(), new SeededRandom(1)));
        return run;
    }

    /** Turns to the nearest machine of a kind ({@code null}: any) the player can see, plus {@code off} radians. */
    private void face(RivetGame.PlayScene play, MachineKind kind, double off) {
        World w = play.world();
        Player p = w.player();
        Machine best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Machine m : w.machines()) {
            double d = Math.hypot(m.x() - p.x(), m.y() - p.y());
            if (!m.alive() || kind != null && m.kind() != kind || !w.map().sees(p.x(), p.y(), m.x(), m.y()) || d > bestDistance) continue;
            best = m;
            bestDistance = d;
        }
        if (best == null) return;
        double want = Math.atan2(best.y() - p.y(), best.x() - p.x()) + off;
        double turn = Math.atan2(Math.sin(want - p.angle()), Math.cos(want - p.angle()));
        play.step(STEP, new Input(0, 0, turn, false, false, false, 0, 0));
        game.effects().update(0);
    }

    private void walk(RivetGame.PlayScene play, double seconds) {
        for (int i = 0; i < seconds / STEP; i++) play.step(STEP, new Input(1, 0, 0, false, false, false, 0, 0));
    }

    /** Fires, and stops {@code seconds} later with the shot's light still on. */
    private void fire(RivetGame.PlayScene play, double seconds) {
        for (int i = 0; i < seconds / STEP; i++) play.step(STEP, new Input(0, 0, 0, false, true, false, 0, 0));
        play.step(STEP, new Input(0, 0, 0, false, false, false, 0, 0));
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        steps(1);
        keyboard.released(key, now[0]);
        steps(4);
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

    private void save(File file) throws Exception {
        BufferedImage image = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.scale(2, 2);
        game.render(g, 1);
        game.renderOverlay(g, 1);
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
