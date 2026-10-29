package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.raycast.Door;
import eu.amadeos.games.rivet.domain.Ammo;
import eu.amadeos.games.rivet.domain.Chip;
import eu.amadeos.games.rivet.domain.LevelData;
import eu.amadeos.games.rivet.domain.Pickup;
import eu.amadeos.games.rivet.domain.Player;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.domain.Weapon;
import eu.amadeos.games.rivet.domain.World;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

import static eu.amadeos.games.rivet.ui.game.Look.colour;
import static eu.amadeos.games.rivet.ui.game.Look.font;

/**
 * The overlay: the suit's display with health, plating, the tool and its ammunition, the chips, the level and
 * what is left in it, the messages, the crosshair and the red edge of a hit; the map on Tab; and the cards of
 * the title, the setup, the pause, the end of a level, the fall of the engineer and the end of the run.
 * Drawn after the world, at the screen's full size, so the letters are sharp in every render mode.
 */
final class Hud {
    static final double HEIGHT = 400;
    static final double CARD_ROW = 24;

    private final Messages msg;

    Hud(Messages msg) {
        this.msg = msg;
    }

    private static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void panel(Graphics2D g, double x, double y, double w, double h) {
        g.setPaint(Look.PANEL_FILL);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 8, 8));
        g.setPaint(Look.PANEL_EDGE);
        g.setStroke(new BasicStroke(0.8f));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 8, 8));
    }

    private static void text(Graphics2D g, String s, double x, double y, float size, boolean bold, Color c) {
        g.setFont(font(size, bold));
        g.setPaint(c);
        g.drawString(s, (float) x, (float) y);
    }

    private static double width(Graphics2D g, String s, float size, boolean bold) {
        return g.getFontMetrics(font(size, bold)).stringWidth(s);
    }

    private static void centred(Graphics2D g, String s, double cx, double y, float size, boolean bold, Color c) {
        text(g, s, cx - width(g, s, size, bold) / 2, y, size, bold, c);
    }

    String levelName(String id) {
        return msg.get("level." + id);
    }

    // ---- while playing ------------------------------------------------------------------------------

    // --8<-- [start:play]
    void play(Graphics2D g, int w, World world, Effects fx, double time) {
        smooth(g);
        Player p = world.player();
        hurt(g, w, p, fx);
        if (p.alive()) crosshair(g, w, fx);
        level(g, world);
        chips(g, w, p);
        messages(g, w, fx);
        suit(g, p, time);
        tool(g, w, p);
    }
    // --8<-- [end:play]

    private void crosshair(Graphics2D g, int w, Effects fx) {
        double cx = w / 2.0, cy = HEIGHT / 2;
        g.setStroke(new BasicStroke(1.2f));
        g.setPaint(new Color(232, 246, 255, 210));
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            g.draw(new Line2D.Double(cx + d[0] * 4, cy + d[1] * 4, cx + d[0] * 9, cy + d[1] * 9));
        }
        if (fx.hitMarker() > 0) {
            g.setPaint(colour(0xffc060, (int) (fx.hitMarker() * 230)));
            for (int[] d : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
                g.draw(new Line2D.Double(cx + d[0] * 11, cy + d[1] * 11, cx + d[0] * 15, cy + d[1] * 15));
            }
        }
    }

    private void level(Graphics2D g, World world) {
        String where = msg.format("hud.level", world.level().id());
        String name = levelName(world.level().id());
        String counts = msg.format("hud.counts", world.kills(), world.machinesAtStart(), world.secretsFound(), world.secretsTotal());
        double nameWidth = width(g, name, 13, true);
        double w = Math.max(width(g, where, 9, true), nameWidth + 14 + width(g, counts, 9, false)) + 20;
        panel(g, 10, 10, w, 38);
        text(g, where, 20, 24, 9, true, Look.ICE);
        text(g, name, 20, 40, 13, true, Look.TEXT);
        text(g, counts, 34 + nameWidth, 40, 9, false, new Color(0xb5c3cc));
    }

    private void chips(Graphics2D g, int w, Player p) {
        double x0 = w - 106;
        panel(g, x0, 10, 96, 30);
        Chip[] all = Chip.values();
        for (int i = 0; i < all.length; i++) {
            double x = x0 + 12 + i * 28, y = 17;
            int rgb = Look.chip(all[i]);
            if (p.chips().contains(all[i])) {
                g.setPaint(new Color(rgb));
                g.fill(new RoundRectangle2D.Double(x, y, 18, 16, 4, 4));
                g.setPaint(new Color(0x0b1117));
                for (int k = 0; k < 3; k++) g.fill(new Rectangle2D.Double(x + 4 + k * 4, y + 5, 2, 6));
            } else {
                g.setPaint(colour(rgb, 120));
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
                g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, 17, 15, 4, 4));
            }
        }
    }

    private void messages(Graphics2D g, int w, Effects fx) {
        List<Effects.Message> list = fx.messages();
        double y = 58;
        for (Effects.Message m : list) {
            double age = fx.time() - m.born();
            int alpha = (int) (255 * Math.min(1, (Effects.MESSAGE_SECONDS - age) * 2));
            double mw = width(g, m.text(), 10, false);
            g.setPaint(new Color(11, 17, 23, alpha * 140 / 255));
            g.fill(new RoundRectangle2D.Double(w / 2.0 - mw / 2 - 10, y, mw + 20, 18, 9, 9));
            text(g, m.text(), w / 2.0 - mw / 2, y + 13, 10, false, colour(m.colour(), alpha));
            y += 22;
        }
    }

    private void suit(Graphics2D g, Player p, double time) {
        panel(g, 10, 344, 196, 46);
        boolean low = p.health() <= 25 && p.alive();
        Color health = low && ((int) (time * 3)) % 2 == 0 ? Look.WARN : Look.HEALTH;
        g.setPaint(health);
        g.fill(new Rectangle2D.Double(22, 357, 6, 18));
        g.fill(new Rectangle2D.Double(16, 363, 18, 6));
        text(g, Integer.toString(p.health()), 40, 377, 26, true, low ? Look.WARN : Look.TEXT);
        text(g, msg.get("hud.suit"), 41, 386, 8, false, Look.DIM);
        Path2D.Double shield = new Path2D.Double();
        shield.moveTo(130, 356);
        shield.lineTo(144, 360);
        shield.lineTo(143, 372);
        shield.lineTo(130, 379);
        shield.lineTo(117, 372);
        shield.lineTo(116, 360);
        shield.closePath();
        g.setPaint(p.plating() > 0 ? Look.PLATING : Look.FAINT);
        g.fill(shield);
        text(g, Integer.toString(p.plating()), 150, 375, 20, true, Look.TEXT);
        text(g, msg.get("hud.plating"), 150, 386, 8, false, Look.DIM);
    }

    private void tool(Graphics2D g, int w, Player p) {
        double x0 = w - 190;
        panel(g, x0, 344, 180, 46);
        for (Weapon t : Weapon.values()) {
            double x = x0 + 10 + (t.slot() - 1) * 12, y = 352;
            boolean owned = p.owns(t), selected = t == p.weapon();
            g.setPaint(selected ? Look.SELECTED : owned ? Look.OWNED : Look.MISSING);
            g.fill(new RoundRectangle2D.Double(x, y, 10, 11, 3, 3));
            text(g, Integer.toString(t.slot()), x + 3, y + 8.5, 8, true, selected ? new Color(0x0b1117) : owned ? new Color(0xd8e2e8) : Look.FAINT);
        }
        String name = msg.get(p.weapon().key());
        // long names (the German welder) get a smaller size, so they stay clear of the count
        text(g, name, x0 + 12, 381, width(g, name, 9, false) > 74 ? 7.5f : 9f, false, new Color(0xcfdbe2));
        Ammo a = p.weapon().ammo();
        if (a == null) return;
        String count = Integer.toString(p.ammo(a));
        boolean empty = p.ammo(a) < p.weapon().perShot();
        double cw = width(g, count, 26, true), right = x0 + 170;
        String max = "/ " + a.max();
        double mw = width(g, max, 9, false);
        text(g, count, right - mw - 4 - cw, 377, 26, true, empty ? Look.WARN : Look.TEXT);
        text(g, max, right - mw, 377, 9, false, Look.DIM);
        text(g, msg.get(a.key()).toUpperCase(msg.locale()), right - mw - 4 - cw, 386, 8, false, Look.DIM);
    }

    /** Red at the edges when hit, brightest towards where the hit came from; white for a moment on a pickup. */
    private void hurt(Graphics2D g, int w, Player p, Effects fx) {
        double h = p.hurt();
        if (!p.alive()) h = 1;
        if (h > 0) {
            g.setPaint(new RadialGradientPaint((float) (w / 2.0), 200f, (float) (w * 0.62), new float[]{0.45f, 1f},
                    new Color[]{new Color(255, 30, 20, 0), new Color(200, 20, 10, (int) Math.min(200, h * 190))}));
            g.fill(new Rectangle2D.Double(0, 0, w, HEIGHT));
        }
        if (fx.hurtShown() > 0 && p.alive()) {
            // the angle on the screen: 0 ahead (up), clockwise
            double rel = fx.hurtFrom() - p.angle();
            double a = Math.toDegrees(Math.atan2(Math.sin(rel), Math.cos(rel)));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setPaint(new Color(255, 70, 50, (int) (fx.hurtShown() * 200)));
            g.draw(new Arc2D.Double(w / 2.0 - 60, 140, 120, 120, 90 - a - 20, 40, Arc2D.OPEN));
        }
        if (p.glint() > 0) {
            g.setPaint(new Color(255, 245, 210, (int) (p.glint() * 60)));
            g.fill(new Rectangle2D.Double(0, 0, w, HEIGHT));
        }
    }

    // ---- the map ------------------------------------------------------------------------------------

    // --8<-- [start:map]
    /** The cells seen so far, centred on the player and turned so that ahead is up. */
    void map(Graphics2D g, int w, World world, Viewport view) {
        smooth(g);
        g.setPaint(new Color(4, 9, 14, 190));
        g.fill(new Rectangle2D.Double(0, 0, w, HEIGHT));
        LevelData level = world.level();
        Player p = world.player();
        double cell = 9;
        AffineTransform was = g.getTransform();
        g.translate(w / 2.0, HEIGHT / 2);
        g.rotate(-p.angle() - Math.PI / 2);
        g.scale(cell, cell);
        g.translate(-p.x(), -p.y());
        for (int y = 0; y < level.height(); y++) for (int x = 0; x < level.width(); x++) {
            if (!view.explored(x, y)) continue;
            char c = level.at(x, y);
            Door door = world.map().door(x, y);
            if (LevelData.wall(c)) {
                g.setPaint(c == 'X' ? new Color(0x56e08a) : new Color(0x6d8a9c));
                g.fill(new Rectangle2D.Double(x, y, 1, 1));
            } else if (door != null && !world.secret(door)) {
                Chip lock = world.lock(door);
                g.setPaint(lock == null ? new Color(0xd8a422) : new Color(Look.chip(lock)));
                if (door.alongX()) g.fill(new Rectangle2D.Double(x, y + 0.35, 1, 0.3));
                else g.fill(new Rectangle2D.Double(x + 0.35, y, 0.3, 1));
            } else {
                g.setPaint(new Color(0x1d2a33));
                g.fill(new Rectangle2D.Double(x, y, 1, 1));
            }
        }
        for (Pickup k : world.pickups()) {
            if (k.taken() || k.kind().chip() == null || !view.explored((int) k.x(), (int) k.y())) continue;
            g.setPaint(new Color(Look.chip(k.kind().chip())));
            g.fill(new java.awt.geom.Ellipse2D.Double(k.x() - 0.25, k.y() - 0.25, 0.5, 0.5));
        }
        g.setTransform(was);
        // the player: an arrow pointing up
        Path2D.Double arrow = new Path2D.Double();
        arrow.moveTo(w / 2.0, HEIGHT / 2 - 6);
        arrow.lineTo(w / 2.0 + 4.5, HEIGHT / 2 + 5);
        arrow.lineTo(w / 2.0, HEIGHT / 2 + 2.5);
        arrow.lineTo(w / 2.0 - 4.5, HEIGHT / 2 + 5);
        arrow.closePath();
        g.setPaint(Look.GOLD);
        g.fill(arrow);
        centred(g, levelName(level.id()), w / 2.0, 24, 13, true, Look.TEXT);
        centred(g, msg.get("hud.map.hint"), w / 2.0, 388, 9, false, Look.DIM);
    }
    // --8<-- [end:map]

    // ---- cards --------------------------------------------------------------------------------------

    private static void veil(Graphics2D g, int w, int alpha) {
        g.setPaint(new Color(2, 6, 10, alpha));
        g.fill(new Rectangle2D.Double(0, 0, w, HEIGHT));
    }

    private static void card(Graphics2D g, double x, double y, double w, double h) {
        g.setPaint(new GradientPaint(0, (float) y, new Color(18, 28, 36, 235), 0, (float) (y + h), new Color(8, 13, 18, 235)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));
        g.setPaint(new Color(159, 212, 236, 90));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 12, 12));
    }

    /** The name of the game in big letters, with a line under it. */
    private void logo(Graphics2D g, int w, double y) {
        String name = msg.get("title.name");
        Font big = font(64, true);
        FontMetrics m = g.getFontMetrics(big);
        double x = w / 2.0 - m.stringWidth(name) / 2.0;
        g.setFont(big);
        g.setPaint(new Color(0, 0, 0, 150));
        g.drawString(name, (float) x + 3, (float) y + 3);
        g.setPaint(new GradientPaint(0, (float) (y - 50), new Color(0xe6f8ff), 0, (float) y, new Color(0x7fb6d0)));
        g.drawString(name, (float) x, (float) y);
        g.setPaint(new Color(0xf2a332));
        g.fill(new Rectangle2D.Double(w / 2.0 - 70, y + 10, 140, 3));
        centred(g, msg.get("title.subtitle"), w / 2.0, y + 30, 11, false, Look.ICE);
    }

    void title(Graphics2D g, int w, double time, double loaded) {
        smooth(g);
        veil(g, w, 90);
        logo(g, w, 150);
        if (loaded < 1) {
            double bw = 220, x = w / 2.0 - bw / 2, y = 300;
            g.setPaint(new Color(11, 17, 23, 200));
            g.fill(new RoundRectangle2D.Double(x, y, bw, 8, 8, 8));
            g.setPaint(Look.ICE);
            g.fill(new RoundRectangle2D.Double(x, y, Math.max(8, bw * loaded), 8, 8, 8));
            centred(g, msg.get("title.loading"), w / 2.0, y + 26, 10, false, Look.DIM);
        } else if (((int) (time * 1.6)) % 2 == 0) {
            centred(g, msg.get("title.start"), w / 2.0, 312, 13, true, Look.TEXT);
        }
        centred(g, msg.get("title.keys"), w / 2.0, 376, 9, false, Look.DIM);
    }

    // --8<-- [start:setup]
    void setup(Graphics2D g, int w, SetupCard card) {
        smooth(g);
        veil(g, w, 120);
        double cw = 330, x = w / 2.0 - cw / 2, y = 112;
        card(g, x, y, cw, 176);
        centred(g, msg.get("setup.title"), w / 2.0, y + 30, 15, true, Look.TEXT);
        String[] labels = {msg.get("setup.difficulty"), msg.get("setup.section"), ""};
        String[] values = {msg.get(card.difficulty().key()), msg.format("setup.section.value", card.section(), msg.get("section." + card.section())),
                msg.get("setup.start")};
        for (int i = 0; i < 3; i++) {
            double ry = rowY(i);
            boolean selected = card.selected().ordinal() == i;
            if (selected) {
                g.setPaint(new Color(242, 163, 50, 50));
                g.fill(new RoundRectangle2D.Double(x + 12, ry - 16, cw - 24, CARD_ROW, 6, 6));
            }
            if (i == 2) {
                centred(g, values[i], w / 2.0, ry, 13, true, selected ? Look.SELECTED : Look.TEXT);
            } else {
                text(g, labels[i], x + 24, ry, 11, false, Look.DIM);
                String v = (selected ? "‹  " : "") + values[i] + (selected ? "  ›" : "");
                text(g, v, x + cw - 24 - width(g, v, 11, true), ry, 11, true, selected ? Look.SELECTED : Look.TEXT);
            }
        }
        if (card.reached() < Run.SECTIONS) centred(g, msg.get("setup.more"), w / 2.0, y + 162, 9, false, Look.FAINT);
    }
    // --8<-- [end:setup]

    /** The baseline of a row of the setup card. */
    static double rowY(int row) {
        return 112 + 70 + row * (CARD_ROW + 8);
    }

    /** The row of the setup card under a point, or -1. */
    static int setupRowAt(int w, double x, double y) {
        if (Math.abs(x - w / 2.0) > 165) return -1;
        for (int i = 0; i < 3; i++) if (y >= rowY(i) - 16 && y < rowY(i) - 16 + CARD_ROW) return i;
        return -1;
    }

    void pause(Graphics2D g, int w) {
        smooth(g);
        veil(g, w, 140);
        centred(g, msg.get("pause.title"), w / 2.0, 186, 28, true, Look.TEXT);
        centred(g, msg.get("pause.hint"), w / 2.0, 216, 11, false, Look.DIM);
    }

    // --8<-- [start:done]
    void levelDone(Graphics2D g, int w, World world, double shown) {
        smooth(g);
        veil(g, w, 150);
        double cw = 330, x = w / 2.0 - cw / 2, y = 92;
        card(g, x, y, cw, 220);
        centred(g, msg.get("done.title"), w / 2.0, y + 28, 11, true, Look.ICE);
        centred(g, levelName(world.level().id()), w / 2.0, y + 52, 18, true, Look.TEXT);
        int seconds = (int) world.time();
        String[][] rows = {
                {msg.get("done.time"), msg.format("done.time.value", seconds / 60, String.format("%02d", seconds % 60),
                        (int) world.level().par() / 60, String.format("%02d", (int) world.level().par() % 60))},
                {msg.get("done.machines"), world.kills() + " / " + world.machinesAtStart()},
                {msg.get("done.secrets"), world.secretsFound() + " / " + world.secretsTotal()},
                {msg.get("done.points"), Integer.toString(world.points())},
                {msg.get("done.bonus"), "+" + world.bonus()}};
        for (int i = 0; i < rows.length; i++) {
            if (shown < 0.3 + i * 0.25) break;
            double ry = y + 88 + i * 22;
            text(g, rows[i][0], x + 30, ry, 11, false, Look.DIM);
            text(g, rows[i][1], x + cw - 30 - width(g, rows[i][1], 11, true), ry, 11, true, i == 4 ? Look.GOLD : Look.TEXT);
        }
        if (shown > 1.6) centred(g, msg.get("done.next"), w / 2.0, y + 204, 10, false, Look.DIM);
    }
    // --8<-- [end:done]

    void dead(Graphics2D g, int w, double shown) {
        smooth(g);
        if (shown < 1) return;
        centred(g, msg.get("dead.title"), w / 2.0, 176, 26, true, new Color(0xffd0c8));
        centred(g, msg.get("dead.retry"), w / 2.0, 210, 12, false, Look.TEXT);
        centred(g, msg.get("dead.giveup"), w / 2.0, 230, 10, false, Look.DIM);
    }

    void won(Graphics2D g, int w, Run run, double shown) {
        smooth(g);
        veil(g, w, 170);
        logo(g, w, 120);
        centred(g, msg.get("won.title"), w / 2.0, 200, 18, true, Look.GOLD);
        centred(g, msg.get("won.text"), w / 2.0, 226, 11, false, Look.TEXT);
        centred(g, msg.format("won.score", run.score(), msg.get(run.difficulty().key())), w / 2.0, 262, 14, true, Look.TEXT);
        if (shown > 2) centred(g, msg.get("won.next"), w / 2.0, 330, 10, false, Look.DIM);
    }
}
