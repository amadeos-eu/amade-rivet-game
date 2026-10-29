package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.gameengine.raycast.model.Material;
import eu.amadeos.gameengine.raycast.model.Model;
import eu.amadeos.games.rivet.domain.PickupKind;
import eu.amadeos.games.rivet.domain.Weapon;

import static eu.amadeos.gameengine.raycast.model.Sdf.box;
import static eu.amadeos.gameengine.raycast.model.Sdf.capsule;
import static eu.amadeos.gameengine.raycast.model.Sdf.cone;
import static eu.amadeos.gameengine.raycast.model.Sdf.cylinder;
import static eu.amadeos.gameengine.raycast.model.Sdf.length;
import static eu.amadeos.gameengine.raycast.model.Sdf.smoothUnion;
import static eu.amadeos.gameengine.raycast.model.Sdf.sphere;
import static eu.amadeos.gameengine.raycast.model.Sdf.torus;

/**
 * The machines, pickups and tools of Rivet as 3D models built from distance functions. Machines face +z
 * and stand on y = -1 (the billboard's floor); tools are held with the barrel along +z in front of the camera.
 */
final class Models {
    // the parts' materials
    static final int STEEL = 1, DARK = 2, ORANGE = 3, RED_EYE = 4, BLUE_FLAME = 5, BRASS = 6, HAZARD = 7, AMBER = 8,
            DRILL = 9, OLIVE = 10, TANK = 11, GREEN = 12, RUBBER = 13, LEATHER = 14, SUIT = 15, CUFF = 16, BODY = 17,
            ROTOR = 18, WHITE = 19, CROSS = 20, BLUE = 21, VIOLET = 22, VIOLET_GLOW = 23, RED = 24, ICE = 25, CYAN = 26,
            YELLOW = 27, RUST = 28, CHIP_GREEN = 29, CHIP_AMBER = 30, CHIP_VIOLET = 31, GOLD = 32, HOT = 33;

    private Models() {
    }

    /** A model whose materials come from the shared palette; the tool builders report parts through {@link #add}. */
    abstract static class Part extends Model {
        final void add(double distance, int material) {
            part(distance, material);
        }

        @Override
        protected Material material(int part, double x, double y, double z) {
            return look(part, x, y, z);
        }
    }

    static Material look(int part, double x, double y, double z) {
        return switch (part) {
            case STEEL -> Material.metal(0x9aa3ab);
            case DARK -> new Material(0x353b41, 0.35, 0);
            case ROTOR -> Material.matte(0x23272b);
            case ORANGE -> Material.paint(0xd8782a);
            case BODY -> Math.abs(y - 0.02) < 0.055 ? Material.paint(0xd8782a) : new Material(0xa8b0b8, 0.7, 0);
            case RED_EYE -> length(x, y - 0.14) < 0.05 ? Material.light(0xffd8c0) : Material.light(0xff2a18);
            case BLUE_FLAME -> Material.light(0xbfefff);
            case AMBER -> Material.light(0xffb030);
            case GREEN -> Material.light(0x56e08a);
            case CYAN -> Material.light(0x7fe8ff);
            case HOT -> Material.light(0xffe8a0);
            case VIOLET_GLOW -> Material.light(0xb68cff);
            case RED -> Material.light(0xff3a2a);
            case CHIP_GREEN -> Material.light(0x4fdc84);
            case CHIP_AMBER -> Material.light(0xf2a332);
            case CHIP_VIOLET -> Material.light(0xa77cf0);
            case BRASS -> Material.metal(0xd2a550);
            case GOLD -> Material.metal(0xe0b85a);
            case HAZARD -> Math.floorMod((int) Math.floor((x + y + z) * 9), 2) == 0 ? Material.paint(0xd9a423) : Material.paint(0x1e1f21);
            case DRILL -> Math.floorMod((int) Math.floor(z * 22 + Math.atan2(x, y + 0.62) * 1.6), 3) == 0
                    ? new Material(0x4a5157, 0.5, 0) : Material.metal(0xc3cad0);
            case OLIVE -> Material.matte(0x5c6a40);
            case TANK -> Math.abs(y + 0.62) < 0.035 || Math.abs(y + 0.08) < 0.035 ? Material.metal(0x8e98a2) : new Material(0x2d6a9a, 0.6, 0);
            case RUBBER -> Material.matte(0x1f2225);
            case LEATHER -> Material.matte(0x7a5232);
            case SUIT -> new Material(0x3d4c5c, 0.2, 0);
            case CUFF -> Material.paint(0xd27a2a);
            case WHITE -> Material.paint(0xe4e8ec);
            case CROSS -> Material.light(0x52e08a);
            case BLUE -> new Material(0x3f7fc0, 0.6, 0);
            case VIOLET -> new Material(0x6a4fb0, 0.6, 0);
            case YELLOW -> Material.paint(0xd2a12a);
            case ICE -> new Material(0x9fd4ec, 0.9, 0.25);
            case RUST -> Material.matte(0x6a3a26);
            default -> Material.paint(0xff00ff);
        };
    }

    // ---- machines -----------------------------------------------------------------------------------

    /** The welder drone: a ball with one red eye, two rotors and a torch underneath. Frame 1 is about to fire. */
    static Model drone(int frame) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(sphere(x, (y - 0.12) / 0.92, z, 0.40) * 0.92, BODY);
                part(torus(length(x, y - 0.14), z - 0.33, 0.15, 0.045), DARK);
                part(sphere(x, y - 0.14, z - 0.28, 0.14), RED_EYE);
                double ax = Math.abs(x);
                part(capsule(ax, y, z, 0.32, 0.22, 0, 0.74, 0.42, 0, 0.045), DARK);
                part(cylinder(length(ax - 0.76, z), y - 0.44, 0.06, 0.05), DARK);
                part(cylinder(length(ax - 0.76, z), y - 0.50, 0.24, 0.012), ROTOR);
                part(box(ax - 0.36, y - 0.02, z, 0.05, 0.12, 0.14, 0.03), DARK);
                part(capsule(x, y, z, 0, -0.2, 0, 0, -0.46, 0.04, 0.05), DARK);
                part(cylinder(length(x, z - 0.05), y + 0.5, 0.055, 0.05), BRASS);
                double flame = frame == 1 ? 0.88 : 0.74;
                part(capsule(x, y, z, 0, -0.57, 0.05, 0, -flame, 0.06, frame == 1 ? 0.05 : 0.03), BLUE_FLAME);
            }
        };
    }

    /** The crawler: a striped drill bot on six legs. Frames 0 to 3 walk, frame 4 bites. */
    static Model crawler(int frame) {
        double phase = frame * Math.PI / 2;
        boolean bite = frame == 4;
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(box(x, y + 0.6, z + 0.08, 0.25, 0.13, 0.40, 0.07), HAZARD);
                part(box(x, y + 0.44, z + 0.36, 0.17, 0.07, 0.13, 0.04), DARK);
                part(box(x, y + 0.6, z - 0.36, 0.2, 0.12, 0.14, 0.05), DARK);
                part(sphere(Math.abs(x) - 0.09, y + 0.55, z - 0.52, 0.035), AMBER);
                double reach = bite ? 0.12 : 0;
                part(cylinder(length(x, y + 0.6), z - 0.56 - reach, 0.1, 0.04), STEEL);
                part(cone(length(x, y + 0.6), z - 0.6 - reach, 0.1, 0.4), DRILL);
                double ax = Math.abs(x);
                boolean left = x < 0;
                for (int i = 0; i < 3; i++) {
                    double hz = -0.24 + i * 0.26;
                    double swing = bite ? 0 : Math.sin(phase + i * 2.1 + (left ? Math.PI : 0)) * 0.08;
                    double lift = bite ? 0 : Math.max(0, Math.cos(phase + i * 2.1 + (left ? Math.PI : 0))) * 0.06;
                    part(capsule(ax, y, z, 0.22, -0.6, hz, 0.46, -0.42 + lift, hz + swing, 0.04), DARK);
                    part(capsule(ax, y, z, 0.46, -0.42 + lift, hz + swing, 0.58, -0.97 + lift, hz + swing * 1.5, 0.03), STEEL);
                    part(sphere(ax - 0.46, y + 0.42 - lift, z - hz - swing, 0.055), ORANGE);
                }
            }
        };
    }

    /** The sentry: a turret on a tripod. Frame 1 fires. */
    static Model sentry(int frame) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2 / 3 + Math.PI / 6;
                    part(capsule(x, y, z, 0, -0.35, 0, Math.cos(a) * 0.45, -0.97, Math.sin(a) * 0.45, 0.035), DARK);
                }
                part(cylinder(length(x, z), y + 0.32, 0.08, 0.1), STEEL);
                part(box(x, y + 0.08, z, 0.26, 0.17, 0.3, 0.06), YELLOW);
                part(box(x, y + 0.08, z - 0.05, 0.27, 0.05, 0.31, 0.02), DARK);
                part(sphere(x, y + 0.04, z - 0.28, 0.06), RED);
                double ax = Math.abs(x);
                part(cylinder(length(ax - 0.11, y), z - 0.42, 0.04, 0.15), STEEL);
                part(cylinder(length(ax - 0.11, y), z - 0.56, 0.05, 0.02), DARK);
                if (frame == 1) part(sphere(ax - 0.11, y, z - 0.64, 0.07), HOT);
            }
        };
    }

    /** The loader: a heavy walker with two claws that throws blocks of ice. Frames 0 to 3 walk, 4 throws. */
    static Model loader(int frame) {
        double phase = frame * Math.PI / 2;
        boolean throwing = frame == 4;
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(box(x, y - 0.05, z, 0.42, 0.32, 0.3, 0.1), YELLOW);
                part(box(x, y - 0.05, z - 0.02, 0.44, 0.06, 0.32, 0.02), DARK);
                part(box(x, y - 0.38, z, 0.25, 0.08, 0.2, 0.05), DARK);
                part(box(x, y - 0.4, z - 0.32, 0.28, 0.08, 0.04, 0.02), HAZARD);
                part(sphere(x, y, z - 0.3, 0.09), RED);
                part(cylinder(length(x, z + 0.08), y - 0.36, 0.18, 0.08), STEEL);
                double ax = Math.abs(x);
                boolean left = x < 0;
                double step = throwing ? 0 : Math.sin(phase + (left ? Math.PI : 0)) * 0.14;
                double raise = throwing && !left ? 0.55 : 0;
                // legs
                part(capsule(ax, y, z, 0.22, -0.4, 0, 0.3, -0.7, step, 0.09), DARK);
                part(capsule(ax, y, z, 0.3, -0.7, step, 0.28, -0.92, step * 1.3, 0.08), STEEL);
                part(box(ax - 0.28, y + 0.95, z - step * 1.3, 0.12, 0.05, 0.18, 0.03), DARK);
                // arms and claws
                part(sphere(ax - 0.5, y - 0.1, z, 0.12), STEEL);
                part(capsule(ax, y, z, 0.5, -0.1, 0, 0.62, -0.42 + raise * 1.3, 0.2, 0.08), DARK);
                part(capsule(ax, y, z, 0.62, -0.42 + raise * 1.3, 0.2, 0.58, -0.62 + raise * 1.6, 0.42, 0.06), STEEL);
                part(box(ax - 0.58, y + 0.68 - raise * 1.6, z - 0.48, 0.09, 0.07, 0.07, 0.02), DARK);
                if (throwing && !left) part(box(ax - 0.58, y - 0.36, z - 0.52, 0.16, 0.16, 0.16, 0.03), ICE);
            }
        };
    }

    /** The Foreman's core: a sphere on a pedestal, ringed with violet eyes, cables to the floor. */
    static Model foreman(int frame) {
        double pulse = frame == 1 ? 0.012 : 0;
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(cylinder(length(x, z), y + 0.85, 0.5, 0.15) - 0.03, DARK);
                part(cylinder(length(x, z), y + 0.62, 0.3, 0.12), STEEL);
                part(sphere(x, y - 0.05, z, 0.5), STEEL);
                part(torus(length(x, z), y - 0.05, 0.5, 0.06 + pulse), VIOLET_GLOW);
                part(torus(length(x, z), y - 0.3, 0.42, 0.03), DARK);
                part(torus(length(x, z), y - 0.2, 0.47, 0.02), DARK);
                part(sphere(x, y - 0.56, z, 0.12), VIOLET_GLOW);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    double cx = Math.cos(a), cz = Math.sin(a);
                    part(capsule(x, y, z, cx * 0.35, -0.35, cz * 0.35, cx * 0.85, -0.97, cz * 0.85, 0.04), RUBBER);
                }
            }
        };
    }

    /** What is left of a machine. */
    static Model scrap(boolean big) {
        double s = big ? 1.0 : 0.7;
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                x /= s;
                y = (y + 1) / s - 1;
                z /= s;
                part(box(x + 0.25, y + 0.9, z - 0.1, 0.18, 0.08, 0.14, 0.03) * s, DARK);
                part(box(x - 0.2, y + 0.92, z + 0.15, 0.14, 0.06, 0.2, 0.02) * s, YELLOW);
                part(capsule(x, y, z, -0.45, -0.97, -0.2, 0.1, -0.93, 0.3, 0.035) * s, STEEL);
                part(sphere(x - 0.05, y + 0.88, z - 0.25, 0.12) * s, BODY);
                part(cylinder(length(x - 0.4, z + 0.3), y + 0.94, 0.12, 0.03) * s, RUST);
                part(capsule(x, y, z, 0.1, -0.97, -0.4, 0.45, -0.85, 0.05, 0.025) * s, DARK);
            }
        };
    }

    // ---- pickups and props --------------------------------------------------------------------------

    static Model pickup(PickupKind kind) {
        return switch (kind) {
            case PATCH -> kit(0.32, 0.18, false);
            case MEDKIT -> kit(0.5, 0.28, true);
            case PLATING -> plate(BLUE, 0.4);
            case HEAVY_PLATING -> plate(VIOLET, 0.55);
            case RIVETS -> rivets();
            case CELLS -> cell();
            case GAS -> gas();
            case CHARGES -> chargePack(2);
            case CHIP_GREEN -> chip(CHIP_GREEN);
            case CHIP_AMBER -> chip(CHIP_AMBER);
            case CHIP_VIOLET -> chip(CHIP_VIOLET);
            case BLASTING_CHARGE -> chargePack(3);
            case SCATTER_RIVETER, ARC_WELDER, PLASMA_CUTTER -> lying(tool(kind.weapon(), false, false));
        };
    }

    private static Model kit(double w, double h, boolean handle) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(box(x, y + 1 - h, z, w, h, w * 0.6, 0.05), WHITE);
                double fy = y + 1 - h, fz = z - w * 0.6;
                part(box(x, fy, fz, w * 0.5, 0.06, 0.012, 0), CROSS);
                part(box(x, fy, fz, 0.06, h * 0.6, 0.012, 0), CROSS);
                if (handle) part(torus(length(x, y + 1 - h * 2 - 0.05), z, 0.12, 0.025), DARK);
            }
        };
    }

    private static Model plate(int material, double size) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                double py = y + 1 - size * 0.9;
                part(box(x, py, z, size * 0.75, size * 0.9, 0.06, 0.06), material);
                part(box(x, py + size * 0.15, z - 0.05, size * 0.5, 0.04, 0.02, 0.01), STEEL);
                part(box(x, py - size * 0.25, z - 0.05, size * 0.5, 0.04, 0.02, 0.01), STEEL);
            }
        };
    }

    private static Model rivets() {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(box(x, y + 0.85, z, 0.42, 0.15, 0.3, 0.03), OLIVE);
                for (int i = -2; i <= 2; i++) for (int j = -1; j <= 1; j++) {
                    part(sphere(x - i * 0.15, y + 0.68, z - j * 0.17, 0.065), BRASS);
                }
            }
        };
    }

    private static Model cell() {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                double b = box(x, y + 0.66, z, 0.5, 0.3, 0.32, 0.05);
                part(b, DARK);
                part(Math.max(box(x, y + 0.66, z, 0.52, 0.06, 0.34, 0), b - 0.01), GREEN);
                part(cylinder(length(Math.abs(x) - 0.25, z), y + 0.3, 0.07, 0.07), STEEL);
            }
        };
    }

    private static Model gas() {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                for (int i = -1; i <= 1; i += 2) {
                    part(cylinder(length(x - i * 0.2, z), y + 0.55, 0.17, 0.4) - 0.04, i < 0 ? TANK : ORANGE);
                    part(cylinder(length(x - i * 0.2, z), y + 0.08, 0.06, 0.08), BRASS);
                }
            }
        };
    }

    private static Model chargePack(int count) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                for (int i = 0; i < count; i++) {
                    double ox = (i - (count - 1) / 2.0) * 0.4;
                    part(box(x - ox, y + 0.78, z, 0.17, 0.22, 0.12, 0.03), OLIVE);
                    part(box(x - ox, y + 0.6, z - 0.12, 0.1, 0.05, 0.02, 0.01), DARK);
                    part(sphere(x - ox, y + 0.6, z - 0.14, 0.035), RED);
                }
            }
        };
    }

    private static Model chip(int material) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                double py = y + 0.55;
                part(box(x, py, z, 0.32, 0.22, 0.03, 0.04), DARK);
                part(box(x, py, z - 0.03, 0.26, 0.04, 0.01, 0), material);
                part(box(x + 0.12, py + 0.1, z - 0.03, 0.08, 0.06, 0.012, 0.01), GOLD);
            }
        };
    }

    static Model crate() {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(box(x, y + 0.56, z, 0.42, 0.42, 0.42, 0.03), OLIVE);
                double e = box(x, y + 0.56, z, 0.44, 0.44, 0.44, 0.03);
                part(Math.max(e, -box(x, y + 0.56, z, 0.37, 0.37, 0.50, 0)), DARK);
                part(Math.max(e, -box(x, y + 0.56, z, 0.50, 0.37, 0.37, 0)), DARK);
            }
        };
    }

    static Model canister() {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(cylinder(length(x, z), y + 0.4, 0.34, 0.52) - 0.05, TANK);
                part(cylinder(length(x, z), y - 0.2, 0.08, 0.06), STEEL);
                part(cylinder(length(x - 0.06, z), y - 0.28, 0.025, 0.06), BRASS);
            }
        };
    }

    /** A thrown charge, small, lying where it stopped. */
    static Model charge() {
        return chargePack(1);
    }

    static Model iceBlock() {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                part(box(x, y, z, 0.45, 0.45, 0.45, 0.06), ICE);
            }
        };
    }

    /** A tool lying on the floor, as a pickup: turned sideways and shrunk. */
    static Model lying(Model tool) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                double s = 1.15;
                // model space of the tool: barrel along +z; here the barrel points to the right (+x)
                part(tool.distance(-z * s, (y + 0.75) * s, x * s) / s, 0);
            }

            @Override
            protected Material material(int part, double x, double y, double z) {
                double s = 1.15;
                return tool.materialAt(-z * s, (y + 0.75) * s, x * s);
            }
        };
    }

    // ---- the tools in the hand ----------------------------------------------------------------------

    /**
     * A tool held in the right hand, in the camera's space (x right, y up, z ahead), the barrel along +z.
     * {@code firing} lights the muzzle (for the picture's light); {@code withHand} adds glove and sleeve.
     */
    static Model tool(Weapon w, boolean firing, boolean withHand) {
        return new Part() {
            @Override
            protected void shape(double x, double y, double z) {
                switch (w) {
                    case ICE_PICK -> icePick(this, x, y, z);
                    case RIVET_GUN -> rivetGun(this, x, y, z);
                    case SCATTER_RIVETER -> scatter(this, x, y, z);
                    case ARC_WELDER -> welder(this, x, y, z, firing);
                    case PLASMA_CUTTER -> plasma(this, x, y, z, firing);
                    case BLASTING_CHARGE -> charge(this, x, y, z);
                }
                if (withHand && w != Weapon.ICE_PICK && w != Weapon.BLASTING_CHARGE) hand(this, x, y, z);
            }
        };
    }

    private static void hand(Part m, double x, double y, double z) {
        m.add(box(x - 0.005, y + 0.17, z + 0.21, 0.07, 0.085, 0.075, 0.045), LEATHER);
        for (int i = 0; i < 3; i++) {
            double fy = -0.115 - i * 0.042;
            m.add(capsule(x, y, z, -0.06, fy, -0.12 + i * 0.012, 0.06, fy, -0.12 + i * 0.012, 0.024), LEATHER);
        }
        m.add(capsule(x, y, z, -0.07, -0.1, -0.22, -0.09, -0.04, -0.1, 0.026), LEATHER);
        m.add(capsule(x, y, z, 0.0, -0.19, -0.27, 0.07, -0.40, -0.85, 0.085), SUIT);
        m.add(capsule(x, y, z, -0.002, -0.2, -0.29, 0.012, -0.235, -0.38, 0.093), CUFF);
    }

    private static void rivetGun(Part m, double x, double y, double z) {
        m.add(box(x, y, z, 0.085, 0.072, 0.30, 0.025), DARK);
        m.add(box(x, y - 0.075, z - 0.04, 0.062, 0.03, 0.25, 0.02), STEEL);
        for (int i = 0; i < 6; i++) m.add(box(x, y - 0.104, z - 0.12 + i * 0.05, 0.05, 0.006, 0.012, 0.003), DARK);
        m.add(box(x + 0.087, y + 0.005, z + 0.02, 0.004, 0.022, 0.2, 0.002), HAZARD);
        double bore = cylinder(length(x, y - 0.005), z - 0.56, 0.019, 0.05);
        m.add(Math.max(cylinder(length(x, y - 0.005), z - 0.42, 0.036, 0.15), -bore), STEEL);
        m.add(Math.max(cylinder(length(x, y - 0.005), z - 0.545, 0.047, 0.025), -bore), DARK);
        m.add(cylinder(length(x, y - 0.005), z - 0.33, 0.048, 0.012), DARK);
        m.add(cylinder(length(x, z + 0.14), y - 0.115, 0.075, 0.025) - 0.006, STEEL);
        m.add(cylinder(length(x, z + 0.14), y - 0.145, 0.022, 0.01), DARK);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            m.add(sphere(x - Math.cos(a) * 0.054, y - 0.142, z + 0.14 - Math.sin(a) * 0.054, 0.011), BRASS);
        }
        grip(m, x, y, z);
    }

    private static void grip(Part m, double x, double y, double z) {
        double gy = y + 0.15, gz = z + 0.17, ca = Math.cos(0.3), sa = Math.sin(0.3);
        m.add(box(x, gy * ca - gz * sa, gy * sa + gz * ca, 0.042, 0.11, 0.045, 0.02), RUBBER);
        m.add(capsule(x, y, z, 0, -0.08, 0.02, 0, -0.17, -0.06, 0.018), DARK);
    }

    private static void scatter(Part m, double x, double y, double z) {
        m.add(box(x, y, z, 0.11, 0.08, 0.32, 0.03), DARK);
        m.add(box(x, y - 0.085, z - 0.06, 0.08, 0.025, 0.24, 0.02), STEEL);
        for (int s = -1; s <= 1; s += 2) {
            double bore = cylinder(length(x - s * 0.05, y), z - 0.6, 0.022, 0.05);
            m.add(Math.max(cylinder(length(x - s * 0.05, y), z - 0.45, 0.04, 0.16), -bore), STEEL);
        }
        m.add(box(x, y, z - 0.6, 0.1, 0.05, 0.025, 0.015), DARK);
        m.add(box(x + 0.112, y + 0.005, z + 0.02, 0.004, 0.025, 0.22, 0.002), HAZARD);
        // two drums of rivets
        for (int s = -1; s <= 1; s += 2) {
            m.add(cylinder(length(x - s * 0.06, z + 0.1), y - 0.13, 0.06, 0.03) - 0.005, STEEL);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2 / 9;
                m.add(sphere(x - s * 0.06 - Math.cos(a) * 0.042, y - 0.163, z + 0.1 - Math.sin(a) * 0.042, 0.01), BRASS);
            }
        }
        grip(m, x, y, z);
    }

    private static void welder(Part m, double x, double y, double z, boolean firing) {
        m.add(box(x, y, z, 0.08, 0.075, 0.26, 0.03), YELLOW);
        m.add(box(x, y + 0.002, z, 0.083, 0.02, 0.27, 0.005), DARK);
        for (int i = 0; i < 5; i++) m.add(torus(length(x, y - 0.01), z - 0.3 - i * 0.04, 0.045, 0.012), BRASS);
        m.add(cylinder(length(x, y - 0.01), z - 0.38, 0.025, 0.14), DARK);
        m.add(cone(length(x, y - 0.01), z - 0.52, 0.03, 0.08), STEEL);
        if (firing) m.add(sphere(x, y - 0.01, z - 0.62, 0.03), CYAN);
        m.add(box(x, y - 0.1, z + 0.05, 0.05, 0.03, 0.12, 0.02), DARK);
        m.add(sphere(x, y - 0.12, z - 0.02, 0.025), GREEN);
        m.add(capsule(x, y, z, 0.07, -0.02, -0.2, 0.2, -0.25, -0.5, 0.018), RUBBER);
        grip(m, x, y, z);
    }

    private static void plasma(Part m, double x, double y, double z, boolean firing) {
        m.add(smoothUnion(box(x, y, z, 0.1, 0.09, 0.3, 0.05), cylinder(length(x, y + 0.01), z - 0.3, 0.06, 0.08), 0.04), DARK);
        m.add(box(x, y - 0.095, z - 0.05, 0.07, 0.02, 0.2, 0.01), CYAN);
        double bore = cylinder(length(x, y + 0.01), z - 0.48, 0.03, 0.06);
        m.add(Math.max(cone(length(x, y + 0.01), z - 0.36, 0.06, 0.16), -bore), STEEL);
        if (firing) m.add(sphere(x, y + 0.01, z - 0.5, 0.04), CYAN);
        // the gas tank on the side
        m.add(capsule(x, y, z, -0.13, -0.02, -0.18, -0.13, -0.02, 0.12, 0.045), TANK);
        m.add(cylinder(length(x + 0.13, y + 0.02), z - 0.15, 0.02, 0.02), BRASS);
        grip(m, x, y, z);
    }

    private static void charge(Part m, double x, double y, double z) {
        m.add(box(x, y + 0.06, z - 0.02, 0.08, 0.1, 0.06, 0.02), OLIVE);
        m.add(box(x, y + 0.06, z - 0.085, 0.05, 0.03, 0.01, 0.005), DARK);
        m.add(sphere(x + 0.03, y + 0.1, z - 0.09, 0.012), RED);
        m.add(torus(length(x, y - 0.05), z - 0.02, 0.05, 0.01), DARK);
    }

    private static void icePick(Part m, double x, double y, double z) {
        m.add(capsule(x, y, z, 0, -0.3, -0.15, 0.0, 0.25, 0.25, 0.034), RUBBER);
        m.add(capsule(x, y, z, 0, -0.1, -0.005, 0.0, 0.0, 0.07, 0.038), HAZARD);
        m.add(box(x, y - 0.27, z - 0.28, 0.04, 0.06, 0.08, 0.015), STEEL);
        m.add(cone(length(x, y - 0.27 + (z - 0.28) * 0.35), z - 0.32, 0.045, 0.24), STEEL);
        m.add(box(x, y - 0.3, z - 0.15, 0.06, 0.02, 0.07, 0.01), STEEL);
    }
}
