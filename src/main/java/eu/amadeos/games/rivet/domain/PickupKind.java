package eu.amadeos.games.rivet.domain;

/** What lies around in the station; the letter is its mark in a level's map. */
public enum PickupKind {
    PATCH('h'), MEDKIT('m'), PLATING('n'), HEAVY_PLATING('u'),
    RIVETS('r'), CELLS('b'), GAS('q'), CHARGES('k'),
    SCATTER_RIVETER('3'), ARC_WELDER('4'), PLASMA_CUTTER('5'), BLASTING_CHARGE('6'),
    CHIP_GREEN('G'), CHIP_AMBER('A'), CHIP_VIOLET('V');

    private final char mark;

    PickupKind(char mark) {
        this.mark = mark;
    }

    public char mark() {
        return mark;
    }

    public static PickupKind ofMark(char c) {
        for (PickupKind k : values()) if (k.mark == c) return k;
        return null;
    }

    public Chip chip() {
        return switch (this) {
            case CHIP_GREEN -> Chip.GREEN;
            case CHIP_AMBER -> Chip.AMBER;
            case CHIP_VIOLET -> Chip.VIOLET;
            default -> null;
        };
    }

    public Weapon weapon() {
        return switch (this) {
            case SCATTER_RIVETER -> Weapon.SCATTER_RIVETER;
            case ARC_WELDER -> Weapon.ARC_WELDER;
            case PLASMA_CUTTER -> Weapon.PLASMA_CUTTER;
            case BLASTING_CHARGE -> Weapon.BLASTING_CHARGE;
            default -> null;
        };
    }

    public String key() {
        return "pickup." + name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
