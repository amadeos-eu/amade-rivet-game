package eu.amadeos.games.rivet.domain;

import eu.amadeos.gameengine.random.SeededRandom;

import java.util.List;

/**
 * A run through the station, level after level: the player carries health, plating, tools and ammunition
 * from one level to the next, and the points add up. A level lost can be tried again as it was entered.
 */
public final class Run {
    /** The levels in order: three sections of three. */
    public static final List<String> LEVELS = List.of("1-1", "1-2", "1-3", "2-1", "2-2", "2-3", "3-1", "3-2", "3-3");
    public static final int SECTIONS = 3;

    /** How a run ended: the points, how hard it was, the level reached, and whether the core was shut down. */
    public record Result(int score, Difficulty difficulty, String levelId, boolean won) {
    }

    private final Difficulty difficulty;
    private int index;
    private Player player;
    private Player atLevelStart;
    private int score;
    private int scoreAtLevelStart;

    /** A run from the first level of {@code section} (1 to 3), with that section's outfit. */
    public Run(Difficulty difficulty, int section) {
        this.difficulty = difficulty;
        this.index = (Math.max(1, Math.min(SECTIONS, section)) - 1) * 3;
        this.player = Player.outfitFor(section);
    }

    /** Builds the current level. */
    public World start(Surfaces surfaces, SeededRandom random) {
        atLevelStart = player.copy();
        scoreAtLevelStart = score;
        return new World(LevelData.load(LEVELS.get(index)), surfaces, difficulty, player, random);
    }

    /** The level was left by the lift: its points and bonus count, on to the next. */
    public void levelDone(World world) {
        score += world.points() + world.bonus();
        index++;
    }

    /** Back to the start of the current level, with what the player had then. */
    public void retry() {
        player = atLevelStart.copy();
        score = scoreAtLevelStart;
    }

    /** The points of a level left unfinished, when the player gives up. */
    public void giveUp(World world) {
        score = scoreAtLevelStart + world.points();
    }

    public boolean finished() {
        return index >= LEVELS.size();
    }

    public String levelId() {
        return LEVELS.get(Math.min(index, LEVELS.size() - 1));
    }

    /** The section of the current level, 1 to 3. */
    public int section() {
        return Math.min(index, LEVELS.size() - 1) / 3 + 1;
    }

    public int score() {
        return score;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public Player player() {
        return player;
    }

    public Result result() {
        return new Result(score, difficulty, levelId(), finished());
    }
}
