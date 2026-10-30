package eu.amadeos.games.rivet.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.raycast.LightMap;
import eu.amadeos.gameengine.raycast.Views;
import eu.amadeos.games.rivet.domain.Chip;
import eu.amadeos.games.rivet.domain.Event;
import eu.amadeos.games.rivet.domain.MachineKind;
import eu.amadeos.games.rivet.domain.PickupKind;
import eu.amadeos.games.rivet.domain.Player;
import eu.amadeos.games.rivet.domain.Projectile;
import eu.amadeos.games.rivet.domain.Weapon;
import eu.amadeos.games.rivet.domain.World;

import java.util.ArrayList;
import java.util.List;

/**
 * What the events of a step look and sound like: shots and their flashes of light, sparks where a rivet
 * strikes, blasts, the freezing cloud of a canister, the messages at the top, the red edge when the suit is
 * hit and from where, the shaking of the view. The rules know nothing of this.
 */
final class Effects {
    static final double MESSAGE_SECONDS = 2.6;
    /** Machines further away than this are not heard. */
    static final double HEARING = 16;

    /** A picture that plays once where something happened: sparks, a blast, a cloud. */
    record Sprite(Views views, double x, double y, double lift, double size, double born, double seconds) {
    }

    /** A line at the top of the view. */
    record Message(String text, int colour, double born) {
    }

    private final SoundBank sounds = new SoundBank();
    private final Music music = new Music();
    private final Mixer mixer;
    private final Messages msg;
    private final List<Sprite> sprites = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private Art art;
    private double time;
    private double muzzle;
    private Weapon muzzleWeapon = Weapon.RIVET_GUN;
    private double hitMarker;
    private double beamX;
    private double beamY;
    private double beam;
    private double shake;
    /** The angle from the player to what hit them last, and how fresh that is. */
    private double hurtFrom;
    private double hurtShown;
    private double raise = 1;

    Effects(Mixer mixer, Messages msg) {
        this.mixer = mixer;
        this.msg = msg;
    }

    void setArt(Art art) {
        this.art = art;
    }

    Art art() {
        return art;
    }

    /** A new level: nothing left over from the one before. */
    void clear() {
        sprites.clear();
        messages.clear();
        muzzle = hitMarker = beam = shake = hurtShown = 0;
        raise = 1;
    }

    // ---- music ---------------------------------------------------------------------------------------

    void titleMusic() {
        mixer.playMusic(music.title, true);
    }

    void sectionMusic(int section) {
        mixer.playMusic(music.section[Math.max(0, Math.min(music.section.length - 1, section - 1))], true);
    }

    void levelDoneMusic() {
        mixer.playMusic(music.levelDone, false);
    }

    void wonMusic() {
        mixer.playMusic(music.won, false);
    }

    void deadMusic() {
        mixer.playMusic(music.dead, false);
    }

    void stopMusic() {
        mixer.stopMusic();
    }

    void play(Sound sound) {
        mixer.play(sound);
    }

    SoundBank sounds() {
        return sounds;
    }

    // ---- the events of a step ------------------------------------------------------------------------

    // --8<-- [start:react]
    void react(World world) {
        Player p = world.player();
        for (Event e : world.events()) {
            boolean near = Math.hypot(e.x() - p.x(), e.y() - p.y()) < HEARING;
            switch (e.type()) {
                case SHOT -> {
                    Weapon w = (Weapon) e.detail();
                    mixer.play(sounds.shot(w));
                    if (w != Weapon.ICE_PICK && w != Weapon.BLASTING_CHARGE) {
                        muzzle = 1;
                        muzzleWeapon = w;
                    }
                }
                case BEAM -> {
                    mixer.play(sounds.weld);
                    beamX = e.x();
                    beamY = e.y();
                    beam = 1;
                    sprite(art == null ? null : art.puff, e.x(), e.y(), 0.35, 0.35, 0.2);
                }
                case MELEE_HIT -> mixer.play(sounds.clank);
                case HIT_WALL -> {
                    if (near) mixer.play(sounds.tink);
                    boolean big = e.detail() == Projectile.Kind.ICE_BLOCK;
                    sprite(art == null ? null : big ? art.cloud : art.puff, e.x(), e.y(), big ? 0 : 0.4, big ? 0.8 : 0.35, big ? 0.5 : 0.25);
                }
                case HIT_MACHINE -> {
                    mixer.play(sounds.clank);
                    hitMarker = 1;
                }
                case MACHINE_ALERTED -> {
                    if (near) mixer.play(sounds.alert((MachineKind) e.detail()));
                }
                case MACHINE_ATTACKS -> {
                    if (near) mixer.play(sounds.attack((MachineKind) e.detail()));
                }
                case MACHINE_DESTROYED -> {
                    mixer.play(sounds.crunch);
                    MachineKind k = (MachineKind) e.detail();
                    double size = k == MachineKind.FOREMAN ? 2.4 : k == MachineKind.LOADER ? 1.4 : 0.9;
                    sprite(art == null ? null : art.explosion, e.x(), e.y(), 0.05, size, 0.6);
                    if (k == MachineKind.FOREMAN) message(msg.get("message.foreman"), 0xc6a8ff);
                }
                case PLAYER_HURT -> {
                    mixer.play(sounds.hurt);
                    hurtFrom = Math.atan2(e.y() - p.y(), e.x() - p.x());
                    hurtShown = 1;
                    shake = Math.max(shake, Math.min(1, ((Number) e.detail()).doubleValue() / 30));
                }
                case PLAYER_DIED -> mixer.play(sounds.died);
                case PICKUP -> pickup((PickupKind) e.detail());
                case DOOR_OPENS -> mixer.play(sounds.door);
                case LOCKED -> {
                    mixer.play(sounds.locked);
                    Chip chip = (Chip) e.detail();
                    message(msg.format("message.locked", msg.get(chip.key())), Look.chip(chip));
                }
                case SECRET_FOUND -> {
                    mixer.play(sounds.secret);
                    message(msg.get("message.secret"), 0xffe27a);
                }
                case EXPLOSION -> {
                    mixer.play(sounds.explosion);
                    double radius = ((Number) e.detail()).doubleValue();
                    sprite(art == null ? null : art.explosion, e.x(), e.y(), 0, radius * 0.9, 0.7);
                    double d = Math.hypot(e.x() - p.x(), e.y() - p.y());
                    shake = Math.max(shake, Math.max(0, 1 - d / (radius * 3)));
                }
                case CANISTER_BURSTS -> {
                    mixer.play(sounds.hiss);
                    sprite(art == null ? null : art.cloud, e.x(), e.y(), 0, 2.2, 1.4);
                }
                case EMPTY -> mixer.play(sounds.empty);
                case WEAPON_CHANGED -> {
                    mixer.play(sounds.change);
                    raise = 1;
                }
                case DRONES_LAUNCHED -> {
                    mixer.play(sounds.launch);
                    message(msg.get("message.drones"), 0xc6a8ff);
                }
                case LEVEL_DONE -> mixer.play(sounds.lift);
            }
        }
    }
    // --8<-- [end:react]

    private void pickup(PickupKind kind) {
        if (kind.chip() != null) {
            mixer.play(sounds.chip);
            message(msg.format("message.chip", msg.get(kind.chip().key())), Look.chip(kind.chip()));
        } else if (kind.weapon() != null) {
            mixer.play(sounds.gotTool);
            message(msg.format("message.tool", msg.get(kind.weapon().key())), 0xffe27a);
        } else {
            mixer.play(sounds.pickup);
            message(msg.get(kind.key()), 0x8ff0b4);
        }
    }

    private void sprite(Views views, double x, double y, double lift, double size, double seconds) {
        if (views != null) sprites.add(new Sprite(views, x, y, lift, size, time, seconds));
    }

    void message(String text, int colour) {
        messages.removeIf(m -> m.text().equals(text));
        messages.add(new Message(text, colour, time));
        while (messages.size() > 3) messages.removeFirst();
    }

    void update(double seconds) {
        time += seconds;
        muzzle = Math.max(0, muzzle - seconds * 12);
        hitMarker = Math.max(0, hitMarker - seconds * 5);
        beam = Math.max(0, beam - seconds * 8);
        shake = Math.max(0, shake - seconds * 2.5);
        hurtShown = Math.max(0, hurtShown - seconds * 1.2);
        raise = Math.max(0, raise - seconds * 4);
        sprites.removeIf(s -> time - s.born() > s.seconds());
        messages.removeIf(m -> time - m.born() > MESSAGE_SECONDS);
    }

    // --8<-- [start:flashes]
    /** The lights of this moment: shots, blasts, glowing shots in flight. */
    void lights(World world, LightMap lights) {
        Player p = world.player();
        if (muzzle > 0) {
            double c = Math.cos(p.angle()) * 0.5, s = Math.sin(p.angle()) * 0.5, k = muzzle;
            if (muzzleWeapon == Weapon.PLASMA_CUTTER) lights.flash(new LightMap.Light(p.x() + c, p.y() + s, 0.3 * k, 0.9 * k, 1.1 * k, 4));
            else lights.flash(new LightMap.Light(p.x() + c, p.y() + s, 1.3 * k, 0.9 * k, 0.45 * k, 4));
        }
        if (beam > 0) lights.flash(new LightMap.Light(beamX, beamY, 0.5, 0.8, 1.3, 3));
        for (Projectile pr : world.projectiles()) {
            switch (pr.kind()) {
                case PLASMA -> lights.flash(new LightMap.Light(pr.x(), pr.y(), 0.2, 0.7, 0.9, 2.5));
                case SPARK -> lights.flash(new LightMap.Light(pr.x(), pr.y(), 0.7, 0.45, 0.15, 1.8));
                case BOLT -> lights.flash(new LightMap.Light(pr.x(), pr.y(), 0.55, 0.3, 0.9, 2.5));
                default -> {
                }
            }
        }
        for (Sprite s : sprites) {
            if (art == null || s.views() != art.explosion) continue;
            double k = 1 - (time - s.born()) / s.seconds();
            lights.flash(new LightMap.Light(s.x(), s.y(), 1.6 * k, 0.9 * k, 0.35 * k, 2 + s.size() * 2));
        }
    }
    // --8<-- [end:flashes]

    double time() {
        return time;
    }

    List<Sprite> sprites() {
        return sprites;
    }

    List<Message> messages() {
        return messages;
    }

    /** 1 right after a shot, fading quickly. */
    double muzzle() {
        return muzzle;
    }

    double hitMarker() {
        return hitMarker;
    }

    double beam() {
        return beam;
    }

    double beamX() {
        return beamX;
    }

    double beamY() {
        return beamY;
    }

    double shake() {
        return shake;
    }

    double hurtFrom() {
        return hurtFrom;
    }

    double hurtShown() {
        return hurtShown;
    }

    /** 1 when a tool was just taken in hand, going to 0 as it comes up into view. */
    double raise() {
        return raise;
    }
}
