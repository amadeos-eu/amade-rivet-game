package eu.amadeos.games.rivet.domain;

/** What the tools use up, and how much of each fits into the suit's pockets. */
public enum Ammo {
    RIVETS(200), CELLS(300), GAS(50), CHARGES(20);

    private final int max;

    Ammo(int max) {
        this.max = max;
    }

    public int max() {
        return max;
    }

    /** The key of the name in the texts: {@code ammo.rivets}. */
    public String key() {
        return "ammo." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
