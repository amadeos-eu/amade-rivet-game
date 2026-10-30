package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.raycast.Texture;
import eu.amadeos.games.rivet.domain.Chip;
import eu.amadeos.games.rivet.domain.Surfaces;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The walls, floors, ceilings and doors of Kessel Deep, painted texel by texel from noise and a few rules
 * (seams, rivets, stripes) when the game starts: no picture files. Each is 64 × 64.
 */
final class Textures implements Surfaces {
    static final int T = 64;

    private final Map<Character, Texture> walls = new HashMap<>();
    private final Map<String, Texture> floors = new HashMap<>();
    private final Texture ceiling = paintCeiling(false);
    private final Texture lamp = paintCeiling(true);
    private final Map<Chip, Texture> lockedDoors = new HashMap<>();
    private final Texture door = door(0xffa431, 0xd8a422);
    private final Texture frame = frame();
    private final BufferedImage sky = paintSky();

    Textures() {
        walls.put('P', panel(0x5d6873, false, 1));
        walls.put('H', panel(0x5d6873, true, 2));
        walls.put('T', pipes());
        walls.put('C', console());
        walls.put('I', ice());
        walls.put('R', rock());
        walls.put('K', crates());
        walls.put('M', pump());
        walls.put('X', liftSwitch());
        floors.put("plate", plate());
        floors.put("grate", grate());
        floors.put("snow", snow());
        floors.put("ice", iceFloor());
        floors.put("rock", rock());
        lockedDoors.put(Chip.GREEN, door(0x4fdc84, 0x4fdc84));
        lockedDoors.put(Chip.AMBER, door(0xf2a332, 0xf2a332));
        lockedDoors.put(Chip.VIOLET, door(0xa77cf0, 0xa77cf0));
    }

    @Override
    public Texture wall(char mark) {
        return walls.getOrDefault(mark, walls.get('P'));
    }

    @Override
    public Texture floor(String name) {
        return floors.getOrDefault(name, floors.get("plate"));
    }

    @Override
    public Texture ceiling(boolean withLamp) {
        return withLamp ? lamp : ceiling;
    }

    @Override
    public Texture door(Chip lock) {
        return lock == null ? door : lockedDoors.get(lock);
    }

    @Override
    public Texture doorFrame() {
        return frame;
    }

    /** The sky over the landing pad: stars, and the banded giant planet the moon goes round. */
    BufferedImage sky() {
        return sky;
    }

    // ---- painting helpers ---------------------------------------------------------------------------

    static int add(int rgb, int n) {
        int r = Math.max(0, Math.min(255, ((rgb >> 16) & 255) + n));
        int g = Math.max(0, Math.min(255, ((rgb >> 8) & 255) + n));
        int b = Math.max(0, Math.min(255, (rgb & 255) + n));
        return (r << 16) | (g << 8) | b;
    }

    private static double[][] grid(Random r, int n) {
        double[][] g = new double[n][n];
        for (double[] row : g) for (int i = 0; i < n; i++) row[i] = r.nextDouble();
        return g;
    }

    /** Smooth value noise that wraps around, so the texture tiles. */
    private static double noise(double[][] grid, double x, double y) {
        int n = grid.length, x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = x - x0, fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = grid[Math.floorMod(y0, n)][Math.floorMod(x0, n)], b = grid[Math.floorMod(y0, n)][Math.floorMod(x0 + 1, n)];
        double c = grid[Math.floorMod(y0 + 1, n)][Math.floorMod(x0, n)], d = grid[Math.floorMod(y0 + 1, n)][Math.floorMod(x0 + 1, n)];
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }

    /** Collects texels, then makes the texture. */
    private static final class Canvas {
        final int[] rgb = new int[T * T];
        final float[] glow = new float[T * T];

        void set(int x, int y, int c) {
            rgb[y * T + x] = 0xff000000 | c;
        }

        void set(int x, int y, int c, float g) {
            set(x, y, c);
            glow[y * T + x] = g;
        }

        int get(int x, int y) {
            return rgb[y * T + x] & 0xffffff;
        }

        Texture texture() {
            return Texture.of(T, rgb, glow);
        }
    }

    // ---- walls --------------------------------------------------------------------------------------

    static Texture panel(int base, boolean hazard, long seed) {
        Canvas t = new Canvas();
        Random r = new Random(seed);
        int[][] rivets = {{4, 4}, {27, 4}, {4, 27}, {27, 27}};
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(base, r.nextInt(12) - 6 - y * y / 220);
            int lx = x % 32, ly = y % 32;
            if (ly == 0) c = add(c, -42); else if (ly == 1) c = add(c, 20);
            if (lx == 0) c = add(c, -36); else if (lx == 1) c = add(c, 16);
            for (int[] p : rivets) {
                double d = Math.hypot(lx - p[0] - 0.5, ly - p[1] - 0.5);
                if (d < 1.3) c = add(base, 50); else if (d < 2.3) c = add(base, -34);
            }
            if (hazard && y >= 46) c = y == 46 ? 0x1a1d20 : add(((x + y) / 6) % 2 == 0 ? 0xd4a020 : 0x1e1e1e, r.nextInt(14) - 7);
            if (!hazard && y >= 58) c = add(c, -18);
            t.set(x, y, c);
        }
        return t.texture();
    }

    private static Texture pipes() {
        Canvas t = new Canvas();
        Random r = new Random(3);
        int[][] p = {{11, 8, 0x8a4c32}, {31, 6, 0x7d8892}, {50, 9, 0x4e6a50}};
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(0x2e343b, r.nextInt(10) - 5);
            for (int[] q : p) {
                double d = (x + 0.5 - q[0]) / q[1];
                if (Math.abs(d) < 1) {
                    boolean flange = Math.abs(y - 18) < 2 || Math.abs(y - 50) < 2;
                    double s = 0.4 + 0.75 * Math.cos(d * Math.PI / 2) + (Math.abs(d + 0.35) < 0.12 ? 0.35 : 0);
                    int base = flange ? add(q[2], 28) : q[2];
                    int cr = (int) Math.min(255, ((base >> 16) & 255) * s), cg = (int) Math.min(255, ((base >> 8) & 255) * s);
                    int cb = (int) Math.min(255, (base & 255) * s);
                    c = add((cr << 16) | (cg << 8) | cb, r.nextInt(8) - 4);
                } else if (Math.abs(d) < 1.25) {
                    c = add(c, -22);
                }
            }
            t.set(x, y, c);
        }
        return t.texture();
    }

    private static Texture console() {
        Canvas t = new Canvas();
        Texture base = panel(0x3a424b, false, 4);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) t.set(x, y, base.argb(x, y) & 0xffffff);
        Random r = new Random(5);
        for (int y = 10; y < 38; y++) for (int x = 9; x < 55; x++) {
            boolean frame = y < 12 || y > 35 || x < 11 || x > 52;
            t.set(x, y, frame ? 0x15191d : 0x072a19, frame ? 0 : 0.6f);
        }
        for (int line = 0; line < 7; line++) {
            int y = 14 + line * 3, len = 8 + r.nextInt(30);
            for (int x = 14; x < 14 + len; x++) if (r.nextInt(6) > 0) t.set(x, y, 0x6cf0a0, 1);
        }
        int[] colors = {0xe04030, 0xf0b030, 0x40e070, 0x40e070, 0xe04030};
        for (int i = 0; i < 5; i++) for (int y = 43; y < 48; y++) for (int x = 13 + i * 8; x < 18 + i * 8; x++) t.set(x, y, colors[i], 1);
        return t.texture();
    }

    private static Texture ice() {
        Canvas t = new Canvas();
        Random r = new Random(6);
        double[][] g1 = grid(r, 8), g2 = grid(r, 16);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            double n = noise(g1, x / 8.0, y / 8.0) * 0.7 + noise(g2, x / 4.0, y / 4.0) * 0.3;
            int c = n < 0.35 ? 0x4c86a6 : n < 0.6 ? 0x7fb6d0 : 0xa9d8ea;
            t.set(x, y, add(c, (int) (n * 30) + r.nextInt(8) - 4), 0.28f);
        }
        for (int k = 0; k < 5; k++) {
            double x = r.nextInt(T), y = r.nextInt(T), a = r.nextDouble() * Math.PI;
            for (int s = 0; s < 40; s++) {
                x += Math.cos(a);
                y += Math.sin(a);
                a += r.nextGaussian() * 0.35;
                t.set(Math.floorMod((int) x, T), Math.floorMod((int) y, T), 0xe6f8ff, 0.6f);
            }
        }
        return t.texture();
    }

    private static Texture rock() {
        Canvas t = new Canvas();
        Random r = new Random(7);
        double[][] g = grid(r, 8), g2 = grid(r, 16);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            double n = noise(g, x / 8.0, y / 8.0) * 0.75 + noise(g2, x / 4.0, y / 4.0) * 0.25;
            int c = add(0x47423c, (int) (n * 56) - 22 + r.nextInt(10));
            if (Math.abs(n - 0.5) < 0.015) c = add(c, -30);
            t.set(x, y, c);
        }
        return t.texture();
    }

    private static Texture crates() {
        Canvas t = new Canvas();
        Random r = new Random(9);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int lx = x % 32, ly = y % 32;
            boolean edge = lx < 3 || lx > 28 || ly < 3 || ly > 28;
            boolean brace = Math.abs(lx - ly) < 2 || Math.abs(lx + ly - 31) < 2;
            int c = edge ? 0x3c4430 : brace ? 0x4b5539 : 0x5c6a40;
            if (lx == 0 || ly == 0) c = 0x1d2117;
            if (ly > 18 && ly < 24 && lx > 8 && lx < 23 && (lx + ly) % 4 == 0) c = 0xd8d2b8;
            t.set(x, y, add(c, r.nextInt(10) - 5));
        }
        return t.texture();
    }

    private static Texture pump() {
        Canvas t = new Canvas();
        Random r = new Random(10);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(0x3b4249, r.nextInt(8) - 4);
            double d = Math.hypot(x - 31.5, y - 28.5);
            if (d < 20) {
                double s = 0.55 + 0.45 * Math.cos(d / 20 * Math.PI / 2);
                c = add((int) (0x6a * s) << 16 | (int) (0x74 * s) << 8 | (int) (0x7e * s), r.nextInt(6));
                if (Math.abs(d - 14) < 1) c = 0x22272b;
                if (d < 5) c = add(0x8a949e, -(int) (d * 6));
            }
            for (int bx : new int[]{6, 57}) if (Math.abs(x - bx) < 2 && y > 4 && y < 60) c = add(0x8a4c32, (y % 8 == 0) ? -40 : 0);
            if (y > 52 && y < 58 && x > 14 && x < 50) {
                boolean on = x % 9 < 6;
                t.set(x, y, on ? 0x40e070 : 0x12181c, on ? 1 : 0);
                continue;
            }
            t.set(x, y, c);
        }
        return t.texture();
    }

    private static Texture liftSwitch() {
        Canvas t = new Canvas();
        Texture base = panel(0x4a5560, true, 11);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) t.set(x, y, base.argb(x, y) & 0xffffff);
        for (int y = 12; y < 40; y++) for (int x = 18; x < 46; x++) {
            boolean frame = y < 14 || y > 37 || x < 20 || x > 43;
            t.set(x, y, frame ? 0x15191d : 0x0b3a1f, frame ? 0 : 0.5f);
        }
        // an arrow down: the lift
        for (int y = 17; y < 35; y++) for (int x = 22; x < 42; x++) {
            boolean shaft = x >= 29 && x < 35 && y < 27;
            boolean head = y >= 26 && Math.abs(x - 31.5) < 35 - y;
            if (shaft || head) t.set(x, y, 0x70ff9c, 1);
        }
        return t.texture();
    }

    private static Texture door(int light, int stripe) {
        Canvas t = new Canvas();
        Random r = new Random(8);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(0x6c727a, r.nextInt(10) - 5);
            float g = 0;
            if (x < 8 || x >= 56) c = add(((x + y) / 5) % 2 == 0 ? stripe : 0x202020, r.nextInt(10) - 5);
            if (x == 8 || x == 55) c = 0x1a1d20;
            if (x == 31 || x == 32) c = 0x16191c;
            if (x == 33) c = add(c, 20);
            for (int rib : new int[]{24, 44}) {
                if (y == rib) c = add(c, 34);
                if (y == rib + 1 || y == rib + 2) c = add(c, -30);
            }
            if (y < 5) c = 0x24282d;
            if (y >= 10 && y < 17 && x >= 25 && x < 39) {
                boolean frame = y == 10 || y == 16 || x == 25 || x == 38;
                c = frame ? 0x1a1a1a : light;
                g = frame ? 0 : 1;
            }
            t.set(x, y, c, g);
        }
        return t.texture();
    }

    private static Texture frame() {
        Canvas t = new Canvas();
        Random r = new Random(12);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(0x2f353b, r.nextInt(8) - 4);
            if (x % 16 < 2) c = add(c, -14);
            if (((x + y) / 8) % 2 == 0 && (y < 6 || y > 57)) c = 0xb88a1c;
            t.set(x, y, c);
        }
        return t.texture();
    }

    // ---- floors and ceilings -----------------------------------------------------------------------

    private static Texture grate() {
        Canvas t = new Canvas();
        Random r = new Random(9);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c;
            if (x % 16 < 2 || y % 16 < 2) c = add(0x535c65, r.nextInt(12) - 6);
            else if (x % 16 == 2 || y % 16 == 2) c = add(0x3a4149, r.nextInt(8));
            else c = add(0x22282d, r.nextInt(6));
            if (x < 2 || y < 2) c = add(0x6a737d, r.nextInt(10) - 5);
            t.set(x, y, c);
        }
        return t.texture();
    }

    private static Texture plate() {
        Canvas t = new Canvas();
        Random r = new Random(10);
        double[][] g = grid(r, 8);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(0x4b5158, r.nextInt(10) - 5);
            int lx = x % 8, ly = y % 8;
            boolean flip = ((x / 8) + (y / 8)) % 2 == 0;
            int a = flip ? lx - ly : lx + ly - 7;
            if (Math.abs(a) <= 1 && lx > 1 && lx < 7 && ly > 1 && ly < 7) c = add(c, a == 0 ? 34 : 18);
            if (x == 0 || y == 0) c = 0x2a2e33;
            double frost = noise(g, x / 8.0, y / 8.0);
            if (frost > 0.72) c = add(c, (int) ((frost - 0.72) * 260));
            t.set(x, y, c);
        }
        return t.texture();
    }

    private static Texture snow() {
        Canvas t = new Canvas();
        Random r = new Random(13);
        double[][] g = grid(r, 8);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            double n = noise(g, x / 8.0, y / 8.0);
            t.set(x, y, add(0xb9c6d2, (int) (n * 30) - 12 + r.nextInt(8)));
        }
        return t.texture();
    }

    private static Texture iceFloor() {
        Canvas t = new Canvas();
        Random r = new Random(14);
        double[][] g = grid(r, 8);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            double n = noise(g, x / 8.0, y / 8.0);
            int c = add(n < 0.45 ? 0x3c6a84 : 0x5b8fa8, (int) (n * 24) + r.nextInt(6));
            if (x == 0 || y == 0) c = add(c, -20);
            t.set(x, y, c, 0.12f);
        }
        return t.texture();
    }

    private static Texture paintCeiling(boolean withLamp) {
        Canvas t = new Canvas();
        Random r = new Random(11);
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            int c = add(0x2a3036, r.nextInt(8) - 4);
            if (x % 32 == 0 || y % 32 == 0) c = add(c, -16);
            float g = 0;
            if (withLamp && x >= 22 && x < 42 && y >= 6 && y < 58) {
                boolean frame = x < 24 || x >= 40 || y < 8 || y >= 56;
                c = frame ? 0x181b1f : add(0xdcecff, r.nextInt(10) - 5);
                g = frame ? 0 : 1;
            }
            t.set(x, y, c, g);
        }
        return t.texture();
    }

    private static BufferedImage paintSky() {
        int w = 1024, h = 160;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(15);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double t = (double) y / h;
            int c = (int) (0x06 + t * 0x18) << 16 | (int) (0x0a + t * 0x22) << 8 | (int) (0x16 + t * 0x36);
            img.setRGB(x, y, c);
        }
        for (int i = 0; i < 700; i++) {
            int x = r.nextInt(w), y = r.nextInt(h - 20);
            int b = 120 + r.nextInt(135) - y / 2;
            if (b > 40) img.setRGB(x, y, b << 16 | b << 8 | Math.min(255, b + 20));
        }
        // the giant planet, low over the horizon, with bands
        double cx = 300, cy = h + 40, rad = 150;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy);
            if (d > rad) continue;
            double band = Math.sin(dy / rad * 9 + Math.sin(dx / rad * 3) * 0.6);
            int base = band > 0.3 ? 0xc9a27a : band > -0.4 ? 0xa97c5a : 0x8a6248;
            double lit = Math.max(0.15, Math.min(1, 0.6 - dx / rad * 0.6 - dy / rad * 0.2));
            int c = (int) (((base >> 16) & 255) * lit) << 16 | (int) (((base >> 8) & 255) * lit) << 8 | (int) ((base & 255) * lit);
            img.setRGB(x, y, c);
        }
        return img;
    }
}
