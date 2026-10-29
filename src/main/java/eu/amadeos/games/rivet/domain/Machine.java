package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.raycast.Hitscan;

/** One of the Foreman's machines in a level. */
public final class Machine implements Hitscan.Target {
    public enum State { IDLE, CHASE, WINDUP, PAIN, DEAD }

    final MachineKind kind;
    double x;
    double y;
    double facing;
    double health;
    State state = State.IDLE;
    double stateTime;
    double cooldown;
    /** 1 right after a hit, fading. */
    double flash;
    /** Seconds the machine has been moving, for the walk animation. */
    double moving;
    /** Shots of a sentry's burst still to fire. */
    int burst;
    double spawnTimer;

    Machine(MachineKind kind, double x, double y, double facing, double health) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.facing = facing;
        this.health = health;
        this.cooldown = 0.5;
        this.spawnTimer = 8;
    }

    void to(State state) {
        this.state = state;
        stateTime = 0;
    }

    public MachineKind kind() {
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
        return kind.radius();
    }

    public double facing() {
        return facing;
    }

    public double health() {
        return health;
    }

    public State state() {
        return state;
    }

    public double stateTime() {
        return stateTime;
    }

    public boolean alive() {
        return state != State.DEAD;
    }

    public double flash() {
        return flash;
    }

    public double moving() {
        return moving;
    }
}
