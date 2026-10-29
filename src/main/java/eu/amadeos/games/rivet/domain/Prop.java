package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.raycast.Hitscan;

/**
 * Furniture of the station that stands in the way: a crate, a nitrogen canister (which bursts into a
 * freezing cloud when it is hit hard enough), or the scrap of a destroyed machine (which does not block).
 */
public final class Prop implements Hitscan.Target {
    public enum Kind { CRATE, CANISTER, SCRAP, BIG_SCRAP }

    final Kind kind;
    final double x;
    final double y;
    double health = 20;
    boolean gone;

    Prop(Kind kind, double x, double y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double radius() {
        return kind == Kind.CRATE ? 0.4 : 0.3;
    }

    public boolean blocks() {
        return !gone && (kind == Kind.CRATE || kind == Kind.CANISTER);
    }

    public boolean gone() {
        return gone;
    }
}
