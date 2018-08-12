// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainApplicationTest;
import org.openstreetmap.josm.gui.MainInitialization;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.lifecycle.Lifecycle;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Fixture to define a proper and safe environment before running tests.
 */
public class JOSMFixture {

    /**
     * Returns a new test fixture initialized to "unit" home.
     * @return A new test fixture for unit tests
     */
    public static JOSMFixture createUnitTestFixture() {
        return new JOSMFixture("test/config/unit-josm.home");
    }

    /**
     * Returns a new test fixture initialized to "functional" home.
     * @return A new test fixture for functional tests
     */
    public static JOSMFixture createFunctionalTestFixture() {
        return new JOSMFixture("test/config/functional-josm.home");
    }

    /**
     * Returns a new test fixture initialized to "performance" home.
     * @return A new test fixture for performance tests
     */
    public static JOSMFixture createPerformanceTestFixture() {
        return new JOSMFixture("test/config/performance-josm.home");
    }

    private final String josmHome;

    /**
     * Constructs a new text fixture initialized to a given josm home.
     * @param josmHome The user home where preferences are to be read/written
     */
    public JOSMFixture(String josmHome) {
        this.josmHome = josmHome;
    }

    /**
     * Initializes the test fixture, without GUI.
     */
    public void init() {
        init(false);
    }

    /**
     * Initializes the test fixture, with or without GUI.
     * @param createGui if {@code true} creates main GUI components
     */
    public void init(boolean createGui) {

        // check josm.home
        //
        if (josmHome == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
        } else {
            File f = new File(josmHome);
            if (!f.exists() || !f.canRead()) {
                fail(MessageFormat.format(
                        // CHECKSTYLE.OFF: LineLength
                        "property ''{0}'' points to ''{1}'' which is either not existing ({2}) or not readable ({3}). Current directory is ''{4}''.",
                        // CHECKSTYLE.ON: LineLength
                        "josm.home", josmHome, f.exists(), f.canRead(), Paths.get("").toAbsolutePath()));
            }
        }
        System.setProperty("josm.home", josmHome);
        TimeZone.setDefault(DateUtils.UTC);
        Config.setPreferencesInstance(Main.pref);
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        Config.setUrlsProvider(JosmUrls.getInstance());
        Main.pref.resetToInitialState();
        Main.pref.enableSaveOnPut(false);
        I18n.init();
        // initialize the plaform hook, and
        // call the really early hook before we anything else
        PlatformManager.getPlatform().preStartupHook();

        Logging.setLogLevel(Logging.LEVEL_INFO);
        Main.pref.init(false);
        String url = Config.getPref().get("osm-server.url");
        if (url == null || url.isEmpty() || isProductionApiUrl(url)) {
            Config.getPref().put("osm-server.url", "https://api06.dev.openstreetmap.org/api");
        }
        I18n.set(Config.getPref().get("language", "en"));

        try {
            CertificateAmendment.addMissingCertificates();
        } catch (IOException | GeneralSecurityException ex) {
            throw new JosmRuntimeException(ex);
        }

        // init projection
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

        // setup projection grid files
        MainApplication.setupNadGridSources();

        // make sure we don't upload to or test against production
        url = OsmApi.getOsmApi().getBaseUrl().toLowerCase(Locale.ENGLISH).trim();
        if (isProductionApiUrl(url)) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }

        // Setup callbacks
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);

        if (createGui) {
            GuiHelper.runInEDTAndWaitWithException(() -> setupGUI());
        }
    }

    private static boolean isProductionApiUrl(String url) {
        return url.startsWith("http://www.openstreetmap.org") || url.startsWith("http://api.openstreetmap.org")
            || url.startsWith("https://www.openstreetmap.org") || url.startsWith("https://api.openstreetmap.org");
    }

    private void setupGUI() {
        JOSMTestRules.cleanLayerEnvironment();
        assertTrue(MainApplication.getLayerManager().getLayers().isEmpty());
        assertNull(MainApplication.getLayerManager().getEditLayer());
        assertNull(MainApplication.getLayerManager().getActiveLayer());

        initContentPane();
        initMainPanel(false);
        initToolbar();
        if (Main.main == null) {
            Lifecycle.initialize(new MainInitialization(new MainApplication()));
        }
        // Add a test layer to the layer manager to get the MapFrame
        MainApplication.getLayerManager().addLayer(new TestLayer());
    }

    /**
     * Make sure {@code MainApplication.contentPanePrivate} is initialized.
     */
    public static void initContentPane() {
        MainApplicationTest.initContentPane();
    }

    /**
     * Make sure {@code MainApplication.mainPanel} is initialized.
     */
    public static void initMainPanel() {
        initMainPanel(false);
    }

    /**
     * Make sure {@code MainApplication.mainPanel} is initialized.
     * @param reAddListeners {@code true} to re-add listeners
     */
    public static void initMainPanel(boolean reAddListeners) {
        MainApplicationTest.initMainPanel(reAddListeners);
    }

    /**
     * Make sure {@code MainApplication.toolbar} is initialized.
     */
    public static void initToolbar() {
        MainApplicationTest.initToolbar();
    }

    /**
     * Make sure {@code MainApplication.menu} is initialized.
     */
    public static void initMainMenu() {
        MainApplicationTest.initMainMenu();
    }
}
