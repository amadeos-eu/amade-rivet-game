# amade-artillery-game

**amade-artillery-game** is the artillery duel of the [amadeOS](https://github.com/datazuul/amadeos) desktop: two to four tanks stand on a landscape of hills and valleys and take turns. Set the barrel's angle and the shot's power, mind the wind, and fire. Every hit blasts a crater out of the ground, earth without support falls down, and a tank whose ground is shot away falls with it. People and computers of three levels play in any mix at one screen. Built on [amade-game-engine](https://github.com/datazuul/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

![Artillery](docs/screenshots/amade-artillery-game.png)

## Playing

- **Enter**, **Space** or **Start** on a gamepad opens the card for a new match from the title, where three computers fight.
- On the card, **up** and **down** choose a row, **left** and **right** change it: the number of players (2 to 4), who drives each tank (a player or the computer, easy, normal or hard), the rounds needed to win the match (1 to 5) and the landscape (one of four, or a different one each round). **Enter** starts, **Esc** goes back. The card remembers the last match. The mouse works too: click a row to choose it, click it again to change it.
- **Left** and **right** (or **A** and **D**) turn the barrel, **up** and **down** (or **W** and **S**) set the power. A tap moves one degree or one step, holding moves faster.
- **Tab** or **E** picks the next weapon, **Q** the one before. **Space** fires.
- With the mouse: hold the button, point and let go. The barrel follows the pointer, and the farther the pointer is from the tank, the stronger the shot.
- The **wind** in the top bar changes with every turn and pushes the shells. The dots show where your last shot flew.
- **Weapons**: the **shell**, as many as you like; the **heavy shell**, a bigger crater; the **triple shot**, three shells fanned out; the **dirt shot**, which heaps up earth instead of blasting it away, to bury a tank or build a wall. Each tank has two of every special weapon per round.
- A tank that falls more than a few units takes damage; a wreck explodes and hurts what stands next to it. The last tank standing wins the round, and the decisive shot is shown once more in slow motion (**Enter** skips it).
- **Points**: every point of damage dealt to others, 50 for each tank destroyed and 100 for a round won. A match won by a player enters the high score table with its points.
- On the **moon** the gravity is weaker and there is no wind.
- **P** or **Esc** pauses, and so does leaving the window; **Esc** in the pause leaves the match.
- Gamepads: d-pad or left stick to aim, **A** fires, **X** or **Y** the next weapon, **B** the one before, **Start** pauses. Players take turns, so any gamepad drives the tank whose turn it is.
- **F11** switches to full screen and back. *Game → Graphics* chooses how the battle reaches the screen: **Direct** (at the screen's full size) or **Pixels** (640 × 400, enlarged by whole factors). **F12** shows how long each frame takes.
- Sound effects and music can be switched off under *Game → Sound*.

The keys can be changed in `~/.config/amadeos/artillery/artillery.properties`, with the names Java gives them, several separated by spaces:

```properties
keys.left=LEFT A
keys.right=RIGHT D
keys.up=UP W
keys.down=DOWN S
keys.weapon=TAB E
keys.previous-weapon=Q
keys.fire=SPACE
keys.pause=P ESCAPE
```

Texts in English, Deutsch, Français, Español and Italiano.

## Gamepads

Plugged in before or while the game runs; any of them plays. *Help → About* shows how many are connected.

- **Linux**: the game reads `/dev/input/js0` to `js3`, which needs read access to them (on most desktops the user at the screen has it). The buttons follow the Xbox layout of the `xpad` driver.
- **Windows**: XInput, so Xbox and compatible controllers.
- Elsewhere the game runs without gamepads.

## Files

| What | Where |
|---|---|
| Settings (graphics, sound, keys, the last match's setup) | `~/.config/amadeos/artillery/artillery.properties` |
| Best matches | `~/.local/share/amadeos/games/artillery/` |

## Building

```bash
./gradlew test          # the window test needs a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-artillery-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
./gradlew benchmark     # the frame times of every landscape
```

Gamepads on Windows go through Java's foreign function interface, so the game starts with `--enable-native-access=ALL-UNNAMED` (set in `build.gradle` for `run`, the tests and the packages).

All pictures are drawn with Java2D shapes at run time; the icon is drawn by `tools/MakeIcon.java`. `./gradlew screenshots -Pshots=<folder>` draws the handbook's screenshots.

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface or time of their own. `Battle` (one round: the landscape, aiming, shells in flight, blasts, settling earth, falling tanks, turns and wind, the replay of the decisive shot), `Tank`, `Weapon`, `Theme` (the four landscapes), `Earth` (the kinds of ground), `Computer` (the computer players: whom to aim at, a solved shot and a level's errors), `Match` (rounds, wins and points), `Seat`, `Control`, `Input`, `Event`, `Field` (sizes, physics, damage, points).
- `repository`: `ArtillerySettings` (graphics, sound, keys, the last setup).
- `ui.game`: the running game on the loop thread. `ArtilleryGame` (loop, controls, scenes: title, setup card, play, replay, pause, end of a round and of the match), `SetupCard`, `Painter` (the battle and what moves in it), `GroundPainter` (the breakable ground as an image, repainted where it changed), `Backdrop` (the skies and far views of the landscapes), `TankArt`, `Hud` (top bar, labels, banners, cards), `Effects` (events become particles, damage numbers, sounds and music), `SoundBank`, `Music` (the tunes, in MML), `Look` (colours), `Arena` (what the presenter may ask of the game).
- `ui.main`: Model-View-Presenter for everything around the battle: `ArtilleryPresenter`, `ArtilleryView`, `ArtilleryWindow`.

From the engine: `Ground` and `Landscape` (destructible ground and generated landscapes), `Projectile` and `Ballistics` (shells under gravity, wind and drag), `ShotSolver` (the computer's aim), `TurnOrder`, `HighScores`, `Gamepads`, `GameLoop`, `GameCanvas`, `Mouse`, `Sprite`, `Particles`, `Sound`, `Tune` and `Mixer`, `Keyboard` and `Controls`, `SceneStack`, `FullScreen`.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) quotes; chapter 29 is about this game.

## Licence

[EUPL-1.2](LICENSE)
