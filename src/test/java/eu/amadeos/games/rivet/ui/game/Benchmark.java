package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Input;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.repository.RivetSettings;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Locale;

/**
 * How long a frame of each level takes to draw, in the logical 640 x 400 and at twice that:
 * {@code ./gradlew benchmark}. The engineer turns on the spot at the start, so every column of the view
 * changes from frame to frame, and the machines that see the engineer come closer. The frames are drawn into
 * a compatible image, as the canvas draws, but without waiting for the screen.
 */
public final class Benchmark {
    static final int WARM_UP = 60;
    static final int FRAMES = 300;

    public static void main(String[] args) throws Exception {
        Messages msg = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        RivetGame game = new RivetGame(new Keyboard(), new Mouse(), new MouseLook(), Gamepads.none(), msg,
                new RivetSettings(Files.createTempDirectory("rivet").resolve("rivet.properties")), mixer, null,
                Runnable::run);
        game.loadArt();
        GraphicsConfiguration gc = GraphicsEnvironment.isHeadless() ? null
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        double step = 1.0 / RivetGame.UPDATES_PER_SECOND;
        Input turn = new Input(0, 0, 0.9 * step, false, false, false, 0, 0);
        System.out.println("ms per frame");
        for (int scale : new int[]{1, 2}) {
            for (int level = 0; level < Run.LEVELS.size(); level++) {
                Run run = new Run(Difficulty.NORMAL, level / 3 + 1);
                for (int i = level % 3; i > 0; i--) run.levelDone(run.start(new Textures(), new SeededRandom(1)));
                RivetGame.PlayScene play = game.play(run, level);
                int w = RivetGame.WIDTH * scale;
                int h = Viewport.HEIGHT * scale;
                BufferedImage image = gc != null ? gc.createCompatibleImage(w, h) : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                long total = 0;
                for (int i = 0; i < WARM_UP + FRAMES; i++) {
                    play.step(step, turn);
                    play.step(step, turn);
                    Graphics2D g = image.createGraphics();
                    g.scale(scale, scale);
                    long start = System.nanoTime();
                    game.render(g, 0.5);
                    game.renderOverlay(g, 0.5);
                    if (i >= WARM_UP) total += System.nanoTime() - start;
                    g.dispose();
                }
                System.out.printf(Locale.ROOT, "%-4s at %4d x %4d   %6.2f%n", Run.LEVELS.get(level), w, h, total / 1e6 / FRAMES);
            }
        }
        System.exit(0);
    }
}
