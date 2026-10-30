package eu.amadeos.games.rivet.domain;

/** How hard the machines are: how much their hits hurt, how much they take, how quickly they attack again. */
public enum Difficulty {
    EASY(0.5, 0.8, 1.25), NORMAL(1, 1, 1), HARD(1.5, 1.25, 0.8);

    private final double damage;
    private final double strength;
    private final double pause;

    Difficulty(double damage, double strength, double pause) {
        this.damage = damage;
        this.strength = strength;
        this.pause = pause;
    }

    /** Factor on the damage machines do. */
    public double damage() {
        return damage;
    }

    /** Factor on the machines' health. */
    public double strength() {
        return strength;
    }

    /** Factor on the time between two attacks of a machine. */
    public double pause() {
        return pause;
    }

    public String key() {
        return "difficulty." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
