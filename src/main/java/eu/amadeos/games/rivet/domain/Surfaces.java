package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.raycast.Texture;

/** The textures a {@link World} builds its map with; the game paints them, tests use plain ones. */
public interface Surfaces {
    /** The wall for a wall character of the map, such as {@code P}. */
    Texture wall(char mark);

    /** A floor by its name in the level file: plate, grate, snow, ice or rock. */
    Texture floor(String name);

    Texture ceiling(boolean lamp);

    /** A door, locked to a chip's colour or ({@code null}) open to all. */
    Texture door(Chip lock);

    Texture doorFrame();
}
