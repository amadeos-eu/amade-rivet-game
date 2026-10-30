package eu.amadeos.games.rivet.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.input.MouseLook;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.rivet.repository.RivetSettings;
import eu.amadeos.games.rivet.ui.game.Arena;
import eu.amadeos.games.rivet.ui.game.RivetGame;
import eu.amadeos.games.rivet.ui.main.RivetPresenter;
import eu.amadeos.games.rivet.ui.main.RivetView;
import eu.amadeos.games.rivet.ui.main.RivetWindow;

import java.util.Locale;

/**
 * amade-rivet-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class RivetModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.rivet.messages";

    @Override
    protected void configure() {
        bind(RivetSettings.class).in(Scopes.SINGLETON);
        bind(Keyboard.class).in(Scopes.SINGLETON);
        bind(Mouse.class).in(Scopes.SINGLETON);
        bind(MouseLook.class).in(Scopes.SINGLETON);
        bind(Mixer.class).in(Scopes.SINGLETON);
        bind(RivetGame.class).in(Scopes.SINGLETON);
        bind(Arena.class).to(RivetGame.class);
        bind(RivetWindow.class).in(Scopes.SINGLETON);
        bind(RivetView.class).to(RivetWindow.class);
        bind(RivetPresenter.class).in(Scopes.SINGLETON);
    }

    /** The best runs in {@code ~/.local/share/amadeos/games/rivet}. */
    @Provides
    @Singleton
    HighScores highScores(XdgDirectories xdg, AppInfo app) {
        return HighScores.of(xdg, app.configName(), Ranking.HIGHER_WINS);
    }

    @Provides
    @Singleton
    PlayerName playerName(AppDirectories directories) {
        return new PlayerName(directories.config(), System.getProperty("user.name"));
    }

    /** The gamepads of this computer, by the backend of its operating system. */
    @Provides
    @Singleton
    Gamepads gamepads() {
        return Gamepads.open();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(RivetModule.class, MESSAGES, locale);
    }
}
