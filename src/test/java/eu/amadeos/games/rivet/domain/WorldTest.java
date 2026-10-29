package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.raycast.Door;
import org.junit.jupiter.api.Test;

import static eu.amadeos.games.rivet.domain.Levels.fire;
import static eu.amadeos.games.rivet.domain.Levels.run;
import static eu.amadeos.games.rivet.domain.Levels.use;
import static eu.amadeos.games.rivet.domain.Levels.walk;
import static eu.amadeos.games.rivet.domain.Levels.world;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTest {
    private static boolean happened(World w, Event.Type type) {
        return w.events().stream().anyMatch(e -> e.type() == type);
    }

    @Test
    void theStartPutsThePlayerInTheMiddleOfItsCell() {
        World w = world("RRRRR", "R.@.R", "RRRRR");
        assertEquals(2.5, w.player().x(), 1e-9);
        assertEquals(1.5, w.player().y(), 1e-9);
    }

    @Test
    void walkingStopsAtTheWall() {
        World w = world("RRRRRR", "R@...R", "RRRRRR");
        run(w, 3, walk(1));
        assertEquals(5 - Player.RADIUS, w.player().x(), 0.05);
    }

    @Test
    void rivetsDestroyACrawler() {
        World w = world("RRRRRRRRRR", "R@.....c.R", "RRRRRRRRRR");
        Machine crawler = w.machines().getFirst();
        int before = w.player().ammo(Ammo.RIVETS);
        boolean destroyed = false;
        for (int i = 0; i < 600 && !destroyed; i++) {
            w.update(1.0 / 120, fire());
            destroyed |= happened(w, Event.Type.MACHINE_DESTROYED);
        }
        assertTrue(destroyed);
        assertFalse(crawler.alive());
        assertTrue(w.player().ammo(Ammo.RIVETS) < before);
        assertEquals(1, w.kills());
        assertEquals(MachineKind.CRAWLER.points(), w.points());
    }

    @Test
    void aCrawlerThatHearsAShotComesAndBites() {
        World w = world("RRRRRRRRRR", "R@......cR", "RRRRRRRRRR");
        run(w, 1.6, Input.NONE);
        run(w, 0.05, fire());
        run(w, 6, Input.NONE);
        assertTrue(w.player().health() < Player.MAX_HEALTH);
        assertTrue(w.machines().getFirst().state() != Machine.State.IDLE);
    }

    // --8<-- [start:test-wake]
    @Test
    void machinesTakeAMomentToNoticeThePlayerWhenTheLevelStarts() {
        World w = world("RRRRRRRR", "R@.c...R", "RRRRRRRR");
        run(w, 1.2, Input.NONE);
        assertEquals(Machine.State.IDLE, w.machines().getFirst().state());
        run(w, 1.0, Input.NONE);
        // two cells away it notices the player whichever way it faces
        assertEquals(Machine.State.CHASE, w.machines().getFirst().state());
    }
    // --8<-- [end:test-wake]

    @Test
    void aMachineBehindAWallStaysIdle() {
        World w = world("RRRRRRR", "R@.R.cR", "RRRRRRR");
        run(w, 2, Input.NONE);
        assertEquals(Machine.State.IDLE, w.machines().getFirst().state());
    }

    @Test
    void platingTakesAThirdOfAHit() {
        Player p = new Player();
        p.plating = 30;
        p.damage(30);
        assertEquals(20, p.plating(), 1e-9);
        assertEquals(80, p.health());
    }

    @Test
    void aDoorOpensWhenUsed() {
        World w = world("RRRRRRR", "R@|...R", "RRRRRRR");
        Door door = w.map().door(2, 1);
        w.update(1.0 / 120, use());
        assertTrue(happened(w, Event.Type.DOOR_OPENS));
        run(w, 1, Input.NONE);
        assertTrue(door.passable());
        run(w, 2, walk(1));
        assertTrue(w.player().x() > 3);
    }

    @Test
    void aLockedDoorNeedsItsChip() {
        World w = world("RRRRRRRR", "R@g.G..R", "RRRRRRRR");
        w.update(1.0 / 120, use());
        assertTrue(happened(w, Event.Type.LOCKED));
        assertEquals(Door.State.CLOSED, w.map().door(2, 1).state());

        // the same door with the chip already in the pocket: the chip lies before the door here
        World w2 = world("RRRRRRRR", "RG@g...R", "RRRRRRRR");
        w2.update(1.0 / 120, new Input(0, 0, Math.PI, false, false, false, 0, 0));
        run(w2, 0.5, walk(1));
        assertTrue(w2.player().chips().contains(Chip.GREEN));
        w2.update(1.0 / 120, new Input(0, 0, Math.PI, false, false, false, 0, 0));
        run(w2, 0.3, walk(1));
        w2.update(1.0 / 120, use());
        assertTrue(happened(w2, Event.Type.DOOR_OPENS));
    }

    @Test
    void aSecretDoorCountsOnce() {
        World w = world("RRRRRR", "R@sh.R", "RRRRRR");
        assertEquals(1, w.secretsTotal());
        w.update(1.0 / 120, use());
        assertTrue(happened(w, Event.Type.SECRET_FOUND));
        assertEquals(1, w.secretsFound());
        assertEquals(50, w.points());
    }

    @Test
    void pickupsAreTakenOnlyWhenTheyHelp() {
        World w = world("RRRRRRR", "R@h.n.R", "RRRRRRR");
        run(w, 1.2, walk(1));
        assertFalse(w.pickups().get(0).taken(), "full health: the patch stays");
        assertTrue(w.pickups().get(1).taken());
        assertEquals(25, w.player().plating());
    }

    @Test
    void aNewToolIsTakenInHand() {
        World w = world("RRRRR", "R@3.R", "RRRRR");
        run(w, 0.5, walk(1));
        assertEquals(Weapon.SCATTER_RIVETER, w.player().weapon());
        assertEquals(70, w.player().ammo(Ammo.RIVETS));
    }

    @Test
    void theLiftEndsTheLevel() {
        World w = world("RRRR", "R.@X", "RRRR");
        w.update(1.0 / 120, use());
        assertTrue(w.done());
        assertTrue(happened(w, Event.Type.LEVEL_DONE));
    }

    @Test
    void aCanisterBurstsAndHurtsWhatIsNear() {
        World w = world("RRRRRRRRR", "R@....ycR", "RRRRRRRRR");
        Machine crawler = w.machines().getFirst();
        double health = crawler.health();
        for (int i = 0; i < 600 && !happened(w, Event.Type.CANISTER_BURSTS); i++) w.update(1.0 / 120, fire());
        assertTrue(crawler.health() < health);
    }

    @Test
    void anEmptyToolSwitchesToOneThatWorks() {
        Player p = new Player();
        p.ammo.put(Ammo.RIVETS, 0);
        World w = world(p, "RRRRR", "R@..R", "RRRRR");
        run(w, 0.4, Input.NONE);
        w.update(1.0 / 120, fire());
        assertTrue(happened(w, Event.Type.EMPTY));
        assertEquals(Weapon.ICE_PICK, w.player().weapon());
    }

    @Test
    void theForemanLeavesTheVioletChip() {
        World w = world("RRRRRRRRR", "R@....f.R", "RRRRRRRRR");
        Machine foreman = w.machines().getFirst();
        foreman.health = 1;
        for (int i = 0; i < 120 && foreman.alive(); i++) w.update(1.0 / 120, fire());
        assertFalse(foreman.alive());
        assertTrue(w.pickups().stream().anyMatch(p -> p.kind() == PickupKind.CHIP_VIOLET));
    }
}
