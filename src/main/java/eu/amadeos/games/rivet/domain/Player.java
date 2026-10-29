package eu.amadeos.games.rivet.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The engineer: where they stand and look, their health and plating, tools, ammunition and chips. */
public final class Player {
    public static final double RADIUS = 0.25;
    public static final int MAX_HEALTH = 100;
    public static final int MAX_PLATING = 100;

    double x;
    double y;
    double angle;
    double health = MAX_HEALTH;
    double plating;
    final EnumMap<Ammo, Integer> ammo = new EnumMap<>(Ammo.class);
    final EnumSet<Weapon> owned = EnumSet.of(Weapon.ICE_PICK, Weapon.RIVET_GUN);
    final EnumSet<Chip> chips = EnumSet.noneOf(Chip.class);
    Weapon weapon = Weapon.RIVET_GUN;
    double cooldown;
    /** Seconds since the last shot, for the recoil of the tool in the picture. */
    double sinceShot = 10;
    /** 1 right after a hit, fading to 0: the red flash. */
    double hurt;
    /** 1 right after a pickup, fading: the bright flash. */
    double glint;
    /** How far the player has walked, for the bob of the eye and the tool. */
    double walked;
    boolean firing;

    public Player() {
        for (Ammo a : Ammo.values()) ammo.put(a, 0);
        ammo.put(Ammo.RIVETS, 50);
    }

    /** A copy to restart a level as it was entered. */
    public Player copy() {
        Player p = new Player();
        p.health = health;
        p.plating = plating;
        p.ammo.putAll(ammo);
        p.owned.clear();
        p.owned.addAll(owned);
        p.weapon = weapon;
        return p;
    }

    /** The standard outfit for starting at a later section. */
    public static Player outfitFor(int section) {
        Player p = new Player();
        if (section >= 2) {
            p.owned.add(Weapon.SCATTER_RIVETER);
            p.owned.add(Weapon.ARC_WELDER);
            p.ammo.put(Ammo.RIVETS, 120);
            p.ammo.put(Ammo.CELLS, 100);
            p.plating = 50;
        }
        if (section >= 3) {
            p.owned.add(Weapon.PLASMA_CUTTER);
            p.owned.add(Weapon.BLASTING_CHARGE);
            p.ammo.put(Ammo.GAS, 20);
            p.ammo.put(Ammo.CHARGES, 5);
            p.plating = 100;
        }
        return p;
    }

    /** Takes a hit: plating takes a third of it while it lasts. */
    void damage(double amount) {
        if (amount <= 0 || health <= 0) return;
        double absorbed = Math.min(plating, amount / 3);
        plating -= absorbed;
        health = Math.max(0, health - (amount - absorbed));
        hurt = Math.min(1, hurt + 0.4 + amount / 40);
    }

    /** Adds ammunition up to the maximum; returns true if any fitted. */
    boolean add(Ammo a, int amount) {
        int now = ammo.get(a);
        if (now >= a.max()) return false;
        ammo.put(a, Math.min(a.max(), now + amount));
        return true;
    }

    boolean canFire(Weapon w) {
        return w.ammo() == null || ammo.get(w.ammo()) >= w.perShot();
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double angle() {
        return angle;
    }

    public int health() {
        return (int) Math.ceil(health);
    }

    public int plating() {
        return (int) Math.ceil(plating);
    }

    public boolean alive() {
        return health > 0;
    }

    public int ammo(Ammo a) {
        return ammo.get(a);
    }

    public Map<Ammo, Integer> ammo() {
        return Map.copyOf(ammo);
    }

    public Set<Weapon> owned() {
        return EnumSet.copyOf(owned);
    }

    public boolean owns(Weapon w) {
        return owned.contains(w);
    }

    public Set<Chip> chips() {
        return chips.isEmpty() ? EnumSet.noneOf(Chip.class) : EnumSet.copyOf(chips);
    }

    public Weapon weapon() {
        return weapon;
    }

    public double sinceShot() {
        return sinceShot;
    }

    public double hurt() {
        return hurt;
    }

    public double glint() {
        return glint;
    }

    public double walked() {
        return walked;
    }

    /** True while the trigger is held and the tool is working (the arc welder's beam). */
    public boolean firing() {
        return firing;
    }
}
