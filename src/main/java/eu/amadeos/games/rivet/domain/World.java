package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.path.DistanceMap;
import eu.amadeos.gameengine.path.Passable;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.raycast.Door;
import eu.amadeos.gameengine.raycast.GridMap;
import eu.amadeos.gameengine.raycast.Hitscan;
import eu.amadeos.gameengine.raycast.LightMap;
import eu.amadeos.gameengine.raycast.Mover;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One level being played: the map with its doors, the player, the machines, what flies and what lies
 * around. {@link #update} moves everything on by one step of the game loop and collects what happened as
 * {@link Event}s. No Swing here: the rules can be tested without a window.
 */
public final class World {
    static final double WALK = 3.2;
    static final double RUN = 5.2;
    static final double USE_RANGE = 1.3;
    static final double PICKUP_RANGE = 0.55;
    static final int MAX_DRONES = 6;

    private final LevelData level;
    private final GridMap map;
    private final Difficulty difficulty;
    private final Player player;
    private final SeededRandom random;
    private final List<Machine> machines = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Pickup> pickups = new ArrayList<>();
    private final List<Prop> props = new ArrayList<>();
    private final List<LightMap.Light> lights = new ArrayList<>();
    private final Map<Door, Chip> locks = new HashMap<>();
    private final Set<Door> secrets = new HashSet<>();
    private final Set<Door> found = new HashSet<>();
    private final List<Event> events = new ArrayList<>();
    private final TileMap<Boolean> tiles;
    private final Passable walkable;
    private final int machinesAtStart;
    private DistanceMap flow;
    private Cell flowGoal;
    private double flowAge;
    private double time;
    private int kills;
    private int points;
    private boolean done;
    private boolean deathReported;
    private boolean triggerHeld;

    public World(LevelData level, Surfaces surfaces, Difficulty difficulty, Player player, SeededRandom random) {
        this.level = level;
        this.difficulty = difficulty;
        this.player = player;
        this.random = random;
        this.map = new GridMap(level.width(), level.height());
        this.tiles = new TileMap<>(level.width(), level.height(), Boolean.FALSE);
        lights.addAll(level.lights());
        build(surfaces);
        machinesAtStart = machines.size();
        player.chips.clear();
        player.hurt = 0;
        player.glint = 0;
        player.cooldown = 0.3;
        walkable = (x, y) -> {
            if (!map.contains(x, y) || map.wall(x, y) != null) return false;
            Door door = map.door(x, y);
            if (door != null && locks.containsKey(door)) return door.passable();
            for (Prop p : props) if (p.blocks() && (int) p.x == x && (int) p.y == y) return false;
            return true;
        };
    }

    private void build(Surfaces s) {
        for (int y = 0; y < level.height(); y++) {
            for (int x = 0; x < level.width(); x++) {
                char c = level.at(x, y);
                double cx = x + 0.5, cy = y + 0.5;
                if (LevelData.wall(c)) {
                    map.setWall(x, y, s.wall(c));
                    continue;
                }
                map.setFloor(x, y, s.floor(c == '_' ? "snow" : level.floor()));
                boolean lamp = c == ',' || c == ';' || c == ':';
                map.setCeiling(x, y, c == '_' ? null : s.ceiling(lamp));
                switch (c) {
                    case ',' -> lights.add(new LightMap.Light(cx, cy, 0.95, 0.97, 1.0, 4.5));
                    case ';' -> lights.add(new LightMap.Light(cx, cy, 0.35, 0.6, 0.95, 4.5));
                    case ':' -> lights.add(new LightMap.Light(cx, cy, 1.15, 0.62, 0.2, 4.0));
                    case '@' -> {
                        player.x = cx;
                        player.y = cy;
                        player.angle = level.startAngle();
                    }
                    case '-', '|', 'g', 'a', 'v', 's' -> door(s, x, y, c);
                    case 'd' -> machine(MachineKind.DRONE, cx, cy);
                    case 'c' -> machine(MachineKind.CRAWLER, cx, cy);
                    case 't' -> machine(MachineKind.SENTRY, cx, cy);
                    case 'l' -> machine(MachineKind.LOADER, cx, cy);
                    case 'f' -> machine(MachineKind.FOREMAN, cx, cy);
                    case 'x' -> props.add(new Prop(Prop.Kind.CRATE, cx, cy));
                    case 'y' -> props.add(new Prop(Prop.Kind.CANISTER, cx, cy));
                    default -> {
                        PickupKind kind = PickupKind.ofMark(c);
                        if (kind != null) pickups.add(new Pickup(kind, cx, cy));
                    }
                }
            }
        }
        // the lift's switch glows green and lights the way out
        for (int y = 0; y < level.height(); y++) for (int x = 0; x < level.width(); x++) {
            if (level.at(x, y) != 'X') continue;
            for (Direction d : Direction.values()) {
                char n = level.at(x + d.dx, y + d.dy);
                if (!LevelData.wall(n)) lights.add(new LightMap.Light(x + 0.5 + d.dx * 0.7, y + 0.5 + d.dy * 0.7, 0.2, 0.75, 0.35, 3));
            }
        }
    }

    private void door(Surfaces s, int x, int y, char c) {
        boolean alongX = LevelData.wall(level.at(x - 1, y)) && LevelData.wall(level.at(x + 1, y));
        if (c == '-') alongX = true;
        if (c == '|') alongX = false;
        Chip lock = switch (c) {
            case 'g' -> Chip.GREEN;
            case 'a' -> Chip.AMBER;
            case 'v' -> Chip.VIOLET;
            default -> null;
        };
        Door door;
        if (c == 's') {
            char beside = alongX ? level.at(x - 1, y) : level.at(x, y - 1);
            door = new Door(x, y, alongX, s.wall(LevelData.wall(beside) ? beside : 'P'), null).timing(0.9, Double.POSITIVE_INFINITY);
            secrets.add(door);
        } else {
            door = new Door(x, y, alongX, s.door(lock), s.doorFrame()).timing(0.6, 4);
        }
        map.addDoor(door);
        if (lock != null) locks.put(door, lock);
    }

    private Machine machine(MachineKind kind, double x, double y) {
        Machine m = new Machine(kind, x, y, angleTo(x, y, player.x, player.y), kind.health() * difficulty.strength());
        machines.add(m);
        return m;
    }

    // ---- the step ------------------------------------------------------------------------------------

    /** Moves the level on by {@code dt} seconds; the events of this step are then in {@link #events()}. */
    public void update(double dt, Input in) {
        events.clear();
        time += dt;
        player.hurt = Math.max(0, player.hurt - dt * 1.6);
        player.glint = Math.max(0, player.glint - dt * 2.5);
        player.cooldown -= dt;
        player.sinceShot += dt;
        if (player.alive() && !done) steer(dt, in);
        else player.firing = false;
        updateMachines(dt);
        updateProjectiles(dt);
        map.updateDoors(dt, this::occupied);
        machines.removeIf(m -> !m.alive() && m.stateTime > 30);   // their scrap stays as a prop
        if (!player.alive() && !deathReported) {
            deathReported = true;
            events.add(Event.at(Event.Type.PLAYER_DIED, player.x, player.y));
        }
    }

    private void steer(double dt, Input in) {
        choose(in);
        player.angle = normalize(player.angle + in.turn());
        double speed = in.run() ? RUN : WALK;
        double f = clamp(in.forward()), s = clamp(in.strafe());
        double length = Math.hypot(f, s);
        if (length > 1) { f /= length; s /= length; }
        double c = Math.cos(player.angle), n = Math.sin(player.angle);
        double dx = (c * f - n * s) * speed * dt, dy = (n * f + c * s) * speed * dt;
        if (dx != 0 || dy != 0) {
            Mover.Position p = Mover.slide(map, player.x, player.y, dx, dy, Player.RADIUS);
            double nx = p.x(), ny = p.y();
            for (Prop prop : props) {
                if (!prop.blocks()) continue;
                double[] out = pushOut(nx, ny, Player.RADIUS, prop.x, prop.y, prop.radius());
                if (out != null && !Mover.overlaps(map, out[0], out[1], Player.RADIUS)) { nx = out[0]; ny = out[1]; }
            }
            for (Machine m : machines) {
                if (!m.alive()) continue;
                double[] out = pushOut(nx, ny, Player.RADIUS, m.x, m.y, m.radius());
                if (out != null && !Mover.overlaps(map, out[0], out[1], Player.RADIUS)) { nx = out[0]; ny = out[1]; }
            }
            player.walked += Math.hypot(nx - player.x, ny - player.y);
            player.x = nx;
            player.y = ny;
        }
        if (in.use()) use();
        boolean pressed = in.fire() && !triggerHeld;
        triggerHeld = in.fire();
        player.firing = in.fire() && player.weapon.kind() == Weapon.Kind.BEAM && player.canFire(player.weapon);
        if (in.fire() && player.cooldown <= 0) {
            if (player.canFire(player.weapon)) fire(player.weapon);
            else {
                if (pressed) events.add(Event.at(Event.Type.EMPTY, player.x, player.y));
                switchToBest();
                player.cooldown = 0.25;
            }
        }
        pickUp();
    }

    private static double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }

    private void choose(Input in) {
        Weapon wanted = null;
        if (in.slot() > 0) wanted = Weapon.inSlot(in.slot());
        if (in.cycle() != 0) {
            Weapon[] all = Weapon.values();
            int i = player.weapon.ordinal();
            for (int k = 1; k < all.length; k++) {
                Weapon w = all[Math.floorMod(i + k * in.cycle(), all.length)];
                if (player.owns(w) && player.canFire(w)) {
                    wanted = w;
                    break;
                }
            }
        }
        if (wanted != null && wanted != player.weapon && player.owns(wanted)) {
            player.weapon = wanted;
            player.cooldown = Math.max(player.cooldown, 0.25);
            events.add(new Event(Event.Type.WEAPON_CHANGED, player.x, player.y, wanted));
        }
    }

    private void switchToBest() {
        Weapon[] order = {Weapon.PLASMA_CUTTER, Weapon.SCATTER_RIVETER, Weapon.RIVET_GUN, Weapon.ARC_WELDER, Weapon.ICE_PICK};
        for (Weapon w : order) {
            if (player.owns(w) && player.canFire(w) && w != player.weapon) {
                player.weapon = w;
                events.add(new Event(Event.Type.WEAPON_CHANGED, player.x, player.y, w));
                return;
            }
        }
    }

    private void use() {
        GridMap.Ray ray = new GridMap.Ray();
        double dx = Math.cos(player.angle), dy = Math.sin(player.angle);
        map.cast(player.x, player.y, dx, dy, USE_RANGE, ray);
        if (!ray.hit()) return;
        if (ray.door != null) {
            Door door = ray.door;
            Chip lock = locks.get(door);
            if (lock != null && !player.chips.contains(lock)) {
                events.add(new Event(Event.Type.LOCKED, door.x() + 0.5, door.y() + 0.5, lock));
                return;
            }
            if (door.state() == Door.State.CLOSED || door.state() == Door.State.CLOSING) {
                door.open();
                events.add(new Event(Event.Type.DOOR_OPENS, door.x() + 0.5, door.y() + 0.5, secrets.contains(door)));
                if (secrets.contains(door) && found.add(door)) {
                    points += 50;
                    events.add(Event.at(Event.Type.SECRET_FOUND, door.x() + 0.5, door.y() + 0.5));
                }
            }
            return;
        }
        if (level.at(ray.cellX, ray.cellY) == 'X') {
            done = true;
            events.add(Event.at(Event.Type.LEVEL_DONE, ray.cellX + 0.5, ray.cellY + 0.5));
        }
    }

    // ---- shooting -----------------------------------------------------------------------------------

    private void fire(Weapon w) {
        player.cooldown = w.pause();
        player.sinceShot = 0;
        if (w.ammo() != null) player.ammo.merge(w.ammo(), -w.perShot(), Integer::sum);
        double x = player.x, y = player.y, a = player.angle;
        switch (w.kind()) {
            case MELEE -> {
                Hitscan.Hit<Hitscan.Target> hit = Hitscan.shoot(map, x, y, a, w.range(), targets());
                events.add(new Event(Event.Type.SHOT, x, y, w));
                if (hit.target() != null) {
                    struck(hit.target(), w.damage(), hit.x(), hit.y());
                    events.add(Event.at(Event.Type.MELEE_HIT, hit.x(), hit.y()));
                } else if (hit.distance() < w.range()) {
                    events.add(new Event(Event.Type.HIT_WALL, hit.x(), hit.y(), w));
                }
            }
            case HITSCAN -> {
                events.add(new Event(Event.Type.SHOT, x, y, w));
                for (int i = 0; i < w.pellets(); i++) {
                    double angle = a + (random.nextDouble() * 2 - 1) * w.spread();
                    Hitscan.Hit<Hitscan.Target> hit = Hitscan.shoot(map, x, y, angle, w.range(), targets());
                    if (hit.target() != null) struck(hit.target(), w.damage(), hit.x(), hit.y());
                    else if (hit.distance() < w.range()) events.add(new Event(Event.Type.HIT_WALL, hit.x(), hit.y(), w));
                }
                noise(12);
            }
            case BEAM -> {
                Hitscan.Hit<Hitscan.Target> hit = Hitscan.shoot(map, x, y, a, w.range(), targets());
                events.add(new Event(Event.Type.BEAM, hit.x(), hit.y(), w));
                if (hit.target() != null) struck(hit.target(), w.damage(), hit.x(), hit.y());
                noise(7);
            }
            case PROJECTILE -> {
                events.add(new Event(Event.Type.SHOT, x, y, w));
                launch(Projectile.Kind.PLASMA, x, y, 0.42, a, 12, true, w.damage(), 0.9, 5);
                noise(10);
            }
            case THROWN -> {
                events.add(new Event(Event.Type.SHOT, x, y, w));
                launch(Projectile.Kind.CHARGE, x, y, 0.3, a, 7, true, w.damage(), 2.6, 1.3);
            }
        }
    }

    private List<Hitscan.Target> targets() {
        List<Hitscan.Target> t = new ArrayList<>();
        for (Machine m : machines) if (m.alive()) t.add(m);
        for (Prop p : props) if (p.blocks()) t.add(p);
        return t;
    }

    private void struck(Hitscan.Target target, double damage, double x, double y) {
        if (target instanceof Machine m) damage(m, damage);
        else if (target instanceof Prop p) {
            events.add(new Event(Event.Type.HIT_WALL, x, y, p));
            if (p.kind == Prop.Kind.CANISTER) {
                p.health -= damage;
                if (p.health <= 0) burst(p);
            }
        }
    }

    private void burst(Prop p) {
        if (p.gone) return;
        p.gone = true;
        events.add(Event.at(Event.Type.CANISTER_BURSTS, p.x, p.y));
        explode(p.x, p.y, 2.2, 60, true);
    }

    private void launch(Projectile.Kind kind, double x, double y, double z, double angle, double speed, boolean fromPlayer,
                        double damage, double splash, double life) {
        double c = Math.cos(angle), s = Math.sin(angle);
        projectiles.add(new Projectile(kind, x + c * 0.35, y + s * 0.35, z, c * speed, s * speed, fromPlayer, damage, splash, life));
    }

    private void explode(double x, double y, double radius, double damage, boolean byPlayer) {
        events.add(new Event(Event.Type.EXPLOSION, x, y, radius));
        for (Machine m : machines) {
            if (!m.alive()) continue;
            double d = Math.hypot(m.x - x, m.y - y);
            if (d < radius + m.radius() && map.sees(x, y, m.x, m.y)) damage(m, damage * Math.max(0.15, 1 - d / radius));
        }
        double d = Math.hypot(player.x - x, player.y - y);
        if (d < radius && map.sees(x, y, player.x, player.y)) {
            hurtPlayer(damage * (1 - d / radius) * (byPlayer ? 0.5 : difficulty.damage()), x, y);
        }
        for (Prop p : props) {
            if (p.kind == Prop.Kind.CANISTER && !p.gone && Math.hypot(p.x - x, p.y - y) < radius) burst(p);
        }
    }

    private void noise(double radius) {
        for (Machine m : machines) {
            if (m.state != Machine.State.IDLE) continue;
            double d = Math.hypot(m.x - player.x, m.y - player.y);
            if (d < radius && (d < radius / 2 || map.sees(m.x, m.y, player.x, player.y))) alert(m);
        }
    }

    // ---- machines -----------------------------------------------------------------------------------

    private void damage(Machine m, double amount) {
        if (!m.alive()) return;
        m.health -= amount;
        m.flash = 1;
        events.add(new Event(Event.Type.HIT_MACHINE, m.x, m.y, m.kind));
        if (m.health <= 0) {
            m.to(Machine.State.DEAD);
            kills++;
            points += m.kind.points();
            boolean big = m.kind == MachineKind.LOADER || m.kind == MachineKind.FOREMAN;
            props.add(new Prop(big ? Prop.Kind.BIG_SCRAP : Prop.Kind.SCRAP, m.x, m.y));
            // the Foreman's core holds the chip of the last door
            if (m.kind == MachineKind.FOREMAN) pickups.add(new Pickup(PickupKind.CHIP_VIOLET, m.x, m.y));
            events.add(new Event(Event.Type.MACHINE_DESTROYED, m.x, m.y, m.kind));
            return;
        }
        if (m.state == Machine.State.IDLE) alert(m);
        double pain = switch (m.kind) {
            case DRONE, CRAWLER -> 0.5;
            case SENTRY -> 0.3;
            case LOADER -> 0.15;
            case FOREMAN -> 0;
        };
        if (random.nextDouble() < pain) m.to(Machine.State.PAIN);
    }

    private void alert(Machine m) {
        if (m.state != Machine.State.IDLE) return;
        m.to(Machine.State.CHASE);
        m.cooldown = 0.4 + random.nextDouble() * 0.6;
        events.add(new Event(Event.Type.MACHINE_ALERTED, m.x, m.y, m.kind));
        for (Machine other : machines) {
            if (other.state == Machine.State.IDLE && Math.hypot(other.x - m.x, other.y - m.y) < 4) {
                other.to(Machine.State.CHASE);
                other.cooldown = 0.8 + random.nextDouble();
            }
        }
    }

    private void updateMachines(double dt) {
        Cell goal = new Cell((int) player.x, (int) player.y);
        flowAge += dt;
        if (flow == null || !goal.equals(flowGoal) || flowAge > 0.5) {
            flow = DistanceMap.search(tiles, walkable, goal);
            flowGoal = goal;
            flowAge = 0;
        }
        List<Machine> born = new ArrayList<>();
        for (Machine m : machines) {
            m.flash = Math.max(0, m.flash - dt * 5);
            m.stateTime += dt;
            if (!m.alive()) continue;
            m.cooldown -= dt;
            double dx = player.x - m.x, dy = player.y - m.y, dist = Math.hypot(dx, dy);
            boolean sees = player.alive() && dist <= m.kind.sight() && map.sees(m.x, m.y, player.x, player.y);
            switch (m.state) {
                case IDLE -> {
                    double off = Math.abs(normalize(Math.atan2(dy, dx) - m.facing));
                    if (sees && (off < Math.PI * 0.6 || dist < 3)) alert(m);
                }
                case CHASE -> {
                    if (sees) m.facing = Math.atan2(dy, dx);
                    if (sees && dist <= m.kind.range() && m.cooldown <= 0) {
                        m.to(Machine.State.WINDUP);
                        events.add(new Event(Event.Type.MACHINE_ATTACKS, m.x, m.y, m.kind));
                    } else if (m.kind.speed() > 0 && player.alive()) {
                        walk(m, dt, sees, dist);
                    }
                    if (m.kind == MachineKind.FOREMAN) launchDrones(m, dt, born);
                }
                case WINDUP -> {
                    m.facing = Math.atan2(dy, dx);
                    if (m.stateTime >= windup(m.kind)) {
                        attack(m, dist);
                        m.cooldown = m.kind.pause() * difficulty.pause() * (0.8 + random.nextDouble() * 0.4);
                        m.to(Machine.State.CHASE);
                    }
                }
                case PAIN -> {
                    if (m.stateTime >= 0.25) m.to(Machine.State.CHASE);
                }
                case DEAD -> { }
            }
        }
        machines.addAll(born);
        separate();
    }

    private static double windup(MachineKind kind) {
        return switch (kind.attack()) {
            case BITE -> 0.25;
            case SPARK -> 0.4;
            case BURST -> 0.5;
            case ICE_BLOCK -> 0.6;
            case FAN -> 0.7;
        };
    }

    private void walk(Machine m, double dt, boolean sees, double dist) {
        double keep = switch (m.kind.attack()) {
            case SPARK -> 3.5;
            case ICE_BLOCK -> 2.5;
            default -> 0;
        };
        double tx, ty;
        if (sees && dist < 7) {
            if (dist <= keep) return;               // close enough to shoot: hold position
            tx = player.x;
            ty = player.y;
        } else {
            int cx = (int) m.x, cy = (int) m.y;
            Direction d = flow.downhill(cx, cy);
            if (d == null) return;
            int nx = cx + d.dx, ny = cy + d.dy;
            Door door = map.door(nx, ny);
            if (door != null && !door.passable()) {
                if (!locks.containsKey(door)) door.open();
                return;
            }
            tx = nx + 0.5;
            ty = ny + 0.5;
        }
        double ax = tx - m.x, ay = ty - m.y, l = Math.hypot(ax, ay);
        if (l < 1e-6) return;
        double step = Math.min(l, m.kind.speed() * dt);
        if (!sees) m.facing = Math.atan2(ay, ax);
        Mover.Position p = Mover.slide(map, m.x, m.y, ax / l * step, ay / l * step, m.radius() * 0.9);
        m.moving += dt;
        m.x = p.x();
        m.y = p.y();
    }

    private void attack(Machine m, double dist) {
        double damage = m.kind.damage() * difficulty.damage();
        double aim = Math.atan2(player.y - m.y, player.x - m.x);
        switch (m.kind.attack()) {
            case BITE -> {
                if (dist <= m.kind.range() + 0.35) hurtPlayer(damage, m.x, m.y);
            }
            case SPARK -> launch(Projectile.Kind.SPARK, m.x, m.y, m.kind.lift() + 0.35, aim + (random.nextDouble() - 0.5) * 0.12,
                    7, false, damage, 0, 4);
            case BURST -> {
                double chance = Math.max(0.2, Math.min(0.75, 0.75 - dist * 0.03));
                for (int i = 0; i < 3; i++) if (random.nextDouble() < chance) hurtPlayer(damage, m.x, m.y);
            }
            case ICE_BLOCK -> launch(Projectile.Kind.ICE_BLOCK, m.x, m.y, 0.6, aim, 6, false, damage, 1.0, 5);
            case FAN -> {
                for (int i = -2; i <= 2; i++) launch(Projectile.Kind.BOLT, m.x, m.y, 0.6, aim + i * 0.18, 7.5, false, damage, 0, 6);
            }
        }
    }

    private void launchDrones(Machine foreman, double dt, List<Machine> born) {
        foreman.spawnTimer -= dt;
        if (foreman.spawnTimer > 0) return;
        foreman.spawnTimer = 14;
        long drones = machines.stream().filter(m -> m.alive() && m.kind == MachineKind.DRONE).count();
        if (drones >= MAX_DRONES) return;
        int made = 0;
        for (double a = 0; a < Math.PI * 2 && made < 2; a += Math.PI / 4) {
            double x = foreman.x + Math.cos(a) * 1.6, y = foreman.y + Math.sin(a) * 1.6;
            if (Mover.overlaps(map, x, y, 0.3)) continue;
            Machine d = new Machine(MachineKind.DRONE, x, y, a, MachineKind.DRONE.health() * difficulty.strength());
            d.to(Machine.State.CHASE);
            born.add(d);
            made++;
        }
        if (made > 0) events.add(Event.at(Event.Type.DRONES_LAUNCHED, foreman.x, foreman.y));
    }

    /** Machines do not stand inside each other, nor inside the player. */
    private void separate() {
        for (int i = 0; i < machines.size(); i++) {
            Machine a = machines.get(i);
            if (!a.alive() || a.kind.speed() == 0) continue;
            for (int j = 0; j < machines.size(); j++) {
                Machine b = machines.get(j);
                if (i == j || !b.alive()) continue;
                double[] out = pushOut(a.x, a.y, a.radius(), b.x, b.y, b.radius());
                if (out != null && !Mover.overlaps(map, out[0], out[1], a.radius() * 0.9)) { a.x = out[0]; a.y = out[1]; }
            }
            double[] out = pushOut(a.x, a.y, a.radius(), player.x, player.y, Player.RADIUS);
            if (out != null && !Mover.overlaps(map, out[0], out[1], a.radius() * 0.9)) { a.x = out[0]; a.y = out[1]; }
        }
    }

    /** Where a circle at (x, y) must go to stop overlapping another; {@code null} if they do not overlap. */
    private static double[] pushOut(double x, double y, double r, double ox, double oy, double or) {
        double dx = x - ox, dy = y - oy, d = Math.hypot(dx, dy), min = r + or;
        if (d >= min) return null;
        if (d < 1e-6) { dx = 1; dy = 0; d = 1; }
        return new double[]{ox + dx / d * min, oy + dy / d * min};
    }

    private void hurtPlayer(double amount, double fromX, double fromY) {
        if (!player.alive()) return;
        player.damage(amount);
        events.add(new Event(Event.Type.PLAYER_HURT, fromX, fromY, amount));
    }

    // ---- projectiles --------------------------------------------------------------------------------

    private void updateProjectiles(double dt) {
        for (Projectile p : projectiles) {
            if (p.gone) continue;
            p.life -= dt;
            if (p.kind == Projectile.Kind.CHARGE) {
                double slow = Math.max(0, 1 - 1.6 * dt);
                p.vx *= slow;
                p.vy *= slow;
                p.z = Math.max(0.08, p.z - dt * 0.4);
            }
            if (p.life <= 0) {
                p.gone = true;
                if (p.kind == Projectile.Kind.CHARGE) explode(p.x, p.y, p.splash, p.damage, true);
                continue;
            }
            double nx = p.x + p.vx * dt, ny = p.y + p.vy * dt;
            boolean wall = map.blocks((int) Math.floor(nx), (int) Math.floor(ny));
            Prop prop = null;
            for (Prop o : props) if (o.blocks() && Math.hypot(o.x - nx, o.y - ny) < o.radius()) prop = o;
            if (wall || prop != null) {
                if (p.kind == Projectile.Kind.CHARGE) {
                    p.vx = 0;
                    p.vy = 0;
                    continue;
                }
                impact(p);
                if (prop != null && prop.kind == Prop.Kind.CANISTER) burst(prop);
                continue;
            }
            p.x = nx;
            p.y = ny;
            if (p.fromPlayer) {
                for (Machine m : machines) {
                    if (!m.alive() || Math.hypot(m.x - p.x, m.y - p.y) > m.radius() + 0.15) continue;
                    if (p.kind == Projectile.Kind.CHARGE) {
                        p.gone = true;
                        explode(p.x, p.y, p.splash, p.damage, true);
                    } else {
                        damage(m, p.damage);
                        impact(p);
                    }
                    break;
                }
            } else if (player.alive() && Math.hypot(player.x - p.x, player.y - p.y) < Player.RADIUS + 0.12) {
                hurtPlayer(p.damage, p.x, p.y);
                p.gone = true;
                events.add(new Event(Event.Type.HIT_WALL, p.x, p.y, p.kind));
            }
        }
        projectiles.removeIf(p -> p.gone);
    }

    private void impact(Projectile p) {
        p.gone = true;
        if (p.splash > 0 && p.kind != Projectile.Kind.ICE_BLOCK) explode(p.x, p.y, p.splash, p.damage * 0.5, p.fromPlayer);
        else if (p.kind == Projectile.Kind.ICE_BLOCK) {
            double d = Math.hypot(player.x - p.x, player.y - p.y);
            if (d < p.splash) hurtPlayer(p.damage * (1 - d / p.splash) * 0.6, p.x, p.y);
            events.add(new Event(Event.Type.HIT_WALL, p.x, p.y, p.kind));
        } else {
            events.add(new Event(Event.Type.HIT_WALL, p.x, p.y, p.kind));
        }
    }

    // ---- pickups ------------------------------------------------------------------------------------

    private void pickUp() {
        for (Pickup p : pickups) {
            if (p.taken || Math.hypot(p.x - player.x, p.y - player.y) > PICKUP_RANGE) continue;
            if (take(p.kind)) {
                p.taken = true;
                player.glint = 1;
                events.add(new Event(Event.Type.PICKUP, p.x, p.y, p.kind));
            }
        }
    }

    private boolean take(PickupKind kind) {
        switch (kind) {
            case PATCH, MEDKIT -> {
                if (player.health >= Player.MAX_HEALTH) return false;
                player.health = Math.min(Player.MAX_HEALTH, player.health + (kind == PickupKind.PATCH ? 15 : 40));
                return true;
            }
            case PLATING, HEAVY_PLATING -> {
                if (player.plating >= Player.MAX_PLATING) return false;
                player.plating = Math.min(Player.MAX_PLATING, player.plating + (kind == PickupKind.PLATING ? 25 : 100));
                return true;
            }
            case RIVETS -> { return player.add(Ammo.RIVETS, 20); }
            case CELLS -> { return player.add(Ammo.CELLS, 40); }
            case GAS -> { return player.add(Ammo.GAS, 8); }
            case CHARGES -> { return player.add(Ammo.CHARGES, 2); }
            case CHIP_GREEN, CHIP_AMBER, CHIP_VIOLET -> {
                player.chips.add(kind.chip());
                return true;
            }
            default -> {
                Weapon w = kind.weapon();
                boolean fresh = player.owned.add(w);
                int amount = switch (w) {
                    case SCATTER_RIVETER -> 20;
                    case ARC_WELDER -> 60;
                    case PLASMA_CUTTER -> 10;
                    default -> 3;
                };
                boolean fitted = player.add(w.ammo(), amount);
                if (fresh) {
                    player.weapon = w;
                    events.add(new Event(Event.Type.WEAPON_CHANGED, player.x, player.y, w));
                }
                return fresh || fitted;
            }
        }
    }

    // ---- questions ----------------------------------------------------------------------------------

    private boolean occupied(int x, int y) {
        if ((int) player.x == x && (int) player.y == y) return true;
        for (Machine m : machines) if (m.alive() && (int) m.x == x && (int) m.y == y) return true;
        return false;
    }

    static double angleTo(double x, double y, double tx, double ty) {
        return Math.atan2(ty - y, tx - x);
    }

    static double normalize(double a) {
        a %= Math.PI * 2;
        if (a > Math.PI) a -= Math.PI * 2;
        if (a < -Math.PI) a += Math.PI * 2;
        return a;
    }

    public LevelData level() {
        return level;
    }

    public GridMap map() {
        return map;
    }

    public Player player() {
        return player;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public List<Machine> machines() {
        return machines;
    }

    public List<Projectile> projectiles() {
        return projectiles;
    }

    public List<Pickup> pickups() {
        return pickups;
    }

    public List<Prop> props() {
        return props;
    }

    /** The lamps of the level, for its light map. */
    public List<LightMap.Light> lights() {
        return lights;
    }

    /** What happened in the last step. */
    public List<Event> events() {
        return events;
    }

    /** The colour of the lock of a door, or {@code null}. */
    public Chip lock(Door door) {
        return locks.get(door);
    }

    public boolean secret(Door door) {
        return secrets.contains(door);
    }

    public double time() {
        return time;
    }

    public int kills() {
        return kills;
    }

    /** The machines there were at the start (drones the Foreman launches do not count). */
    public int machinesAtStart() {
        return machinesAtStart;
    }

    public int secretsFound() {
        return found.size();
    }

    public int secretsTotal() {
        return secrets.size();
    }

    /** Points for machines destroyed and secrets found so far. */
    public int points() {
        return points;
    }

    /** The bonus at the lift: 2 points for every second under par, 100 if no machine was left. */
    public int bonus() {
        int b = (int) Math.max(0, (level.par() - time) * 2);
        if (kills >= machinesAtStart) b += 100;
        return b;
    }

    public boolean done() {
        return done;
    }
}
