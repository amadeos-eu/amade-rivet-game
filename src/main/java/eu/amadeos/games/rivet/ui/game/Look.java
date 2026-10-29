package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.games.rivet.domain.Chip;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/** The colours and letters of the overlay: cold steel panels with bright figures, as on a suit's display. */
final class Look {
    static final Color PANEL = new Color(0x0b1117, true);
    static final Color PANEL_FILL = new Color(11, 17, 23, 175);
    static final Color PANEL_EDGE = new Color(159, 195, 216, 60);
    static final Color TEXT = new Color(0xf2f6f8);
    static final Color DIM = new Color(0xa9b8c2);
    static final Color FAINT = new Color(0x56626c);
    static final Color ICE = new Color(0x9fd4ec);
    static final Color HEALTH = new Color(0x63e08f);
    static final Color PLATING = new Color(0x8fb8d8);
    static final Color SELECTED = new Color(0xf2a332);
    static final Color OWNED = new Color(0x3d4a55);
    static final Color MISSING = new Color(0x1c242b);
    static final Color WARN = new Color(0xff6a4a);
    static final Color GOLD = new Color(0xffe27a);

    private static final Map<Float, Font> PLAIN = new HashMap<>();
    private static final Map<Float, Font> BOLD = new HashMap<>();

    private Look() {
    }

    static int chip(Chip chip) {
        return switch (chip) {
            case GREEN -> 0x4fdc84;
            case AMBER -> 0xf2a332;
            case VIOLET -> 0xa77cf0;
        };
    }

    static Color colour(int rgb, int alpha) {
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, alpha)));
    }

    static synchronized Font font(float size, boolean bold) {
        return (bold ? BOLD : PLAIN).computeIfAbsent(size, s -> new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(s));
    }
}
