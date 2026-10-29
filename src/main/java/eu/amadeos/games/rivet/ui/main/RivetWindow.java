package eu.amadeos.games.rivet.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.swing.FullScreen;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.HighScoreDialog;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.rivet.bootstrap.AmadeRivetApplication;
import eu.amadeos.games.rivet.repository.RivetSettings;
import eu.amadeos.games.rivet.ui.game.RivetGame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;

/**
 * The game window: a menu and the view, a {@link GameCanvas} the game loop draws on. The window can be
 * resized or switched to full screen with F11; the view scales with it and widens to 16:9 on a wide
 * screen. While a level is played, the mouse turns the view and its pointer is hidden.
 */
public final class RivetWindow extends JFrame implements RivetView {
    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final HighScores scores;
    private final PlayerName playerName;
    private final GameCanvas canvas;
    private final FullScreen fullScreen;
    private final JCheckBoxMenuItem sound;
    private final ButtonGroup mouseGroup = new ButtonGroup();
    private final JMenu mouseSpeed;
    private JMenu graphics;
    private RivetPresenter presenter;

    @Inject
    public RivetWindow(Messages msg, HighScores scores, PlayerName playerName, RivetGame game, Keyboard keyboard, Mouse mouse,
                       MouseLook look) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.scores = scores;
        this.playerName = playerName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        java.net.URL icon = getClass().getResource("/icons/amade-rivet-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        // --8<-- [start:canvas]
        // The canvas is a heavyweight AWT component: lightweight menus would open behind it.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        // Two layers: the 3D view with the tool in the hand, and the suit's display over it at full resolution.
        canvas = new GameCanvas(640, 400, game::render, game::renderOverlay);
        canvas.setMaxLogicalWidth(712);
        // Tab shows the map: it must not move the focus away from the canvas.
        canvas.setFocusTraversalKeysEnabled(false);
        keyboard.attach(canvas);
        mouse.attach(canvas);
        look.attach(canvas);
        // --8<-- [end:canvas]
        getContentPane().add(canvas, BorderLayout.CENTER);
        fullScreen = new FullScreen(this, canvas);
        sound = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.game.sound"));
        sound.setName("game-sound");
        sound.addActionListener(e -> {
            presenter.setSound(sound.isSelected());
            canvas.requestFocusInWindow();
        });
        mouseSpeed = Mnemonics.apply(new JMenu(), msg.get("menu.game.mouse"));
        mouseSpeed.setName("game-mouse");
        for (int speed = RivetSettings.MOUSE_SLOWEST; speed <= RivetSettings.MOUSE_FASTEST; speed++) {
            int chosen = speed;
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(speed == 5 ? msg.format("menu.game.mouse.standard", speed)
                    : Integer.toString(speed));
            item.setName("game-mouse-" + speed);
            item.addActionListener(e -> {
                presenter.setMouseSpeed(chosen);
                canvas.requestFocusInWindow();
            });
            mouseGroup.add(item);
            mouseSpeed.add(item);
        }
        setJMenuBar(menuBar());
        // Another program took the focus: pause, and give the mouse back. Not for the moment F11 takes it.
        fullScreen.whenDeactivated(() -> {
            if (presenter != null) presenter.focusLost();
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                canvas.requestFocusInWindow();
            }
        });
        pack();
    }

    /** Connects the window with the presenter; call once before {@link RivetPresenter#start()}. */
    public void bind(RivetPresenter presenter) {
        this.presenter = presenter;
    }

    public GameCanvas canvas() {
        return canvas;
    }

    public FullScreen fullScreen() {
        return fullScreen;
    }

    /** Runs the action when the window closes, but not when it only switches to or from full screen. */
    public void whenClosed(Runnable action) {
        fullScreen.whenClosed(action);
    }

    // --8<-- [start:menu]
    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(focusing(menus.newGame(() -> presenter.newGame())));
        // No shortcut: P and Esc are the game's own pause keys.
        JMenuItem pause = Mnemonics.apply(new JMenuItem(), msg.get("menu.game.pause"));
        pause.setName("game-pause");
        pause.addActionListener(e -> presenter.pause());
        game.add(focusing(pause));
        game.addSeparator();
        game.add(sound);
        game.add(mouseSpeed);
        graphics = menus.graphics(RenderMode.PIXEL, mode -> {
            presenter.setRenderMode(mode);
            canvas.requestFocusInWindow();
        });
        game.add(graphics);
        game.add(focusing(menus.frameTimes(canvas::setShowFrameTimes)));
        game.add(focusing(menus.fullScreen(fullScreen::toggle)));
        game.addSeparator();
        game.add(menus.highScores(() -> presenter.showScores()));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), () -> presenter.showAbout()));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }
    // --8<-- [end:menu]

    /** After a menu command the keys belong to the game again. */
    private <T extends JMenuItem> T focusing(T item) {
        item.addActionListener(e -> canvas.requestFocusInWindow());
        return item;
    }

    // ---- RivetView ----------------------------------------------------------------------------------

    @Override
    public void showOptions(boolean soundOn, RenderMode mode, int speed) {
        sound.setSelected(soundOn);
        for (int i = 0; i < graphics.getItemCount(); i++) {
            JMenuItem item = graphics.getItem(i);
            item.setSelected(("game-graphics-" + mode.id()).equals(item.getName()));
        }
        for (int i = 0; i < mouseSpeed.getItemCount(); i++) {
            JMenuItem item = mouseSpeed.getItem(i);
            item.setSelected(("game-mouse-" + speed).equals(item.getName()));
        }
    }

    @Override
    public void useRenderMode(RenderMode mode) {
        canvas.setRenderMode(mode);
    }

    @Override
    public Optional<String> askName(String message) {
        Optional<String> name = HighScoreDialog.askName(this, message, playerName);
        canvas.requestFocusInWindow();
        return name;
    }

    @Override
    public void showScores(int highlight) {
        HighScoreDialog.show(this, scores,
                List.of(new HighScoreDialog.Table(RivetPresenter.TABLE, msg.get("scores.table"))),
                points -> Long.toString(points), RivetPresenter.TABLE, highlight);
        canvas.requestFocusInWindow();
    }

    @Override
    public void showAbout(String gamepads) {
        // Swing's HTML only wraps lines with a fixed body width.
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: 340px'><center><h2>"
                        + msg.get("window.title") + " " + AmadeRivetApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.keys") + "</p><br>"
                        + "<p>" + gamepads + "</p><br>"
                        + "<small>amade-rivet-game &bull; &copy; 2026 datazuul.com &bull; EUPL-1.2</small>"
                        + "</center></html>",
                Mnemonics.strip(menus.texts().format("menu.help.about", msg.get("window.title"))),
                JOptionPane.INFORMATION_MESSAGE);
        canvas.requestFocusInWindow();
    }
}
