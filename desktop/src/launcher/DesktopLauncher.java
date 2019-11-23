package launcher;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.esotericsoftware.minlog.Log;
import game.AOGame;
import game.ClientConfiguration;
import game.ClientConfiguration.Init;
import game.ClientConfiguration.Init.Video;
import game.utils.Resources;

public class DesktopLauncher {

    public static void main(String[] arg) {
        System.setProperty("org.lwjgl.opengl.Display.enableOSXFullscreenModeAPI", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Finisterra");

        /**
         * Load desktop config.json or create default.
         */
        ClientConfiguration config = ClientConfiguration.loadConfig(Resources.CLIENT_CONFIG);
        if (config == null) {
            Log.info("DesktopLauncher", "Desktop config.json not found, creating default.");
            config = ClientConfiguration.createConfig();
            config.save(Resources.CLIENT_CONFIG);
        }
        Init initConfig = config.getInitConfig();
        Video video = initConfig.getVideo();

        /**
         * Build LWJGL configuration
         */
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Finisterra - Argentum Online Java");
        cfg.setWindowedMode(video.getWidth(), video.getHeight());
        cfg.useVsync(video.getVsync());
        cfg.setIdleFPS(72);
        cfg.setResizable(initConfig.isResizeable());
        cfg.disableAudio(initConfig.isDisableAudio());
        cfg.setMaximized(initConfig.isStartMaximized());

        if (video.getHiDPIMode().equalsIgnoreCase("Pixels")) {
            cfg.setHdpiMode(HdpiMode.Pixels);
        } else {
            cfg.setHdpiMode(HdpiMode.Logical);
        }

        /**
         * Set the icon that will be used in the window's title bar and in MacOS's dock bar.
         */
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            /*
                TODO: Add icon image handler fot Mac.
            */
        } else {
            cfg.setWindowIcon(Resources.CLIENT_ICON);
        }

        // Log in console. Un-comment the rest if you wish to debug Config.json's I/O
        Log.info("Initializing game...");
        //Log.info("[Parameters - Window] Width: " + video.getWidth());
        //Log.info("[Parameters - Window] Height: " + video.getHeight());
        //Log.info("[Parameters - Window] Start Maximized: " + initConfig.isStartMaximized());
        //Log.info("[Parameters - Window] Resizeable: " + initConfig.isResizeable());
        //Log.info("[Parameters - Graphics] vSync: " + video.getVsync());
        //Log.info("[Parameters - Graphics] HiDPI Mode: " + video.getHiDPIMode());
        //Log.info("[Parameters - Audio] Disabled: " + initConfig.isDisableAudio());

        /**
         * Launch application
         */
        new Lwjgl3Application(new AOGame(config), cfg);
    }
}
