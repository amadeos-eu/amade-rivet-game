package eu.amadeos.games.rivet.domain;

/** The machines the Foreman sends: how strong, fast and big each is, and how it attacks. */
public enum MachineKind {
    //           health speed radius lift  range  damage pause  sight points
    DRONE(40, 1.9, 0.3, 0.18, 9, 8, 1.6, 16, 50, Attack.SPARK),
    CRAWLER(60, 2.6, 0.35, 0, 0.95, 12, 0.8, 14, 60, Attack.BITE),
    SENTRY(90, 0, 0.35, 0, 14, 5, 2.2, 18, 80, Attack.BURST),
    LOADER(320, 1.1, 0.55, 0, 12, 25, 2.6, 16, 250, Attack.ICE_BLOCK),
    FOREMAN(2400, 0, 0.9, 0, 30, 12, 2.0, 40, 2000, Attack.FAN);

    public enum Attack { SPARK, BITE, BURST, ICE_BLOCK, FAN }

    private final double health;
    private final double speed;
    private final double radius;
    private final double lift;
    private final double range;
    private final double damage;
    private final double pause;
    private final double sight;
    private final int points;
    private final Attack attack;

    MachineKind(double health, double speed, double radius, double lift, double range, double damage, double pause,
                double sight, int points, Attack attack) {
        this.health = health;
        this.speed = speed;
        this.radius = radius;
        this.lift = lift;
        this.range = range;
        this.damage = damage;
        this.pause = pause;
        this.sight = sight;
        this.points = points;
        this.attack = attack;
    }

    public double health() {
        return health;
    }

    /** Units per second; 0 for machines fixed in place. */
    public double speed() {
        return speed;
    }

    public double radius() {
        return radius;
    }

    /** How high above the floor it floats. */
    public double lift() {
        return lift;
    }

    /** From how far it attacks. */
    public double range() {
        return range;
    }

    public double damage() {
        return damage;
    }

    /** Seconds between two attacks. */
    public double pause() {
        return pause;
    }

    /** How far it sees. */
    public double sight() {
        return sight;
    }

    public int points() {
        return points;
    }

    public Attack attack() {
        return attack;
    }

    public String key() {
        return "machine." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
