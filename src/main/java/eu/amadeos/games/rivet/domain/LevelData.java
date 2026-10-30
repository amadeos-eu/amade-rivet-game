package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.raycast.LightMap;

import java.util.ArrayList;
import java.util.List;

/**
 * A level as written in its file, {@code levels/1-2.txt}: the name, where the player looks at the start,
 * the par time, fog, light and the map, one character per cell.
 *
 * <pre>
 * name Pump Hall
 * start 0             # the player looks east (90 = south)
 * par 150             # seconds; faster gives a bonus
 * fog 0a1219 0.075    # colour and density
 * ambient 0.09 0.09 0.11
 * floor grate         # plate, grate, snow, ice or rock
 * light 21.4 7.5 1.3 0.62 0.18 4
 * map
 * RRRRRRRRRR
 * R@..,..d.X
 * RRRRRRRRRR
 * end
 * </pre>
 *
 * The characters of the map: walls {@code P} panel, {@code H} hazard panel, {@code T} pipes, {@code C}
 * console, {@code I} ice, {@code R} rock, {@code K} stacked crates, {@code M} pump, {@code X} the lift's
 * switch, which ends the level. Open cells: {@code .} floor, {@code ,} a white ceiling lamp, {@code ;} a
 * blue one, {@code :} an amber one, {@code _} open sky. Doors {@code -} (running west to east) and
 * {@code |}, locked doors {@code g a v} (green, amber, violet) and secret doors {@code s}. {@code @} is the
 * start; machines {@code d c t l f}; pickups as in {@link PickupKind}; crates {@code x} and canisters {@code y}.
 */
public record LevelData(String id, String name, int section, double startAngle, double par, int fog, double fogDensity,
                        double[] ambient, String floor, List<LightMap.Light> lights, List<String> rows) {
    public static final String WALLS = "PHTCIRKMX";

    public int width() {
        return rows.getFirst().length();
    }

    public int height() {
        return rows.size();
    }

    public char at(int x, int y) {
        if (y < 0 || y >= rows.size() || x < 0 || x >= rows.get(y).length()) return 'R';
        return rows.get(y).charAt(x);
    }

    public static boolean wall(char c) {
        return WALLS.indexOf(c) >= 0;
    }

    /** Reads {@code eu/amadeos/games/rivet/levels/<id>.txt} among the resources. */
    public static LevelData load(String id) {
        return parse(id, TextData.load(LevelData.class, "/eu/amadeos/games/rivet/levels/" + id + ".txt"));
    }

    public static LevelData parse(String id, List<TextData.Line> lines) {
        String name = id;
        double start = 0, par = 120, density = 0.075;
        int fog = 0x0a1219;
        double[] ambient = {0.1, 0.1, 0.12};
        String floor = "plate";
        List<LightMap.Light> lights = new ArrayList<>();
        List<String> rows = new ArrayList<>();
        boolean inMap = false;
        TextData.Line mapLine = null;
        for (TextData.Line line : lines) {
            if (inMap) {
                if (line.keyword().equals("end")) {
                    inMap = false;
                    continue;
                }
                if (!rows.isEmpty() && line.keyword().length() != rows.getFirst().length()) {
                    throw line.error("the map's rows must be equally long: " + rows.getFirst().length() + " expected");
                }
                rows.add(line.keyword());
                continue;
            }
            switch (line.keyword()) {
                case "name" -> name = String.join(" ", line.words().subList(1, line.size()));
                case "start" -> start = line.number(1);
                case "par" -> par = line.number(1);
                case "fog" -> {
                    try {
                        fog = Integer.parseInt(line.word(1), 16);
                    } catch (NumberFormatException e) {
                        throw line.error('"' + line.word(1) + "\" is not a colour like 0a1219");
                    }
                    density = line.number(2);
                }
                case "ambient" -> ambient = new double[]{line.number(1), line.number(2), line.number(3)};
                case "floor" -> floor = line.word(1);
                case "light" -> lights.add(new LightMap.Light(line.number(1), line.number(2), line.number(3), line.number(4),
                        line.number(5), line.number(6)));
                case "map" -> {
                    inMap = true;
                    mapLine = line;
                }
                default -> throw line.error('"' + line.keyword() + "\" is not something a level has");
            }
        }
        if (rows.isEmpty()) throw new TextData.DataException(id + ": the level has no map");
        if (inMap) throw mapLine.error("the map has no end");
        int section = Integer.parseInt(id.substring(0, id.indexOf('-')));
        return new LevelData(id, name, section, Math.toRadians(start), par, fog, density, ambient, floor, List.copyOf(lights),
                List.copyOf(rows));
    }
}
