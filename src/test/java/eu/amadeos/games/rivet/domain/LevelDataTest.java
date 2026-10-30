package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.data.TextData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelDataTest {
    @Test
    void readsTheHeaderAndTheMap() {
        LevelData l = LevelData.parse("2-1", TextData.parse("t.txt", new StringReader("""
                name Ice Shaft
                start 90
                par 200
                fog 101c26 0.07
                ambient 0.1 0.2 0.3
                floor ice
                light 1.5 1.5 1 0.5 0.2 4
                map
                RRRR
                R@.X
                RRRR
                end
                """)));
        assertEquals("Ice Shaft", l.name());
        assertEquals(2, l.section());
        assertEquals(Math.PI / 2, l.startAngle(), 1e-9);
        assertEquals(0x101c26, l.fog());
        assertEquals("ice", l.floor());
        assertEquals(1, l.lights().size());
        assertEquals(4, l.width());
        assertEquals('X', l.at(3, 1));
    }

    @Test
    void rowsOfDifferentLengthAreAnError() {
        TextData.DataException e = assertThrows(TextData.DataException.class, () -> LevelData.parse("1-1",
                TextData.parse("t.txt", new StringReader("map\nRRR\nR@.R\nend\n"))));
        assertTrue(e.getMessage().startsWith("t.txt:3:"), e.getMessage());
    }

    @Test
    void everyLevelLoadsAndCanBeFinished() {
        for (String id : Run.LEVELS) {
            LevelData l = LevelData.load(id);
            assertTrue(reachable(l), id + " cannot be finished");
        }
    }

    @Test
    void everyLevelHasOneStartOneLiftAndASecret() {
        for (String id : Run.LEVELS) {
            LevelData l = LevelData.load(id);
            int starts = 0, lifts = 0, secrets = 0;
            for (int y = 0; y < l.height(); y++) for (int x = 0; x < l.width(); x++) {
                char c = l.at(x, y);
                if (c == '@') starts++;
                if (c == 'X') lifts++;
                if (c == 's') secrets++;
                if (x == 0 || y == 0 || x == l.width() - 1 || y == l.height() - 1) {
                    assertTrue(LevelData.wall(c), id + ": the edge must be wall at " + x + "," + y);
                }
            }
            assertEquals(1, starts, id);
            assertEquals(1, lifts, id);
            assertTrue(secrets >= 1, id + " has no secret");
        }
    }

    @Test
    void everythingOnTheMapIsKnown() {
        String known = LevelData.WALLS + ".,;:_-|gavs@dctlfxy";
        for (String id : Run.LEVELS) {
            LevelData l = LevelData.load(id);
            for (int y = 0; y < l.height(); y++) for (int x = 0; x < l.width(); x++) {
                char c = l.at(x, y);
                assertTrue(known.indexOf(c) >= 0 || PickupKind.ofMark(c) != null, id + ": unknown '" + c + "' at " + x + "," + y);
            }
        }
    }

    /**
     * Walks from the start through every open cell and door it can open, picking up the chips it passes,
     * until it stands next to the lift's switch. Crates and canisters block. The Foreman's level counts its
     * violet chip as found once the Foreman can be reached.
     */
    static boolean reachable(LevelData l) {
        Set<Chip> chips = EnumSet.noneOf(Chip.class);
        while (true) {
            Set<Long> seen = new HashSet<>();
            Deque<int[]> queue = new ArrayDeque<>();
            for (int y = 0; y < l.height(); y++) for (int x = 0; x < l.width(); x++) {
                if (l.at(x, y) == '@') { queue.add(new int[]{x, y}); seen.add(key(x, y)); }
            }
            boolean lift = false;
            Set<Chip> before = EnumSet.copyOf(chips.isEmpty() ? EnumSet.noneOf(Chip.class) : chips);
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                char here = l.at(p[0], p[1]);
                PickupKind kind = PickupKind.ofMark(here);
                if (kind != null && kind.chip() != null) chips.add(kind.chip());
                if (here == 'f') chips.add(Chip.VIOLET);
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int x = p[0] + d[0], y = p[1] + d[1];
                    char c = l.at(x, y);
                    if (c == 'X') lift = true;
                    if (LevelData.wall(c) || c == 'x' || c == 'y' || !seen.add(key(x, y))) continue;
                    Chip lock = c == 'g' ? Chip.GREEN : c == 'a' ? Chip.AMBER : c == 'v' ? Chip.VIOLET : null;
                    if (lock != null && !chips.contains(lock)) { seen.remove(key(x, y)); continue; }
                    queue.add(new int[]{x, y});
                }
            }
            if (lift) return true;
            if (chips.equals(before)) return false;
        }
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | y;
    }

    @Test
    void theReachTestNoticesALockedWay() {
        assertFalse(reachable(Levels.of("RRRRRR", "R@.gXR", "RRRRRR")));
        assertTrue(reachable(Levels.of("RRRRRRR", "R@G.g.X", "RRRRRRR")));
    }
}
