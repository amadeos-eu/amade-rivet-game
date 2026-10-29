package eu.amadeos.games.rivet.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Gamepad;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.rivet.domain.Input;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.domain.World;
import eu.amadeos.games.rivet.repository.RivetSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The running game: the loop, the controls, the effects and the scenes (the title over the landing pad, the
 * setup card, a level being played, the pause, the end of a level, the engineer's fall, the end of the run).
 * Everything here runs on the game loop thread, except the {@link Arena} methods, which only post commands
 * to it. The 3D pictures are rendered on a thread of their own while the title is shown.
 */
public final class RivetGame implements Arena {
    enum Action {
        FORWARD, BACK, TURN_LEFT, TURN_RIGHT, STRAFE_LEFT, STRAFE_RIGHT, RUN, FIRE, USE, NEXT_TOOL, MAP, PAUSE, ESCAPE, OK
    }

    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    /** Turning with the keys, in radians per second. */
    static final double TURN_SPEED = 2.6;
    /** Turning with the mouse at speed 5, in radians per pixel. */
    static final double MOUSE_TURN = 0.0028;
    /** The view widens to 16:9 on a wide screen; the height stays 400. */
    static final int WIDTH = 640, MAX_WIDTH = 712;
    /** Seconds the lift hums before the level's card. */
    static final double LIFT_SECONDS = 0.9;
    static final double FALL_SECONDS = 1.6;

    private static final Logger log = LoggerFactory.getLogger(RivetGame.class);

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final MouseLook look;
    private final Gamepads gamepads;
    private final Messages msg;
    private final Mixer mixer;
    private final RivetSettings settings;
    private final Path cache;
    private final Textures textures = new Textures();
    private final Viewport viewport = new Viewport(textures.sky());
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private final Controls<Action> controls;
    private final Executor ui;
    private volatile Consumer<Run.Result> onRunOver = result -> { };
    private volatile Art art;
    private volatile double loaded;
    private volatile int mouseSpeed;
    private GameCanvas canvas;
    private GameLoop loop;
    private boolean captured;
    private long seed = SeededRandom.freshSeed();

    @Inject
    public RivetGame(Keyboard keyboard, Mouse mouse, MouseLook look, Gamepads gamepads, Messages msg, RivetSettings settings,
                     Mixer mixer, AppDirectories directories) {
        this(keyboard, mouse, look, gamepads, msg, settings, mixer, directories.cache(), SwingUtilities::invokeLater);
    }

    RivetGame(Keyboard keyboard, Mouse mouse, MouseLook look, Gamepads gamepads, Messages msg, RivetSettings settings, Mixer mixer,
              Path cache, Executor ui) {
        this.keyboard = keyboard;
        this.mouse = mouse;
        this.look = look;
        this.gamepads = gamepads;
        this.msg = msg;
        this.mixer = mixer;
        this.settings = settings;
        this.cache = cache;
        this.ui = ui;
        this.hud = new Hud(msg);
        this.effects = new Effects(mixer, msg);
        this.mouseSpeed = settings.mouseSpeed();
        // --8<-- [start:controls]
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.FORWARD, settings.keys("forward", KeyEvent.VK_W, KeyEvent.VK_UP))
                .bind(Action.BACK, settings.keys("back", KeyEvent.VK_S, KeyEvent.VK_DOWN))
                .bind(Action.TURN_LEFT, settings.keys("turn-left", KeyEvent.VK_LEFT))
                .bind(Action.TURN_RIGHT, settings.keys("turn-right", KeyEvent.VK_RIGHT))
                .bind(Action.STRAFE_LEFT, settings.keys("strafe-left", KeyEvent.VK_A))
                .bind(Action.STRAFE_RIGHT, settings.keys("strafe-right", KeyEvent.VK_D))
                .bind(Action.RUN, settings.keys("run", KeyEvent.VK_SHIFT))
                .bind(Action.FIRE, settings.keys("fire", KeyEvent.VK_CONTROL))
                .bind(Action.USE, settings.keys("use", KeyEvent.VK_SPACE, KeyEvent.VK_E))
                .bind(Action.NEXT_TOOL, settings.keys("next-tool", KeyEvent.VK_Q))
                .bind(Action.MAP, settings.keys("map", KeyEvent.VK_TAB, KeyEvent.VK_M))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.ESCAPE, KeyEvent.VK_ESCAPE)
                .bind(Action.OK, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        for (Gamepad pad : gamepads.pads()) {
            controls.bind(Action.FORWARD, pad, Gamepad.Button.UP)
                    .bind(Action.BACK, pad, Gamepad.Button.DOWN)
                    .bind(Action.TURN_LEFT, pad, Gamepad.Button.LEFT)
                    .bind(Action.TURN_RIGHT, pad, Gamepad.Button.RIGHT)
                    .bind(Action.STRAFE_LEFT, pad, Gamepad.Button.LB)
                    .bind(Action.STRAFE_RIGHT, pad, Gamepad.Button.RB)
                    .bind(Action.FIRE, pad, Gamepad.Button.A)
                    .bind(Action.USE, pad, Gamepad.Button.X)
                    .bind(Action.NEXT_TOOL, pad, Gamepad.Button.Y)
                    .bind(Action.MAP, pad, Gamepad.Button.BACK)
                    .bind(Action.PAUSE, pad, Gamepad.Button.START)
                    .bind(Action.ESCAPE, pad, Gamepad.Button.B)
                    .bind(Action.OK, pad, Gamepad.Button.A, Gamepad.Button.START);
        }
        // --8<-- [end:controls]
        scenes.push(new TitleScene());
    }

    /** Starts the loop on the canvas, and the rendering of the 3D pictures beside it. */
    public void start(GameCanvas canvas) {
        this.canvas = canvas;
        Thread painter = new Thread(this::loadArt, "rivet-art");
        painter.setDaemon(true);
        painter.setPriority(Thread.NORM_PRIORITY - 1);
        painter.start();
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    /** Renders (or reads from the cache) every picture; then the game can start. */
    void loadArt() {
        long start = System.nanoTime();
        try {
            art = Art.load(cache, p -> loaded = p);
            log.info("3D pictures ready after {} ms", (System.nanoTime() - start) / 1_000_000);
        } catch (RuntimeException e) {
            log.error("Cannot make the 3D pictures", e);
        }
    }

    boolean ready() {
        return art != null;
    }

    void update(double seconds) {
        keyboard.poll();
        mouse.poll();
        look.poll();
        gamepads.poll();
        Art a = art;
        if (a != null && effects.art() == null) effects.setArt(a);
        scenes.update(seconds);
        capture(scenes.top() instanceof PlayScene);
    }

    /** The mouse turns the view only while a level is played; on the cards it is an ordinary pointer. */
    private void capture(boolean on) {
        if (on == captured) return;
        captured = on;
        ui.execute(() -> look.capture(on));
    }

    int width() {
        return canvas == null ? WIDTH : canvas.logicalWidth();
    }

    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newGame() {
        run(() -> {
            if (art != null && !(scenes.top() instanceof SetupScene)) scenes.replace(new SetupScene(new TitleScene()));
        });
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public void setMouseSpeed(int speed) {
        mouseSpeed = speed;
    }

    @Override
    public String gamepads() {
        return msg.format("about.gamepads", gamepads.backend(), gamepads.connected());
    }

    @Override
    public void onRunOver(Consumer<Run.Result> listener) {
        onRunOver = listener;
    }

    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    private void runOver(Run run) {
        Run.Result result = run.result();
        Consumer<Run.Result> listener = onRunOver;
        ui.execute(() -> listener.accept(result));
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    Effects effects() {
        return effects;
    }

    Viewport viewport() {
        return viewport;
    }

    void useArt(Art art) {
        this.art = art;
        this.loaded = 1;
    }

    /** Plays a run at once, without the title and the card. */
    PlayScene play(Run run, long seed) {
        this.seed = seed;
        PlayScene play = new PlayScene(run);
        scenes.replace(play);
        return play;
    }

    // ---- input -----------------------------------------------------------------------------------

    // --8<-- [start:input]
    /** The keys, the mouse and the gamepad as one step of the player. */
    private Input input(double seconds) {
        boolean run = controls.isDown(Action.RUN);
        double forward = controls.axis(Action.BACK, Action.FORWARD);
        double strafe = controls.axis(Action.STRAFE_LEFT, Action.STRAFE_RIGHT);
        double turn = controls.axis(Action.TURN_LEFT, Action.TURN_RIGHT) * TURN_SPEED * seconds * (run ? 1.3 : 1);
        for (Gamepad pad : gamepads.pads()) {
            // the stick turns and walks smoothly, not just on or off
            if (Math.abs(pad.x()) > 0 && Math.abs(turn) < 1e-9) turn = pad.x() * TURN_SPEED * seconds;
            if (Math.abs(pad.y()) > 0 && forward == 0) forward = -pad.y();
        }
        turn += look.dx() * MOUSE_TURN * mouseSpeed / 5.0;
        boolean fire = controls.isDown(Action.FIRE) || mouse.isDown(MouseEvent.BUTTON1);
        boolean use = controls.wasPressed(Action.USE) || mouse.wasPressed(MouseEvent.BUTTON3);
        int slot = 0;
        for (int i = 1; i <= 6; i++) if (keyboard.wasPressed(KeyEvent.VK_0 + i)) slot = i;
        int cycle = controls.wasPressed(Action.NEXT_TOOL) ? 1 : Integer.signum(mouse.wheel());
        return new Input(forward, strafe, turn, run, fire, use, slot, cycle);
    }
    // --8<-- [end:input]

    // ---- the scenes ------------------------------------------------------------------------------

    /** The landing pad under the giant planet, the view slowly turning, while the pictures are made. */
    final class TitleScene implements Scene {
        private final World pad;
        private double time;

        TitleScene() {
            Run demo = new Run(settings.difficulty(), 1);
            pad = demo.start(textures, new SeededRandom(1));
            viewport.show(pad);
            effects.clear();
            effects.titleMusic();
        }

        @Override
        public void update(double seconds) {
            time += seconds;
            effects.update(seconds);
            if (art == null) return;
            if (controls.wasPressed(Action.OK) || controls.wasPressed(Action.FIRE) || mouse.wasPressed(MouseEvent.BUTTON1)) {
                scenes.replace(new SetupScene(this));
            }
        }

        void show() {
            if (viewport.world() != pad) viewport.show(pad);
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            show();
            viewport.render(g, width(), effects, art, 0, false, time * 0.12);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.title(g, width(), time, art == null ? loaded : 1);
        }
    }

    // --8<-- [start:setup]
    /** The card before a run, over the turning title. Enter starts, Esc goes back. */
    final class SetupScene implements Scene {
        private final TitleScene behind;
        private final SetupCard card;

        SetupScene(TitleScene behind) {
            this.behind = behind;
            this.card = new SetupCard(settings.difficulty(), settings.section(), settings.reached());
        }

        @Override
        public void update(double seconds) {
            behind.time += seconds;
            effects.update(seconds);
            if (controls.wasPressed(Action.ESCAPE)) {
                scenes.replace(behind);
                return;
            }
            if (controls.wasPressed(Action.FORWARD)) card.move(-1);
            if (controls.wasPressed(Action.BACK)) card.move(1);
            if (controls.wasPressed(Action.TURN_LEFT) || controls.wasPressed(Action.STRAFE_LEFT)) card.change(-1);
            if (controls.wasPressed(Action.TURN_RIGHT) || controls.wasPressed(Action.STRAFE_RIGHT)) card.change(1);
            if (mouse.wasPressed(MouseEvent.BUTTON1)) {
                int row = Hud.setupRowAt(width(), mouse.x(), mouse.y());
                if (row >= 0) {
                    boolean again = row == card.selected().ordinal();
                    card.select(row);
                    if (card.selected() == SetupCard.Row.START) {
                        start();
                        return;
                    }
                    if (again) card.change(1);
                }
            }
            if (keyboard.wasPressed(KeyEvent.VK_ENTER) || controls.wasPressed(Action.OK) && card.selected() == SetupCard.Row.START) {
                start();
            }
        }

        private void start() {
            settings.setRun(card.difficulty(), card.section());
            scenes.replace(new PlayScene(new Run(card.difficulty(), card.section())));
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            behind.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.setup(g, width(), card);
        }

        SetupCard card() {
            return card;
        }
    }
    // --8<-- [end:setup]

    // --8<-- [start:play]
    /** A level being played. */
    final class PlayScene implements Scene {
        private final Run run;
        private final World world;
        private boolean map;
        private double lift;
        private double dying;

        PlayScene(Run run) {
            this.run = run;
            this.world = run.start(textures, new SeededRandom(seed++));
            viewport.show(world);
            effects.clear();
            effects.sectionMusic(run.section());
            effects.message(hud.levelName(world.level().id()), 0x9fd4ec);
            settings.reach(run.section());
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            if (controls.wasPressed(Action.MAP)) map = !map;
            step(seconds, world.player().alive() && !world.done() ? input(seconds) : Input.NONE);
            if (world.done() && (lift += seconds) >= LIFT_SECONDS) {
                run.levelDone(world);
                scenes.replace(new LevelDoneScene(this));
            } else if (!world.player().alive() && dying >= FALL_SECONDS) {
                scenes.replace(new DeadScene(this));
            }
        }

        void step(double seconds, Input input) {
            world.update(seconds, input);
            effects.react(world);
            effects.update(seconds);
            if (!world.player().alive()) dying += seconds;
        }
        // --8<-- [end:play]

        @Override
        public void render(Graphics2D g, double alpha) {
            if (viewport.world() != world) viewport.show(world);
            viewport.render(g, width(), effects, art, dying, true, 0);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.play(g, width(), world, effects, effects.time());
            if (map) hud.map(g, width(), world, viewport);
        }

        World world() {
            return world;
        }

        Run run() {
            return run;
        }

        boolean mapShown() {
            return map;
        }
    }

    /** Drawn over the frozen level until P, Esc, Enter or Start; Q gives up the run. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            // Esc (bound to the pause) goes on; Q, or B on a gamepad, gives up
            if (keyboard.wasPressed(KeyEvent.VK_Q) || controls.wasPressed(Action.ESCAPE) && !keyboard.wasPressed(KeyEvent.VK_ESCAPE)) {
                giveUp();
            } else if (controls.wasPressed(Action.PAUSE) || controls.wasPressed(Action.OK)) {
                scenes.pop();
            }
        }

        private void giveUp() {
            scenes.pop();
            if (scenes.top() instanceof PlayScene play) {
                play.run.giveUp(play.world);
                runOver(play.run);
            }
            scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.pause(g, width());
        }
    }

    /** The lift was reached: the level's figures, then the next level or the end of the run. */
    final class LevelDoneScene implements Scene {
        private final PlayScene play;
        private double shown;

        LevelDoneScene(PlayScene play) {
            this.play = play;
            effects.levelDoneMusic();
        }

        @Override
        public void update(double seconds) {
            shown += seconds;
            effects.update(seconds);
            if (shown < 1.0) return;
            if (controls.wasPressed(Action.OK) || controls.wasPressed(Action.FIRE) || mouse.wasPressed(MouseEvent.BUTTON1)) {
                if (play.run.finished()) {
                    scenes.replace(new WonScene(play.run));
                    runOver(play.run);
                } else {
                    scenes.replace(new PlayScene(play.run));
                }
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            play.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.levelDone(g, width(), play.world, shown);
        }
    }

    /** The suit gave up: Enter tries the level again as it was entered, Esc ends the run. */
    final class DeadScene implements Scene {
        private final PlayScene play;
        private double shown;

        DeadScene(PlayScene play) {
            this.play = play;
            effects.deadMusic();
        }

        @Override
        public void update(double seconds) {
            shown += seconds;
            play.step(seconds, Input.NONE);
            if (shown < 1.0) return;
            if (controls.wasPressed(Action.ESCAPE)) {
                play.run.giveUp(play.world);
                runOver(play.run);
                scenes.replace(new TitleScene());
            } else if (controls.wasPressed(Action.OK) || controls.wasPressed(Action.FIRE) || mouse.wasPressed(MouseEvent.BUTTON1)) {
                play.run.retry();
                scenes.replace(new PlayScene(play.run));
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            play.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.play(g, width(), play.world, effects, effects.time());
            hud.dead(g, width(), shown);
        }
    }

    /** The Foreman's core is cold and the lift goes up: the score, then back to the title. */
    final class WonScene implements Scene {
        private final Run run;
        private double shown;

        WonScene(Run run) {
            this.run = run;
            effects.wonMusic();
        }

        @Override
        public void update(double seconds) {
            shown += seconds;
            effects.update(seconds);
            if (shown > 2 && (controls.wasPressed(Action.OK) || mouse.wasPressed(MouseEvent.BUTTON1))) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.won(g, width(), run, shown);
        }

        Run run() {
            return run;
        }
    }
}
