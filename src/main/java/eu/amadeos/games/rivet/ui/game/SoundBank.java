package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.games.rivet.domain.MachineKind;
import eu.amadeos.games.rivet.domain.Weapon;

/** The sounds of Rivet, made once at start from tones and noise: no recordings. */
final class SoundBank {
    final Sound pickSwing = Sound.tone(Wave.NOISE, 0, 0, 0.12, 0.07);
    final Sound rivet = Sound.mix(Sound.tone(Wave.SQUARE, 240, 90, 0.08, 0.1), Sound.tone(Wave.NOISE, 0, 0, 0.07, 0.16));
    final Sound scatter = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.22, 0.26), Sound.tone(Wave.TRIANGLE, 150, 45, 0.24, 0.4));
    final Sound weld = Sound.mix(Sound.tone(Wave.SQUARE, 95, 110, 0.09, 0.045), Sound.tone(Wave.NOISE, 0, 0, 0.09, 0.05));
    final Sound plasma = Sound.mix(Sound.tone(Wave.TRIANGLE, 1100, 180, 0.3, 0.16), Sound.tone(Wave.SQUARE, 300, 60, 0.18, 0.04));
    final Sound toss = Sound.tone(Wave.TRIANGLE, 300, 520, 0.12, 0.12);
    final Sound tink = Sound.tone(Wave.SQUARE, 2100, 1500, 0.035, 0.035);
    final Sound clank = Sound.mix(Sound.tone(Wave.SQUARE, 620, 300, 0.07, 0.07), Sound.tone(Wave.NOISE, 0, 0, 0.05, 0.08));
    final Sound crunch = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.5, 0.28), Sound.tone(Wave.TRIANGLE, 160, 40, 0.45, 0.35),
            Sound.tone(Wave.SQUARE, 900, 200, 0.2, 0.05));
    final Sound explosion = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 1.0, 0.36), Sound.tone(Wave.TRIANGLE, 90, 22, 0.9, 0.5));
    final Sound hiss = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 1.1, 0.14), Sound.tone(Wave.TRIANGLE, 70, 40, 0.4, 0.3));
    final Sound hurt = Sound.mix(Sound.tone(Wave.TRIANGLE, 190, 85, 0.16, 0.32), Sound.tone(Wave.NOISE, 0, 0, 0.06, 0.08));
    final Sound died = Sound.tone(Wave.TRIANGLE, 260, 40, 1.2, 0.35);
    final Sound pickup = Sound.tone(Wave.SQUARE, 660, 660, 0.05, 0.05).then(Sound.tone(Wave.SQUARE, 990, 990, 0.07, 0.05));
    final Sound gotTool = Sound.tone(Wave.SQUARE, 440, 440, 0.07, 0.06).then(Sound.tone(Wave.SQUARE, 660, 660, 0.07, 0.06))
            .then(Sound.tone(Wave.SQUARE, 880, 880, 0.14, 0.06));
    final Sound chip = Sound.tone(Wave.TRIANGLE, 1320, 1320, 0.1, 0.18).then(Sound.tone(Wave.TRIANGLE, 1760, 1760, 0.25, 0.16));
    final Sound door = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.5, 0.1), Sound.tone(Wave.TRIANGLE, 80, 120, 0.5, 0.18));
    final Sound locked = Sound.tone(Wave.SQUARE, 140, 140, 0.12, 0.08).then(Sound.tone(Wave.SQUARE, 110, 110, 0.2, 0.08));
    final Sound secret = Sound.tone(Wave.TRIANGLE, 784, 784, 0.08, 0.15).then(Sound.tone(Wave.TRIANGLE, 988, 988, 0.08, 0.15))
            .then(Sound.tone(Wave.TRIANGLE, 1175, 1175, 0.08, 0.15)).then(Sound.tone(Wave.TRIANGLE, 1568, 1568, 0.3, 0.15));
    final Sound empty = Sound.tone(Wave.SQUARE, 1400, 1400, 0.015, 0.05);
    final Sound change = Sound.tone(Wave.SQUARE, 500, 500, 0.02, 0.05).then(Sound.tone(Wave.NOISE, 0, 0, 0.03, 0.03))
            .then(Sound.tone(Wave.SQUARE, 700, 700, 0.02, 0.05));
    final Sound launch = Sound.tone(Wave.SQUARE, 200, 1200, 0.6, 0.05);
    final Sound lift = Sound.mix(Sound.tone(Wave.TRIANGLE, 60, 180, 1.2, 0.3), Sound.tone(Wave.NOISE, 0, 0, 1.2, 0.06));

    Sound shot(Weapon w) {
        return switch (w) {
            case ICE_PICK -> pickSwing;
            case RIVET_GUN -> rivet;
            case SCATTER_RIVETER -> scatter;
            case ARC_WELDER -> weld;
            case PLASMA_CUTTER -> plasma;
            case BLASTING_CHARGE -> toss;
        };
    }

    /** What a machine says when it notices the player. */
    final Sound droneAlert = Sound.tone(Wave.SQUARE, 700, 1400, 0.12, 0.05).then(Sound.tone(Wave.SQUARE, 1400, 1400, 0.05, 0.05));
    final Sound crawlerAlert = Sound.tone(Wave.SQUARE, 1800, 1200, 0.04, 0.04).then(Sound.tone(Wave.SQUARE, 1900, 1300, 0.04, 0.04))
            .then(Sound.tone(Wave.SQUARE, 2000, 1400, 0.04, 0.04));
    final Sound sentryAlert = Sound.tone(Wave.SQUARE, 1000, 1000, 0.07, 0.05).then(Sound.tone(Wave.SQUARE, 1000, 1000, 0.05, 0))
            .then(Sound.tone(Wave.SQUARE, 1000, 1000, 0.07, 0.05));
    final Sound loaderAlert = Sound.tone(Wave.SQUARE, 110, 98, 0.45, 0.08);
    final Sound foremanAlert = Sound.mix(Sound.tone(Wave.SQUARE, 55, 52, 1.2, 0.08), Sound.tone(Wave.TRIANGLE, 110, 104, 1.2, 0.2));

    Sound alert(MachineKind k) {
        return switch (k) {
            case DRONE -> droneAlert;
            case CRAWLER -> crawlerAlert;
            case SENTRY -> sentryAlert;
            case LOADER -> loaderAlert;
            case FOREMAN -> foremanAlert;
        };
    }

    final Sound zap = Sound.mix(Sound.tone(Wave.SQUARE, 1600, 400, 0.15, 0.05), Sound.tone(Wave.NOISE, 0, 0, 0.1, 0.06));
    final Sound drill = Sound.tone(Wave.SQUARE, 380, 520, 0.25, 0.06);
    final Sound burst = Sound.mix(Sound.tone(Wave.SQUARE, 300, 120, 0.05, 0.06), Sound.tone(Wave.NOISE, 0, 0, 0.04, 0.1));
    final Sound heave = Sound.tone(Wave.TRIANGLE, 90, 180, 0.35, 0.3);
    final Sound fan = Sound.mix(Sound.tone(Wave.SQUARE, 220, 880, 0.3, 0.05), Sound.tone(Wave.TRIANGLE, 110, 440, 0.3, 0.15));

    Sound attack(MachineKind k) {
        return switch (k) {
            case DRONE -> zap;
            case CRAWLER -> drill;
            case SENTRY -> burst;
            case LOADER -> heave;
            case FOREMAN -> fan;
        };
    }
}
