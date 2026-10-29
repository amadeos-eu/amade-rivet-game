package eu.amadeos.games.rivet.domain;

/** Something flying: a spark bolt, a plasma blob, an ice block, a thrown charge, a bolt of the Foreman. */
public final class Projectile {
    public enum Kind { SPARK, PLASMA, ICE_BLOCK, CHARGE, BOLT }

    final Kind kind;
    double x;
    double y;
    double z;
    double vx;
    double vy;
    final boolean fromPlayer;
    final double damage;
    final double splash;
    double life;
    boolean gone;

    Projectile(Kind kind, double x, double y, double z, double vx, double vy, boolean fromPlayer, double damage, double splash, double life) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.fromPlayer = fromPlayer;
        this.damage = damage;
        this.splash = splash;
        this.life = life;
    }

    public Kind kind() {
        return kind;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Height above the floor. */
    public double z() {
        return z;
    }

    public double angle() {
        return Math.atan2(vy, vx);
    }

    public boolean fromPlayer() {
        return fromPlayer;
    }
}
