// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * JOSM termination sequence.
 * @since 14140
 */
public class MainTermination implements Runnable {

    private final MainApplication application;

    /**
     * Constructs a new {@code MainTermination}
     * @param application Main application. Must not be null
     */
    public MainTermination(MainApplication application) {
        this.application = Objects.requireNonNull(application);
    }

    @Override
    public void run() {
        try {
            MainApplication.worker.shutdown();
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to shutdown worker", e);
        }
        JCSCacheManager.shutdown();

        if (application.getMainFrame() != null) {
            application.getMainFrame().storeState();
        }
        if (MainApplication.getMap() != null) {
            MainApplication.getMap().rememberToggleDialogWidth();
        }
        // Remove all layers because somebody may rely on layerRemoved events (like AutosaveTask)
        MainApplication.getLayerManager().resetState();
        ImageProvider.shutdown(false);
        try {
            Main.pref.saveDefaults();
        } catch (IOException | InvalidPathException ex) {
            Logging.log(Logging.LEVEL_WARN, tr("Failed to save default preferences."), ex);
        }
        ImageProvider.shutdown(true);

        try {
            // in case the current task still hasn't finished
            MainApplication.worker.shutdownNow();
        } catch (SecurityException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to shutdown worker", e);
        }
    }
}

