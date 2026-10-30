package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.raycast.Texture;

import java.util.Arrays;

/** Surfaces of one grey for the tests: the rules do not look at colours. */
final class Plain implements Surfaces {
    static final Plain SURFACES = new Plain();
    private final Texture grey;

    private Plain() {
        int[] texels = new int[16 * 16];
        Arrays.fill(texels, 0xff808080);
        grey = Texture.of(16, texels, null);
    }

    @Override
    public Texture wall(char mark) {
        return grey;
    }

    @Override
    public Texture floor(String name) {
        return grey;
    }

    @Override
    public Texture ceiling(boolean lamp) {
        return grey;
    }

    @Override
    public Texture door(Chip lock) {
        return grey;
    }

    @Override
    public Texture doorFrame() {
        return grey;
    }
}
