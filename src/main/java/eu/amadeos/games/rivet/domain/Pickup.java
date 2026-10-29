package eu.amadeos.games.rivet.domain;

/** Something to pick up, lying on the floor. */
public final class Pickup {
    final PickupKind kind;
    final double x;
    final double y;
    boolean taken;

    Pickup(PickupKind kind, double x, double y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
    }

    public PickupKind kind() {
        return kind;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public boolean taken() {
        return taken;
    }
}
