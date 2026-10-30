package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.raycast.Billboard;
import eu.amadeos.gameengine.raycast.Camera;
import eu.amadeos.gameengine.raycast.LightMap;
import eu.amadeos.gameengine.raycast.Raycaster;
import eu.amadeos.gameengine.raycast.Views;
import eu.amadeos.games.rivet.domain.LevelData;
import eu.amadeos.games.rivet.domain.Machine;
import eu.amadeos.games.rivet.domain.MachineKind;
import eu.amadeos.games.rivet.domain.Pickup;
import eu.amadeos.games.rivet.domain.Player;
import eu.amadeos.games.rivet.domain.Projectile;
import eu.amadeos.games.rivet.domain.Prop;
import eu.amadeos.games.rivet.domain.Weapon;
import eu.amadeos.games.rivet.domain.World;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The player's view of a level: the {@link Raycaster}'s picture of the station with the machines, pickups
 * and effects as billboards, then the tool in the hand, bobbing as the player walks, kicking back when it
 * fires, lit by the room it is in. It also remembers which cells the player has seen, for the map.
 */
final class Viewport {
    static final int HEIGHT = 400;
    /** Where the muzzle of a held tool is on the 640 × 400 view, for the flash and the welder's arc. */
    static final double MUZZLE_X = 381, MUZZLE_Y = 290;

    private final Camera camera = new Camera();
    private final List<Billboard> boards = new ArrayList<>();
    private final float[] light = new float[3];
    private final Map<Long, BufferedImage> tinted = new HashMap<>();
    private final Random jitter = new Random(5);
    private final BufferedImage sky;
    private Raycaster raycaster;
    private World world;
    private LightMap lights;
    private boolean[] explored;
    private double exploredAt = -1;

    Viewport(BufferedImage sky) {
        this.sky = sky;
    }

    /** Starts showing a level. */
    void show(World world) {
        this.world = world;
        LevelData level = world.level();
        double[] a = level.ambient();
        lights = new LightMap(world.map(), a[0], a[1], a[2], world.lights());
        explored = new boolean[level.width() * level.height()];
        exploredAt = -1;
        if (raycaster != null) fog(raycaster);
    }

    World world() {
        return world;
    }

    Camera camera() {
        return camera;
    }

    private void fog(Raycaster r) {
        r.setFog(world.level().fog(), world.level().fogDensity());
        r.setSky(sky);
    }

    /** The cells the player has seen, row by row, for the map. */
    boolean explored(int x, int y) {
        return explored != null && x >= 0 && y >= 0 && x < world.level().width() && y < world.level().height()
                && explored[y * world.level().width() + x];
    }

    // ---- drawing ------------------------------------------------------------------------------------

    // --8<-- [start:render]
    /**
     * Draws the view {@code width} pixels wide. {@code dying} is the seconds since the player fell (the eye
     * sinks), 0 while alive; {@code withTool} false hides the hand, as on the title, where {@code turn} slowly
     * turns the view.
     */
    void render(Graphics2D g, int width, Effects fx, Art art, double dying, boolean withTool, double turn) {
        if (world == null) return;
        if (raycaster == null || raycaster.width() != width) {
            raycaster = new Raycaster(width, HEIGHT);
            fog(raycaster);
        }
        Player p = world.player();
        double shake = fx.shake();
        camera.x = p.x();
        camera.y = p.y();
        camera.angle = p.angle() + turn + (shake > 0 ? (jitter.nextDouble() - 0.5) * shake * 0.03 : 0);
        double bob = Math.sin(p.walked() * Math.PI / 0.75);
        camera.eye = dying > 0 ? Math.max(0.12, 0.5 - dying * 0.6) : 0.5 + Math.abs(bob) * 0.012 + (jitter.nextDouble() - 0.5) * shake * 0.02;
        lights.beginFrame();
        fx.lights(world, lights);
        billboards(fx, art);
        raycaster.render(world.map(), lights, camera, boards);
        g.drawImage(raycaster.frame(), 0, 0, null);
        explore(fx.time());
        if (withTool && dying == 0 && art != null) tool(g, width, p, fx, art, bob);
    }
    // --8<-- [end:render]

    // --8<-- [start:billboards]
    private void billboards(Effects fx, Art art) {
        boards.clear();
        if (art == null) return;
        double time = fx.time();
        for (Machine m : world.machines()) {
            if (!m.alive()) continue;
            MachineKind k = m.kind();
            Billboard b = new Billboard(art.machines.get(k), m.x(), m.y(), Art.size(k));
            b.facing = m.facing();
            b.flash = m.flash() * 0.8;
            b.shadow = k == MachineKind.DRONE ? 0.35 : 0.55;
            b.frame = frame(m, time);
            b.lift = k == MachineKind.DRONE ? 0.25 + Math.sin(time * 3 + m.x() * 2) * 0.03 : -0.01 * b.size;
            boards.add(b);
        }
        for (Pickup p : world.pickups()) {
            if (p.taken()) continue;
            boolean chip = p.kind().chip() != null;
            Billboard b = new Billboard(art.pickups.get(p.kind()), p.x(), p.y(), chip ? 0.5 : 0.55);
            b.facing = time * 1.6;
            b.lift = chip ? 0.08 + Math.sin(time * 2.5 + p.x()) * 0.03 : 0;
            b.shadow = 0.45;
            boards.add(b);
        }
        for (Prop p : world.props()) {
            if (p.gone()) continue;
            Views v = switch (p.kind()) {
                case CRATE -> art.crate;
                case CANISTER -> art.canister;
                case SCRAP -> art.scrap;
                case BIG_SCRAP -> art.bigScrap;
            };
            double size = switch (p.kind()) {
                case CRATE -> 1.6;
                case CANISTER -> 1.4;
                case SCRAP -> 1.0;
                case BIG_SCRAP -> 1.5;
            };
            Billboard b = new Billboard(v, p.x(), p.y(), size);
            b.facing = p.x() * 3 + p.y() * 7;   // each heap of scrap lies a different way
            b.shadow = p.kind() == Prop.Kind.CRATE || p.kind() == Prop.Kind.CANISTER ? 0.5 : 0;
            boards.add(b);
        }
        for (Projectile pr : world.projectiles()) {
            double size = switch (pr.kind()) {
                case SPARK -> 0.2;
                case PLASMA, BOLT -> 0.3;
                case ICE_BLOCK -> 0.45;
                case CHARGE -> 0.6;
            };
            Billboard b = new Billboard(art.projectiles.get(pr.kind()), pr.x(), pr.y(), size);
            b.lift = pr.kind() == Projectile.Kind.CHARGE ? pr.z() - 0.1 : pr.z() - size / 2;
            b.facing = pr.angle();
            boards.add(b);
        }
        double dx = camera.dirX(), dy = camera.dirY();
        for (Effects.Sprite s : fx.sprites()) {
            int frames = s.views().frames();
            int frame = Math.min(frames - 1, (int) ((time - s.born()) / s.seconds() * frames));
            // pulled a little towards the eye, so sparks on a wall are not hidden in it
            Billboard b = new Billboard(s.views(), s.x() - dx * 0.08, s.y() - dy * 0.08, s.size());
            b.frame = frame;
            b.lift = s.lift() > 0 ? s.lift() - s.size() / 2 : 0;
            boards.add(b);
        }
    }
    // --8<-- [end:billboards]

    /** The picture of a machine now: its attack while winding up, its steps while walking. */
    static int frame(Machine m, double time) {
        MachineKind k = m.kind();
        if (m.state() == Machine.State.WINDUP) return Art.attackFrame(k);
        return switch (k) {
            case CRAWLER, LOADER -> m.moving() > 0 ? (int) (m.moving() * (k == MachineKind.CRAWLER ? 10 : 6)) % 4 : 0;
            case FOREMAN -> m.state() == Machine.State.IDLE ? 0 : (int) (time * 2) % 2;
            default -> 0;
        };
    }

    private void explore(double time) {
        if (time - exploredAt < 0.2 && exploredAt >= 0) return;
        exploredAt = time;
        LevelData level = world.level();
        int cx = (int) camera.x, cy = (int) camera.y, r = 7;
        for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++) {
            if (x < 0 || y < 0 || x >= level.width() || y >= level.height() || explored[y * level.width() + x]) continue;
            if (Math.hypot(x + 0.5 - camera.x, y + 0.5 - camera.y) > r) continue;
            // a wall is seen if the cell in front of it, towards the eye, is
            double tx = x + 0.5 - Math.signum(x + 0.5 - camera.x) * 0.55, ty = y + 0.5 - Math.signum(y + 0.5 - camera.y) * 0.55;
            if (world.map().sees(camera.x, camera.y, LevelData.wall(level.at(x, y)) ? tx : x + 0.5, LevelData.wall(level.at(x, y)) ? ty : y + 0.5)) {
                explored[y * level.width() + x] = true;
            }
        }
    }

    // ---- the tool in the hand -----------------------------------------------------------------------

    // --8<-- [start:tool]
    private void tool(Graphics2D g, int width, Player p, Effects fx, Art art, double bob) {
        Weapon w = p.weapon();
        boolean firing = switch (w) {
            case ICE_PICK -> p.sinceShot() < 0.22;
            case BLASTING_CHARGE -> p.sinceShot() < 0.3;
            case ARC_WELDER -> p.firing();
            default -> fx.muzzle() > 0.35;
        };
        double kick = switch (w) {
            case RIVET_GUN -> 7;
            case SCATTER_RIVETER -> 16;
            case PLASMA_CUTTER -> 10;
            case ARC_WELDER -> p.firing() ? 1.5 : 0;
            default -> 0;
        };
        double recoil = Math.max(0, 1 - p.sinceShot() * 7) * kick;
        double ox = (width - 640) / 2.0 + Math.sin(p.walked() * Math.PI / 1.5) * 5;
        double oy = Math.abs(bob) * 5 + recoil + fx.raise() * fx.raise() * 200;
        lights.sample(p.x(), p.y(), light);
        BufferedImage picture = tinted(art.tools.get(w)[firing ? 1 : 0], w, firing);
        g.drawImage(picture, (int) Math.round(Art.TOOL_X + ox), (int) Math.round(Art.TOOL_Y + oy), Art.TOOL_W, Art.TOOL_H, null);
        double mx = MUZZLE_X + ox, my = MUZZLE_Y + oy;
        if (w == Weapon.ARC_WELDER && p.firing()) arc(g, mx, my + 2, width, fx);
        else if (fx.muzzle() > 0 && (w.kind() == Weapon.Kind.HITSCAN || w == Weapon.PLASMA_CUTTER)) {
            flash(g, mx, my, fx.muzzle(), w == Weapon.PLASMA_CUTTER ? 0x7fe8ff : 0xffd27a, w == Weapon.SCATTER_RIVETER ? 26 : 18);
        }
    }
    // --8<-- [end:tool]

    /** The tool's picture in the light of the room, in steps, so only a few pictures are ever made. */
    private BufferedImage tinted(BufferedImage picture, Weapon w, boolean firing) {
        int r = step(light[0]), gr = step(light[1]), b = step(light[2]);
        long key = ((long) w.ordinal() << 32) | (firing ? 1L << 24 : 0) | (r << 16) | (gr << 8) | b;
        return tinted.computeIfAbsent(key, k -> {
            float[] scales = {r / 10f, gr / 10f, b / 10f, 1f};
            return new RescaleOp(scales, new float[4], null).filter(picture, null);
        });
    }

    /** A light level as tenths, between 0.4 and 1.4: the tool is never quite dark. */
    private static int step(float level) {
        return Math.max(4, Math.min(14, Math.round(level * 1.15f * 10)));
    }

    private static void flash(Graphics2D g, double x, double y, double strength, int rgb, double radius) {
        Composite was = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, strength * 1.4)));
        g.setPaint(new RadialGradientPaint((float) x, (float) y, (float) radius, new float[]{0, 0.35f, 1},
                new Color[]{new Color(255, 255, 255, 235), Look.colour(rgb, 170), Look.colour(rgb, 0)}));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        g.setComposite(was);
    }

    /** The welder's arc: a crackling line from the tip to where it touches, blue with a white core. */
    private void arc(Graphics2D g, double x, double y, int width, Effects fx) {
        double[] end = raycaster.project(camera, fx.beamX(), fx.beamY(), 0.45);
        double ex = end == null ? width / 2.0 : end[0], ey = end == null ? HEIGHT / 2.0 : end[1];
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x, y);
        int segments = 7;
        for (int i = 1; i < segments; i++) {
            double t = (double) i / segments;
            path.lineTo(x + (ex - x) * t + (jitter.nextDouble() - 0.5) * 12, y + (ey - y) * t + (jitter.nextDouble() - 0.5) * 12);
        }
        path.lineTo(ex, ey);
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new Color(80, 170, 255, 110));
        g.draw(path);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new Color(235, 248, 255));
        g.draw(path);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        flash(g, x, y, 0.8, 0x7fc8ff, 14);
        flash(g, ex, ey, 0.9, 0x9fd8ff, 16);
    }
}
