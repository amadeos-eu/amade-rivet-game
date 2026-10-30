package eu.amadeos.games.rivet.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.games.rivet.ui.game.RivetGame;
import eu.amadeos.games.rivet.ui.main.RivetPresenter;
import eu.amadeos.games.rivet.ui.main.RivetWindow;

public final class AmadeRivetApplication {
    public static final AppInfo APP = new AppInfo("amade-rivet-game", "Rivet", "rivet");

    private AmadeRivetApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeRivetApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            Gamepads gamepads = injector.getInstance(Gamepads.class);
            RivetWindow window = injector.getInstance(RivetWindow.class);
            RivetPresenter presenter = injector.getInstance(RivetPresenter.class);
            RivetGame game = injector.getInstance(RivetGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                gamepads.close();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new RivetModule());
    }
    // --8<-- [end:main]
}
