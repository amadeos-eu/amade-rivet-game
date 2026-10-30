package eu.amadeos.games.rivet.domain;

/** An access chip: it opens the doors of its colour, in the level where it was found. */
public enum Chip {
    GREEN, AMBER, VIOLET;

    public String key() {
        return "chip." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
