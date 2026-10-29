package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.raycast.Texture;
import eu.amadeos.gameengine.raycast.Views;
import eu.amadeos.gameengine.raycast.model.Model;
import eu.amadeos.gameengine.raycast.model.ModelRenderer;
import eu.amadeos.gameengine.raycast.model.ModelRenderer.Lamp;
import eu.amadeos.gameengine.raycast.model.ModelRenderer.Placement;
import eu.amadeos.games.rivet.domain.MachineKind;
import eu.amadeos.games.rivet.domain.PickupKind;
import eu.amadeos.games.rivet.domain.Projectile;
import eu.amadeos.games.rivet.domain.Weapon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Every picture of the game that is not a wall: the machines from eight sides, the pickups, crates and
 * canisters, glowing shots and blasts, and the tools in the hand. The 3D ones are rendered from
 * {@link Models} when the game starts, which takes a few seconds; they are then kept in the cache directory
 * ({@code ~/.cache/amadeos/rivet}) so the next start is quick.
 */
final class Art {
    /** Raise this when a model changes, so old pictures in the cache are not used. */
    static final int VERSION = 1;
    static final int MACHINE_SIZE = 128;
    static final int ITEM_SIZE = 64;
    static final int DIRECTIONS = 8;
    /** The tool pictures have this many pixels per pixel of the 640 × 400 view, for a sharper tool. */
    static final int TOOL_SCALE = 2;
    /** Where the tool pictures sit in the 640 × 400 view: the window right of and below the middle. */
    static final int TOOL_X = 300, TOOL_Y = 196, TOOL_W = 340, TOOL_H = 214;
    static final int EXPLOSION_FRAMES = 6, PUFF_FRAMES = 4, CLOUD_FRAMES = 6;

    private static final Logger log = LoggerFactory.getLogger(Art.class);

    /** The frames of each machine: walking (or idle), then the attack. */
    static int frames(MachineKind kind) {
        return switch (kind) {
            case DRONE, SENTRY, FOREMAN -> 2;
            case CRAWLER, LOADER -> 5;
        };
    }

    /** The frame of the attack. */
    static int attackFrame(MachineKind kind) {
        return frames(kind) - 1;
    }

    /** How big the billboard of a machine is, and how high it floats: the model's -1 … 1 maps onto it. */
    static double size(MachineKind kind) {
        return switch (kind) {
            case DRONE -> 0.8;
            case CRAWLER -> 1.3;
            case SENTRY -> 1.0;
            case LOADER -> 1.5;
            case FOREMAN -> 1.3;
        };
    }

    final Map<MachineKind, Views> machines = new EnumMap<>(MachineKind.class);
    final Map<PickupKind, Views> pickups = new EnumMap<>(PickupKind.class);
    final Map<Projectile.Kind, Views> projectiles = new EnumMap<>(Projectile.Kind.class);
    final Map<Weapon, BufferedImage[]> tools = new EnumMap<>(Weapon.class);
    Views scrap;
    Views bigScrap;
    Views crate;
    Views canister;
    Views explosion;
    Views puff;
    Views cloud;

    private Art() {
    }

    /** Pictures of nothing, at once: for tests, which need the game but not its looks. */
    static Art placeholder() {
        Art art = new Art();
        Views none = Views.of(Texture.of(1, new int[1], new float[1]));
        for (MachineKind k : MachineKind.values()) {
            Texture[][] frames = new Texture[frames(k)][1];
            for (Texture[] f : frames) f[0] = none.pick(0, 0, 0);
            art.machines.put(k, new Views(frames));
        }
        for (PickupKind k : PickupKind.values()) art.pickups.put(k, none);
        art.projectiles.put(Projectile.Kind.ICE_BLOCK, none);
        art.projectiles.put(Projectile.Kind.CHARGE, none);
        art.scrap = art.bigScrap = art.crate = art.canister = none;
        for (Weapon w : Weapon.values()) {
            art.tools.put(w, new BufferedImage[]{new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB),
                    new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)});
        }
        art.glowing();
        return art;
    }

    /**
     * Renders the pictures, or reads them from {@code cache} (may be {@code null}); {@code progress} hears
     * 0 … 1 as they are done, from this thread.
     */
    static Art load(Path cache, DoubleConsumer progress) {
        Art art = new Art();
        ModelRenderer renderer = new ModelRenderer();
        ModelRenderer toolRenderer = new ModelRenderer().lamps(ModelRenderer.STUDIO, 0.22);
        List<Job> jobs = new ArrayList<>();
        for (MachineKind k : MachineKind.values()) {
            IntFunction<Model> frame = switch (k) {
                case DRONE -> Models::drone;
                case CRAWLER -> Models::crawler;
                case SENTRY -> Models::sentry;
                case LOADER -> Models::loader;
                case FOREMAN -> Models::foreman;
            };
            jobs.add(new Job("machine-" + k.name().toLowerCase(), frames(k) * DIRECTIONS,
                    () -> renderer.views(frame, frames(k), DIRECTIONS, MACHINE_SIZE, 12), v -> art.machines.put(k, v)));
        }
        jobs.add(new Job("scrap", DIRECTIONS / 2, () -> renderer.views(f -> Models.scrap(false), 1, DIRECTIONS, ITEM_SIZE, 20),
                v -> art.scrap = v));
        jobs.add(new Job("big-scrap", DIRECTIONS / 2, () -> renderer.views(f -> Models.scrap(true), 1, DIRECTIONS, ITEM_SIZE, 20),
                v -> art.bigScrap = v));
        for (PickupKind k : PickupKind.values()) {
            jobs.add(new Job("pickup-" + k.name().toLowerCase(), DIRECTIONS / 2,
                    () -> renderer.views(f -> Models.pickup(k), 1, DIRECTIONS, ITEM_SIZE, 18), v -> art.pickups.put(k, v)));
        }
        jobs.add(new Job("crate", 1, () -> Views.of(renderer.billboard(Models.crate(), ITEM_SIZE, 14)), v -> art.crate = v));
        jobs.add(new Job("canister", 1, () -> Views.of(renderer.billboard(Models.canister(), ITEM_SIZE, 14)), v -> art.canister = v));
        jobs.add(new Job("ice-block", 1, () -> Views.of(renderer.billboard(Models.iceBlock(), ITEM_SIZE / 2, 20)),
                v -> art.projectiles.put(Projectile.Kind.ICE_BLOCK, v)));
        jobs.add(new Job("charge", 1, () -> Views.of(renderer.billboard(Models.charge(), ITEM_SIZE / 2, 20)),
                v -> art.projectiles.put(Projectile.Kind.CHARGE, v)));
        int total = jobs.stream().mapToInt(Job::weight).sum() + Weapon.values().length * 6;
        double done = 0;
        Path dir = cache == null ? null : cache.resolve("art-" + VERSION);
        for (Job job : jobs) {
            Path file = dir == null ? null : dir.resolve(job.name + ".bin");
            Views views = file == null ? null : readViews(file);
            if (views == null) {
                views = job.render.get();
                if (file != null) writeViews(file, views);
            }
            job.into.accept(views);
            progress.accept((done += job.weight) / total);
        }
        for (Weapon w : Weapon.values()) {
            BufferedImage[] pictures = new BufferedImage[2];
            for (int firing = 0; firing < 2; firing++) {
                Path file = dir == null ? null : dir.resolve("tool-" + w.name().toLowerCase() + "-" + firing + ".png");
                BufferedImage picture = file == null ? null : readPicture(file);
                if (picture == null) {
                    picture = tool(toolRenderer, w, firing == 1);
                    if (file != null) writePicture(file, picture);
                }
                pictures[firing] = picture;
                progress.accept((done += 3) / total);
            }
            art.tools.put(w, pictures);
        }
        art.glowing();
        progress.accept(1);
        return art;
    }

    private record Job(String name, int weight, Supplier<Views> render, Consumer<Views> into) {
    }

    // ---- the tools ----------------------------------------------------------------------------------

    /** The tool in the right hand as the player sees it; firing, it is lit by its own flash. */
    static BufferedImage tool(ModelRenderer renderer, Weapon w, boolean firing) {
        Placement at = placement(w, firing);
        List<Lamp> lamps = new ArrayList<>(List.of(
                Lamp.from(-0.15, 0.9, 0.42, 1.0, 1.0, 1.05),
                Lamp.from(-0.9, 0.05, 0.3, 0.12, 0.4, 0.2),
                Lamp.from(0.5, 0.3, -0.6, 0.2, 0.28, 0.4)));
        if (firing && w != Weapon.ICE_PICK && w != Weapon.BLASTING_CHARGE) {
            double[] flash = flash(w);
            lamps.add(Lamp.at(at.x() + flash[0], at.y() + flash[1], at.z() + flash[2], flash[3], flash[4], flash[5]));
        }
        int s = TOOL_SCALE;
        return renderer.picture(Models.tool(w, firing, true), TOOL_W * s, TOOL_H * s, 480.0 * s,
                (320 - TOOL_X) * s, (200 - TOOL_Y) * s, at, lamps);
    }

    /** Where the tool is held: low on the right, pointing at the middle of the view, tilted a little. */
    static Placement placement(Weapon w, boolean firing) {
        return switch (w) {
            // swung: the pick comes down from the upper right
            case ICE_PICK -> firing ? new Placement(0.12, -0.3, 1.0, -12, 40, -25) : new Placement(0.3, -0.36, 1.0, -8, -12, -14);
            case BLASTING_CHARGE -> firing ? new Placement(0.12, -0.22, 0.9, -4, -10, -6) : new Placement(0.24, -0.34, 0.85, -3, -6, -9);
            case SCATTER_RIVETER -> new Placement(0.24, -0.36, 1.0, -2.4, firing ? -6 : -3.4, -9);
            default -> new Placement(0.25, -0.36, firing ? 0.97 : 1.0, -2.4, firing ? -5 : -3.4, -9);
        };
    }

    /** The muzzle flash's light, relative to the tool's place, and its colour. */
    private static double[] flash(Weapon w) {
        return switch (w) {
            case ARC_WELDER -> new double[]{-0.03, 0.06, 0.62, 1.5, 3.8, 5.0};
            case PLASMA_CUTTER -> new double[]{-0.03, 0.08, 0.55, 1.5, 4.5, 5.0};
            default -> new double[]{-0.03, 0.04, 0.62, 6.0, 3.6, 1.4};
        };
    }

    // ---- glowing things, drawn rather than rendered -------------------------------------------------

    private void glowing() {
        projectiles.put(Projectile.Kind.SPARK, Views.of(ball(32, 0xfff4d0, 0xffa040, 1)));
        projectiles.put(Projectile.Kind.PLASMA, Views.of(ball(32, 0xf0ffff, 0x40d8ff, 2)));
        projectiles.put(Projectile.Kind.BOLT, Views.of(ball(32, 0xfff0ff, 0xa060ff, 3)));
        Texture[][] blast = new Texture[EXPLOSION_FRAMES][1];
        for (int f = 0; f < EXPLOSION_FRAMES; f++) blast[f][0] = fireball(64, f, EXPLOSION_FRAMES);
        explosion = new Views(blast);
        Texture[][] sparks = new Texture[PUFF_FRAMES][1];
        for (int f = 0; f < PUFF_FRAMES; f++) sparks[f][0] = sparks(32, f);
        puff = new Views(sparks);
        Texture[][] mist = new Texture[CLOUD_FRAMES][1];
        for (int f = 0; f < CLOUD_FRAMES; f++) mist[f][0] = mist(64, f);
        cloud = new Views(mist);
    }

    /** A glowing ball, white in the middle; with a few rays so it flickers between the frames. */
    static Texture ball(int size, int core, int rim, long seed) {
        int[] argb = new int[size * size];
        float[] glow = new float[size * size];
        Random r = new Random(seed);
        double[] spikes = new double[7];
        for (int i = 0; i < spikes.length; i++) spikes[i] = r.nextDouble() * Math.PI * 2;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            double dx = (x + 0.5) / size * 2 - 1, dy = (y + 0.5) / size * 2 - 1, d = Math.hypot(dx, dy);
            double a = Math.atan2(dy, dx), reach = 0.45;
            for (double s : spikes) reach = Math.max(reach, 0.95 - Math.abs(Math.sin((a - s) * 0.5)) * 9);
            if (d > reach) continue;
            argb[y * size + x] = 0xff000000 | (d < 0.25 ? 0xffffff : d < 0.38 ? core : rim);
            glow[y * size + x] = 1;
        }
        return Texture.of(size, argb, glow);
    }

    /** One frame of a blast: a ball of fire that grows, breaks up and goes dark. */
    static Texture fireball(int size, int frame, int frames) {
        int[] argb = new int[size * size];
        float[] glow = new float[size * size];
        Random r = new Random(40 + frame);
        double t = (frame + 0.5) / frames;
        double radius = 0.35 + 0.6 * t;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            double dx = (x + 0.5) / size * 2 - 1, dy = (y + 0.5) / size * 2 - 1, d = Math.hypot(dx, dy);
            double edge = radius * (0.82 + 0.18 * Math.sin(Math.atan2(dy, dx) * 5 + frame * 1.7));
            if (d > edge) continue;
            double heat = 1 - d / edge * (0.4 + t * 0.6) - t * 0.5 + r.nextDouble() * 0.15;
            if (t > 0.5 && r.nextDouble() < (t - 0.5) * 1.6) continue;   // breaking up
            int c = heat > 0.75 ? 0xfff6d8 : heat > 0.5 ? 0xffd060 : heat > 0.25 ? 0xff8a2a : heat > 0 ? 0xb83c18 : 0x3a3434;
            argb[y * size + x] = 0xff000000 | c;
            glow[y * size + x] = heat > 0 ? 1 : 0;
        }
        return Texture.of(size, argb, glow);
    }

    /** One frame of sparks where a rivet strikes: a hot point and flying bits. */
    static Texture sparks(int size, int frame) {
        int[] argb = new int[size * size];
        float[] glow = new float[size * size];
        Random r = new Random(60);
        double c = size / 2.0;
        int core = Math.max(0, 4 - frame * 2);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            if (Math.hypot(x + 0.5 - c, y + 0.5 - c) < core) {
                argb[y * size + x] = 0xfffff0c0;
                glow[y * size + x] = 1;
            }
        }
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Math.PI * 2, speed = 3 + r.nextDouble() * 4, d = speed * (frame + 1);
            int x = (int) (c + Math.cos(a) * d), y = (int) (c + Math.sin(a) * d + frame * frame * 0.6);
            for (int k = 0; k < 2; k++) {
                int px = x - (int) Math.round(Math.cos(a) * k), py = y - (int) Math.round(Math.sin(a) * k);
                if (px < 0 || py < 0 || px >= size || py >= size) continue;
                argb[py * size + px] = 0xff000000 | (r.nextBoolean() ? 0xffe9a0 : 0xffb050);
                glow[py * size + px] = 1;
            }
        }
        return Texture.of(size, argb, glow);
    }

    /** One frame of the freezing cloud from a burst canister: pale, half see-through (every other texel). */
    static Texture mist(int size, int frame) {
        int[] argb = new int[size * size];
        float[] glow = new float[size * size];
        Random r = new Random(80 + frame);
        double radius = 0.4 + frame * 0.1;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            if (((x + y + frame) & 1) == 0) continue;
            double dx = (x + 0.5) / size * 2 - 1, dy = (y + 0.5) / size * 2 - 1, d = Math.hypot(dx, dy * 1.3);
            if (d > radius * (0.85 + r.nextDouble() * 0.15)) continue;
            if (frame > 3 && r.nextDouble() < (frame - 3) * 0.3) continue;
            argb[y * size + x] = 0xff000000 | (d < radius * 0.5 ? 0xeaf8ff : 0xb8e2f4);
            glow[y * size + x] = 0.55f;
        }
        return Texture.of(size, argb, glow);
    }

    // ---- the cache ----------------------------------------------------------------------------------

    static Views readViews(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(Files.newInputStream(file)))) {
            int frames = in.readInt(), directions = in.readInt(), size = in.readInt();
            Texture[][] textures = new Texture[frames][directions];
            for (int f = 0; f < frames; f++) for (int d = 0; d < directions; d++) {
                int[] argb = new int[size * size];
                float[] glow = new float[size * size];
                for (int i = 0; i < argb.length; i++) argb[i] = in.readInt();
                for (int i = 0; i < glow.length; i++) glow[i] = in.readUnsignedByte() / 255f;
                textures[f][d] = Texture.of(size, argb, glow);
            }
            return new Views(textures);
        } catch (IOException | RuntimeException e) {
            log.warn("Cannot read {}, rendering it again: {}", file, e.toString());
            return null;
        }
    }

    static void writeViews(Path file, Views views) {
        try {
            Files.createDirectories(file.getParent());
            Path part = file.resolveSibling(file.getFileName() + ".part");
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(part)))) {
                Texture first = views.pick(0, 0, 0);
                int size = first.size();
                out.writeInt(views.frames());
                out.writeInt(views.directions());
                out.writeInt(size);
                for (int f = 0; f < views.frames(); f++) for (int d = 0; d < views.directions(); d++) {
                    Texture t = views.pick(f, 0, 2 * Math.PI * d / views.directions());
                    for (int v = 0; v < size; v++) for (int u = 0; u < size; u++) out.writeInt(t.argb(u, v));
                    for (int v = 0; v < size; v++) for (int u = 0; u < size; u++) out.writeByte((int) Math.round(t.glow(u, v) * 255));
                }
            }
            Files.move(part, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Cannot keep {} in the cache: {}", file, e.toString());
        }
    }

    private static BufferedImage readPicture(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage read = ImageIO.read(in);
            if (read == null) return null;
            BufferedImage image = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
            image.getGraphics().drawImage(read, 0, 0, null);
            return image;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writePicture(Path file, BufferedImage image) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                ImageIO.write(image, "png", out);
            }
        } catch (IOException e) {
            log.warn("Cannot keep {} in the cache: {}", file, e.toString());
        }
    }
}
