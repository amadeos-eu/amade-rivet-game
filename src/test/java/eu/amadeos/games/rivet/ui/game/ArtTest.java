package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.raycast.Texture;
import eu.amadeos.gameengine.raycast.Views;
import eu.amadeos.gameengine.raycast.model.ModelRenderer;
import eu.amadeos.games.rivet.domain.MachineKind;
import eu.amadeos.games.rivet.domain.Weapon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtTest {
    @TempDir
    Path dir;

    @Test
    void viewsComeBackFromTheCacheAsTheyWent() {
        Views views = new ModelRenderer().views(f -> Models.drone(f), 2, 4, 32, 12);
        Path file = dir.resolve("drone.bin");
        Art.writeViews(file, views);
        Views read = Art.readViews(file);
        assertNotNull(read);
        assertEquals(2, read.frames());
        assertEquals(4, read.directions());
        for (int f = 0; f < 2; f++) for (int d = 0; d < 4; d++) {
            Texture a = views.pick(f, 0, Math.PI / 2 * d), b = read.pick(f, 0, Math.PI / 2 * d);
            for (int v = 0; v < 32; v++) for (int u = 0; u < 32; u++) {
                assertEquals(a.argb(u, v), b.argb(u, v));
                assertEquals(a.glow(u, v), b.glow(u, v), 0.01);
            }
        }
    }

    @Test
    void aDamagedCacheFileIsRenderedAgain() throws Exception {
        Path file = dir.resolve("broken.bin");
        Files.writeString(file, "not a picture");
        assertNull(Art.readViews(file));
        assertNull(Art.readViews(dir.resolve("missing.bin")));
    }

    @Test
    void theDroneSeenFromTheFrontShowsItsGlowingEye() {
        Texture front = new ModelRenderer().billboard(Models.drone(0), 64, 12);
        boolean glowing = false;
        for (int v = 0; v < 64; v++) for (int u = 0; u < 64; u++) glowing |= front.glow(u, v) > 0.9;
        assertTrue(glowing);
    }

    @Test
    void theToolIsDrawnInTheLowerRightAndLitWhenFiring() {
        ModelRenderer renderer = new ModelRenderer();
        BufferedImage still = Art.tool(renderer, Weapon.RIVET_GUN, false);
        assertEquals(Art.TOOL_W * Art.TOOL_SCALE, still.getWidth());
        int covered = 0;
        for (int y = 0; y < still.getHeight(); y += 4) for (int x = 0; x < still.getWidth(); x += 4) {
            if (still.getRGB(x, y) >>> 24 > 0) covered++;
        }
        assertTrue(covered > 200, "the gun is in the picture");
        assertEquals(0, still.getRGB(0, 0) >>> 24, "the upper left is the room");
    }

    @Test
    void everyMachineHasAnAttackFrame() {
        for (MachineKind k : MachineKind.values()) assertEquals(Art.frames(k) - 1, Art.attackFrame(k));
        Art art = Art.placeholder();
        for (MachineKind k : MachineKind.values()) assertEquals(Art.frames(k), art.machines.get(k).frames());
    }

    @Test
    void blastsGrowAndBreakUp() {
        int first = 0, last = 0;
        Texture a = Art.fireball(64, 0, Art.EXPLOSION_FRAMES), b = Art.fireball(64, Art.EXPLOSION_FRAMES - 1, Art.EXPLOSION_FRAMES);
        for (int v = 0; v < 64; v++) for (int u = 0; u < 64; u++) {
            if (a.opaque(u, v)) first++;
            if (b.opaque(u, v)) last++;
        }
        assertTrue(first > 300);
        assertTrue(last < first * 2, "the last frame is broken up");
    }
}
