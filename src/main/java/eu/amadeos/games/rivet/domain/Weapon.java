package eu.amadeos.games.rivet.domain;

/**
 * The engineer's tools, slots 1 to 6. A hitscan tool hits at once along a line; a projectile flies; a
 * beam hits again and again while the trigger is held; the charge is thrown and explodes after a moment.
 */
public enum Weapon {
    //            slot ammo           per  pause  damage pellets spread range
    ICE_PICK(1, null, 0, 0.45, 25, 1, 0, 1.3, Kind.MELEE),
    RIVET_GUN(2, Ammo.RIVETS, 1, 0.3, 14, 1, 0.03, 40, Kind.HITSCAN),
    SCATTER_RIVETER(3, Ammo.RIVETS, 5, 0.85, 11, 7, 0.1, 22, Kind.HITSCAN),
    ARC_WELDER(4, Ammo.CELLS, 1, 0.08, 7, 1, 0, 3.6, Kind.BEAM),
    PLASMA_CUTTER(5, Ammo.GAS, 1, 0.5, 45, 1, 0, 60, Kind.PROJECTILE),
    BLASTING_CHARGE(6, Ammo.CHARGES, 1, 0.9, 150, 1, 0, 0, Kind.THROWN);

    public enum Kind { MELEE, HITSCAN, BEAM, PROJECTILE, THROWN }

    private final int slot;
    private final Ammo ammo;
    private final int perShot;
    private final double pause;
    private final double damage;
    private final int pellets;
    private final double spread;
    private final double range;
    private final Kind kind;

    Weapon(int slot, Ammo ammo, int perShot, double pause, double damage, int pellets, double spread, double range, Kind kind) {
        this.slot = slot;
        this.ammo = ammo;
        this.perShot = perShot;
        this.pause = pause;
        this.damage = damage;
        this.pellets = pellets;
        this.spread = spread;
        this.range = range;
        this.kind = kind;
    }

    public int slot() {
        return slot;
    }

    /** What it uses; {@code null} for the ice pick, which needs nothing. */
    public Ammo ammo() {
        return ammo;
    }

    public int perShot() {
        return perShot;
    }

    /** Seconds between two shots. */
    public double pause() {
        return pause;
    }

    /** Damage of one pellet, one beam tick or the explosion's centre. */
    public double damage() {
        return damage;
    }

    public int pellets() {
        return pellets;
    }

    /** The largest angle a pellet leaves the line of aim, in radians. */
    public double spread() {
        return spread;
    }

    public double range() {
        return range;
    }

    public Kind kind() {
        return kind;
    }

    public String key() {
        return "weapon." + name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    public static Weapon inSlot(int slot) {
        for (Weapon w : values()) if (w.slot == slot) return w;
        return null;
    }
}
