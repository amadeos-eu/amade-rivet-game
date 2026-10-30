package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.domain.World;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewportTest {
    private final Textures textures = new Textures();

    private Effects effects() {
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        return new Effects(mixer, Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH));
    }

    @Test
    void theCellsAroundThePlayerAreExploredButNotThoseBehindWalls() {
        World world = new Run(Difficulty.NORMAL, 1).start(textures, new SeededRandom(1));
        Viewport view = new Viewport(textures.sky());
        view.show(world);
        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        view.render(g, 640, effects(), Art.placeholder(), 0, true, 0);
        g.dispose();
        int px = (int) world.player().x(), py = (int) world.player().y();
        assertTrue(view.explored(px, py));
        assertTrue(view.explored(px + 2, py), "open yard");
        assertFalse(view.explored(20, 4), "a room behind the wall");
    }

    @Test
    void theViewWidensWithTheCanvas() {
        World world = new Run(Difficulty.NORMAL, 1).start(textures, new SeededRandom(1));
        Viewport view = new Viewport(textures.sky());
        view.show(world);
        BufferedImage wide = new BufferedImage(712, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = wide.createGraphics();
        view.render(g, 712, effects(), Art.placeholder(), 0, true, 0);
        g.dispose();
        assertNotEquals(0, wide.getRGB(711, 300), "drawn to the right edge");
    }
}
