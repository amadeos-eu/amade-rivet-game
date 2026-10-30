package eu.amadeos.games.rivet.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.games.rivet.bootstrap.RivetModule;
import eu.amadeos.games.rivet.domain.Ammo;
import eu.amadeos.games.rivet.domain.Chip;
import eu.amadeos.games.rivet.domain.Difficulty;
import eu.amadeos.games.rivet.domain.PickupKind;
import eu.amadeos.games.rivet.domain.Run;
import eu.amadeos.games.rivet.domain.Weapon;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(RivetModule.class, RivetModule.MESSAGES));
    }

    @Test
    void everyThingInTheGameHasAName() {
        Messages msg = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.ENGLISH);
        for (Weapon w : Weapon.values()) assertTrue(msg.has(w.key()), w.key());
        for (Ammo a : Ammo.values()) assertTrue(msg.has(a.key()), a.key());
        for (Chip c : Chip.values()) assertTrue(msg.has(c.key()), c.key());
        for (Difficulty d : Difficulty.values()) assertTrue(msg.has(d.key()), d.key());
        for (PickupKind k : PickupKind.values()) {
            if (k.chip() == null && k.weapon() == null) assertTrue(msg.has(k.key()), k.key());
        }
        for (String level : Run.LEVELS) assertTrue(msg.has("level." + level), level);
        for (int s = 1; s <= Run.SECTIONS; s++) assertTrue(msg.has("section." + s));
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.GERMANY);
        assertEquals("Diese Tür braucht den Zugangschip „Grün“", german.format("message.locked", german.get("chip.green")));
        assertEquals("Lauf endete in Level 2-3: 1.250 Punkte (Schwer).",
                german.format("scores.reached", 1250, "2-3", german.get("difficulty.hard")));
        Messages french = Messages.load(RivetModule.class, RivetModule.MESSAGES, Locale.FRANCE);
        assertEquals("Le cœur du Contremaître s'éteint. Portez sa puce à l'ascenseur.", french.format("message.foreman", 1));
    }

    /** The Game menu mixes the engine's entries with the game's own; no two may share a letter. */
    @Test
    void mnemonicsAreUniqueInTheGameMenu() {
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages engine = GameTexts.load(language.locale());
            Messages game = Messages.load(RivetModule.class, RivetModule.MESSAGES, language.locale());
            List<String> texts = List.of(engine.get("menu.game.new"), game.get("menu.game.pause"),
                    game.get("menu.game.sound"), game.get("menu.game.mouse"), engine.get("menu.game.graphics"),
                    engine.get("menu.game.frame-times"), engine.get("menu.game.full-screen"), engine.get("menu.game.scores"),
                    engine.get("menu.game.exit"));
            Map<Character, String> seen = new HashMap<>();
            for (String text : texts) {
                Mnemonics.Text t = Mnemonics.parse(text);
                if (t.index() < 0) {
                    clashes.put(language.code() + " " + text, "no mnemonic");
                    continue;
                }
                String other = seen.put(Character.toLowerCase(t.text().charAt(t.index())), text);
                if (other != null) clashes.put(language.code() + " " + text, other);
            }
        }
        assertEquals(Map.of(), clashes);
    }
}
