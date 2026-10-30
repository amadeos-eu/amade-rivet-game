package eu.amadeos.games.rivet.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Run;

import javax.swing.KeyStroke;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The settings in {@code ~/.config/amadeos/rivet/rivet.properties}: the graphics mode, the sound, how fast
 * the mouse turns, the last run's difficulty and section, the furthest section reached, and the keys. Keys
 * are written as Java names them, several separated by spaces, e.g. {@code keys.fire=CONTROL F}.
 */
public final class RivetSettings {
    /** The steps of the mouse speed; 5 is the standard. */
    public static final int MOUSE_SLOWEST = 1, MOUSE_FASTEST = 10;
    private static final String GRAPHICS = "graphics";
    private static final String SOUND = "sound";
    private static final String MOUSE = "mouse.speed";
    private static final String DIFFICULTY = "run.difficulty";
    private static final String SECTION = "run.section";
    private static final String REACHED = "reached";

    private final Path file;

    @Inject
    public RivetSettings(AppDirectories directories) {
        this(directories.configFile("rivet.properties"));
    }

    public RivetSettings(Path file) {
        this.file = file;
    }

    public RenderMode renderMode() {
        return RenderMode.ofId(PropertiesFile.load(file).getProperty(GRAPHICS, "")).orElse(RenderMode.PIXEL);
    }

    public void setRenderMode(RenderMode mode) {
        set(GRAPHICS, mode.id());
    }

    public boolean sound() {
        return !"off".equals(PropertiesFile.load(file).getProperty(SOUND, "on"));
    }

    public void setSound(boolean on) {
        set(SOUND, on ? "on" : "off");
    }

    public int mouseSpeed() {
        return Math.max(MOUSE_SLOWEST, Math.min(MOUSE_FASTEST, number(MOUSE, 5)));
    }

    public void setMouseSpeed(int speed) {
        set(MOUSE, Integer.toString(Math.max(MOUSE_SLOWEST, Math.min(MOUSE_FASTEST, speed))));
    }

    public Difficulty difficulty() {
        try {
            return Difficulty.valueOf(PropertiesFile.load(file).getProperty(DIFFICULTY, "normal").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Difficulty.NORMAL;
        }
    }

    public int section() {
        return Math.max(1, Math.min(reached(), number(SECTION, 1)));
    }

    /** The choices of the last run. */
    public void setRun(Difficulty difficulty, int section) {
        Properties p = PropertiesFile.load(file);
        p.setProperty(DIFFICULTY, difficulty.name().toLowerCase(Locale.ROOT));
        p.setProperty(SECTION, Integer.toString(section));
        PropertiesFile.store(file, p, "amade-rivet-game settings");
    }

    /** The furthest section the player has entered, 1 to 3: a run may start at any section up to it. */
    public int reached() {
        return Math.max(1, Math.min(Run.SECTIONS, number(REACHED, 1)));
    }

    public void reach(int section) {
        if (section > reached()) set(REACHED, Integer.toString(Math.min(Run.SECTIONS, section)));
    }

    private int number(String key, int fallback) {
        try {
            return Integer.parseInt(PropertiesFile.load(file).getProperty(key, Integer.toString(fallback)).strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void set(String key, String value) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(key, value);
        PropertiesFile.store(file, properties, "amade-rivet-game settings");
    }

    /**
     * The keys of an action, e.g. {@code keys("fire", KeyEvent.VK_CONTROL)}: those in the file if it names any it
     * understands, else the defaults.
     */
    public int[] keys(String action, int... defaults) {
        String names = PropertiesFile.load(file).getProperty("keys." + action, "").strip();
        List<Integer> codes = new ArrayList<>();
        for (String name : names.split("\\s+")) {
            if (name.isEmpty()) continue;
            KeyStroke stroke = KeyStroke.getKeyStroke(name.toUpperCase(Locale.ROOT));
            if (stroke != null && stroke.getKeyCode() != 0) codes.add(stroke.getKeyCode());
        }
        return codes.isEmpty() ? defaults : codes.stream().mapToInt(Integer::intValue).toArray();
    }
}
