package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.random.SeededRandom;

import java.io.StringReader;

/** Small levels written in the tests. */
final class Levels {
    private Levels() {
    }

    static LevelData of(String... rows) {
        String text = "name Test\nmap\n" + String.join("\n", rows) + "\nend\n";
        return LevelData.parse("1-1", TextData.parse("test.txt", new StringReader(text)));
    }

    static World world(Player player, String... rows) {
        return new World(of(rows), Plain.SURFACES, Difficulty.NORMAL, player, new SeededRandom(7));
    }

    static World world(String... rows) {
        return world(new Player(), rows);
    }

    /** Lets the world run for a while with the same input. */
    static void run(World w, double seconds, Input in) {
        for (double t = 0; t < seconds; t += 1.0 / 120) w.update(1.0 / 120, in);
    }

    static Input fire() {
        return new Input(0, 0, 0, false, true, false, 0, 0);
    }

    static Input use() {
        return new Input(0, 0, 0, false, false, true, 0, 0);
    }

    static Input walk(double forward) {
        return new Input(forward, 0, 0, false, false, false, 0, 0);
    }
}
