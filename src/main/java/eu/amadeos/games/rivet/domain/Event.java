package eu.amadeos.games.rivet.domain;

/**
 * Something that happened in one step, for sounds, flashes and messages. {@code x, y} is where;
 * {@code detail} says more (the tool, the machine, the pickup, the chip).
 */
public record Event(Type type, double x, double y, Object detail) {
    public enum Type {
        SHOT, BEAM, MELEE_HIT, HIT_WALL, HIT_MACHINE, MACHINE_ALERTED, MACHINE_ATTACKS, MACHINE_DESTROYED,
        PLAYER_HURT, PLAYER_DIED, PICKUP, DOOR_OPENS, LOCKED, SECRET_FOUND, EXPLOSION, CANISTER_BURSTS,
        EMPTY, WEAPON_CHANGED, DRONES_LAUNCHED, LEVEL_DONE
    }

    static Event at(Type type, double x, double y) {
        return new Event(type, x, y, null);
    }
}
