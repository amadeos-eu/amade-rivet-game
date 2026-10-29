package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/**
 * The tunes of Rivet, in MML: a slow cold theme for the title, a pulse of bass under each section, a short
 * signal when the lift is reached, and a falling line when the suit gives up.
 */
final class Music {
    final Sound title = Sound.mix(
            Tune.voice(Wave.TRIANGLE, 0.07, "t84 o5 l4 e. d8 c < b2 | a. b8 >c d2 | e. g8 f e | d1"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t84 o2 l2 a a | f f | c c | g g"),
            Tune.voice(Wave.NOISE, 0.02, "t84 o4 l1 r r r c"));

    /** A loop under each section: deeper and faster the further down. */
    final Sound[] section = {
            Sound.mix(Tune.voice(Wave.TRIANGLE, 0.18, "t96 o2 l8 a r a r a > c < a r | f r f r f a f r | g r g r g b g r | e r e r e g# e r"),
                    Tune.voice(Wave.SQUARE, 0.025, "t96 o4 l2 r a | r f | r g | r e")),
            Sound.mix(Tune.voice(Wave.TRIANGLE, 0.2, "t108 o2 l8 d d r d f d r c | d d r d g f e c | d d r d f d r c | < a > c d d d4 r4"),
                    Tune.voice(Wave.NOISE, 0.03, "t108 o4 l8 c r c r c r c r | c r c r c r c r | c r c r c r c r | c r c r c4 r4")),
            Sound.mix(Tune.voice(Wave.TRIANGLE, 0.22, "t120 o1 l8 b b > d < b b > e d < b | b b > d < b b > f e d | b b > d < b b > e d < b | > c c c c < b4 r4"),
                    Tune.voice(Wave.SQUARE, 0.025, "t120 o4 l4 f+ r e r | f+ r g r | f+ r e r | d2 r2"),
                    Tune.voice(Wave.NOISE, 0.035, "t120 o4 l16 c c r8 c c r8 c c r8 c c r8 | c c r8 c c r8 c c r8 c c r8"))};

    final Sound levelDone = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t140 o5 l8 c e g >c4 < g >c e2"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t140 o3 l8 c g >c4 < g c c2"));

    final Sound won = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t120 o5 l8 e g >c4 < b g e4 | f a >d4 c < b >c2"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t120 o3 l4 c g c g | f >c < g c2"));

    final Sound dead = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.05, "t80 o4 l4 e d# d c# | c1"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t80 o2 l2 a g# | a1"));
}
