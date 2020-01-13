// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter.SettingsAdapter;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.spi.lifecycle.InitializationSequence;
import org.openstreetmap.josm.spi.lifecycle.InitializationTask;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Tag2Link;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.Utils;

/**
 * JOSM initialization sequence.
 * @since 14139
 */
public class MainInitialization implements InitializationSequence {

    private final MainApplication application;

    /**
     * Constructs a new {@code MainInitialization}
     * @param application Main application. Must not be null
     */
    public MainInitialization(MainApplication application) {
        this.application = Objects.requireNonNull(application);
    }

    @Override
    public List<InitializationTask> beforeInitializationTasks() {
        return Arrays.asList(
            new InitializationTask(tr("Initializing coordinate format"), () -> {
                ICoordinateFormat fmt = CoordinateFormatManager.getCoordinateFormat(Config.getPref().get("coordinates"));
                if (fmt == null) {
                    fmt = DecimalDegreesCoordinateFormat.INSTANCE;
                }
                CoordinateFormatManager.setCoordinateFormat(fmt);
            }),
            new InitializationTask(tr("Starting file watcher"), FileWatcher.getDefaultInstance()::start),
            new InitializationTask(tr("Executing platform startup hook"),
                    () -> PlatformManager.getPlatform().startupHook(MainApplication::askUpdateJava)),
            new InitializationTask(tr("Building main menu"), application::initializeMainWindow),
            new InitializationTask(tr("Updating user interface"), () -> {
                UndoRedoHandler.getInstance().addCommandQueueListener(application.redoUndoListener);
                // creating toolbar
                GuiHelper.runInEDTAndWait(() -> MainApplication.contentPanePrivate.add(MainApplication.toolbar.control, BorderLayout.NORTH));
                // help shortcut
                MainApplication.registerActionShortcut(MainApplication.menu.help,
                        Shortcut.registerShortcut("system:help", tr("Help"), KeyEvent.VK_F1, Shortcut.DIRECT));
            }),
            // This needs to be done before RightAndLefthandTraffic::initialize is called
            new InitializationTask(tr("Initializing internal boundaries data"), Territories::initialize)
        );
    }

    @Override
    public Collection<InitializationTask> parallelInitializationTasks() {
        return Arrays.asList(
            new InitializationTask(tr("Initializing OSM API"), () -> {
                    OsmApi.addOsmApiInitializationListener(api -> {
                        // This checks if there are any layers currently displayed that are now on the blacklist, and removes them.
                        // This is a rare situation - probably only occurs if the user changes the API URL in the preferences menu.
                        // Otherwise they would not have been able to load the layers in the first place because they would have been disabled
                        if (MainApplication.isDisplayingMapView()) {
                            for (Layer l : MainApplication.getLayerManager().getLayersOfType(ImageryLayer.class)) {
                                if (((ImageryLayer) l).getInfo().isBlacklisted()) {
                                    Logging.info(tr("Removed layer {0} because it is not allowed by the configured API.", l.getName()));
                                    MainApplication.getLayerManager().removeLayer(l);
                                }
                            }
                        }
                    });
                    // We try to establish an API connection early, so that any API
                    // capabilities are already known to the editor instance. However
                    // if it goes wrong that's not critical at this stage.
                    try {
                        OsmApi.getOsmApi().initialize(null, true);
                    } catch (OsmTransferCanceledException | OsmApiInitializationException | SecurityException e) {
                        Logging.warn(Logging.getErrorMessage(Utils.getRootCause(e)));
                    }
                }),
            new InitializationTask(tr("Initializing internal traffic data"), RightAndLefthandTraffic::initialize),
            new InitializationTask(tr("Initializing validator"), OsmValidator::initialize),
            new InitializationTask(tr("Initializing presets"), TaggingPresets::initialize),
            new InitializationTask(tr("Initializing map styles"), MapPaintPreference::initialize),
            new InitializationTask(tr("Initializing Tag2Link rules"), Tag2Link::initialize),
            new InitializationTask(tr("Loading imagery preferences"), ImageryPreference::initialize)
        );
    }

    @Override
    public List<Callable<?>> asynchronousCallableTasks() {
        return Arrays.asList(
                OverpassTurboQueryWizard::getInstance
            );
    }

    @Override
    public List<Runnable> asynchronousRunnableTasks() {
        return Arrays.asList(
                TMSLayer::getCache,
                OsmValidator::initializeTests
            );
    }

    @Override
    public List<InitializationTask> afterInitializationTasks() {
        return Arrays.asList(
            new InitializationTask(tr("Updating user interface"), () -> GuiHelper.runInEDTAndWait(() -> {
                // hooks for the jmapviewer component
                FeatureAdapter.registerBrowserAdapter(OpenBrowser::displayUrl);
                FeatureAdapter.registerImageAdapter(ImageProvider::read);
                FeatureAdapter.registerTranslationAdapter(I18n::tr);
                FeatureAdapter.registerLoggingAdapter(name -> Logging.getLogger());
                FeatureAdapter.registerSettingsAdapter(new JosmSettingsAdapter());
                // UI update
                MainApplication.toolbar.refreshToolbarControl();
                MainApplication.toolbar.control.updateUI();
                MainApplication.contentPanePrivate.updateUI();
            }))
        );
    }

    private static class JosmSettingsAdapter implements SettingsAdapter {

        @Override
        public String get(String key, String def) {
            return Config.getPref().get(key, def);
        }

        @Override
        public boolean put(String key, String value) {
            return Config.getPref().put(key, value);
        }
    }
}
