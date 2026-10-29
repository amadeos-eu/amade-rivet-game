package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunTest {
    @Test
    void aRunGoesFromLevelToLevelAndKeepsTheTools() {
        Run run = new Run(Difficulty.NORMAL, 1);
        World first = run.start(Plain.SURFACES, new SeededRandom(1));
        assertEquals("Landing Pad", first.level().name());
        run.player().owned.add(Weapon.ARC_WELDER);
        run.levelDone(first);
        assertTrue(run.score() > 0, "the time bonus");
        World second = run.start(Plain.SURFACES, new SeededRandom(1));
        assertEquals("1-2", second.level().id());
        assertTrue(second.player().owns(Weapon.ARC_WELDER));
    }

    @Test
    void retryingALevelGivesBackWhatThePlayerHadThen() {
        Run run = new Run(Difficulty.NORMAL, 2);
        World w = run.start(Plain.SURFACES, new SeededRandom(1));
        assertEquals("2-1", w.level().id());
        w.player().damage(500);
        run.retry();
        assertEquals(Player.MAX_HEALTH, run.player().health());
        assertTrue(run.player().owns(Weapon.ARC_WELDER), "the outfit of section 2");
    }

    @Test
    void theRunEndsAfterTheLastLevel() {
        Run run = new Run(Difficulty.EASY, 3);
        for (int i = 0; i < 3; i++) run.levelDone(run.start(Plain.SURFACES, new SeededRandom(i)));
        assertTrue(run.finished());
    }
}
