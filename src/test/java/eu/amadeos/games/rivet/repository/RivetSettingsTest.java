package eu.amadeos.games.rivet.repository;

import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.rivet.domain.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivetSettingsTest {
    @TempDir
    Path dir;

    @Test
    void withoutAFileTheStandardsApply() {
        RivetSettings s = new RivetSettings(dir.resolve("rivet.properties"));
        assertEquals(RenderMode.PIXEL, s.renderMode());
        assertTrue(s.sound());
        assertEquals(5, s.mouseSpeed());
        assertEquals(Difficulty.NORMAL, s.difficulty());
        assertEquals(1, s.section());
        assertEquals(1, s.reached());
    }

    @Test
    void choicesAreKept() {
        Path file = dir.resolve("rivet.properties");
        RivetSettings s = new RivetSettings(file);
        s.setSound(false);
        s.setMouseSpeed(12);
        s.reach(2);
        s.setRun(Difficulty.HARD, 2);
        RivetSettings again = new RivetSettings(file);
        assertFalse(again.sound());
        assertEquals(10, again.mouseSpeed(), "at most the fastest");
        assertEquals(Difficulty.HARD, again.difficulty());
        assertEquals(2, again.section());
        again.reach(1);
        assertEquals(2, again.reached(), "reached sections are not forgotten");
    }

    @Test
    void aSectionNotReachedIsNotOffered() throws Exception {
        Path file = dir.resolve("rivet.properties");
        Files.writeString(file, "run.section=3\nreached=2\nrun.difficulty=impossible\nmouse.speed=fast\n");
        RivetSettings s = new RivetSettings(file);
        assertEquals(2, s.section());
        assertEquals(Difficulty.NORMAL, s.difficulty());
        assertEquals(5, s.mouseSpeed());
    }

    @Test
    void keysCanBeChanged() throws Exception {
        Path file = dir.resolve("rivet.properties");
        Files.writeString(file, "keys.fire=F CONTROL\nkeys.use=NOSUCHKEY\n");
        RivetSettings s = new RivetSettings(file);
        assertArrayEquals(new int[]{KeyEvent.VK_F, KeyEvent.VK_CONTROL}, s.keys("fire", KeyEvent.VK_CONTROL));
        assertArrayEquals(new int[]{KeyEvent.VK_SPACE}, s.keys("use", KeyEvent.VK_SPACE));
    }
}
