package eu.amadeos.games.rivet.domain;

/**
 * What the player does in one step: walk forward (positive) or back, step sideways (positive to the right),
 * turn by an angle in radians (positive to the right), run, hold the trigger, use what is in front, and
 * choose a tool: by slot (1 to 6, 0 for none) or the next/previous one ({@code cycle} ±1).
 */
public record Input(double forward, double strafe, double turn, boolean run, boolean fire, boolean use, int slot, int cycle) {
    public static final Input NONE = new Input(0, 0, 0, false, false, false, 0, 0);
}
