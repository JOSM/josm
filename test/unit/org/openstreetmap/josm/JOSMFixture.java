// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainPanel;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

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
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Main.pref.resetToInitialState();
        Main.pref.enableSaveOnPut(false);
        I18n.init();
        // initialize the plaform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we anything else
        Main.platform.preStartupHook();

        Logging.setLogLevel(Logging.LEVEL_INFO);
        Main.pref.init(false);
        String url = Main.pref.get("osm-server.url");
        if (url == null || url.isEmpty() || isProductionApiUrl(url)) {
            Main.pref.put("osm-server.url", "http://api06.dev.openstreetmap.org/api");
        }
        I18n.set(Main.pref.get("language", "en"));

        try {
            CertificateAmendment.addMissingCertificates();
        } catch (IOException | GeneralSecurityException ex) {
            throw new JosmRuntimeException(ex);
        }

        // init projection
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

        // make sure we don't upload to or test against production
        url = OsmApi.getOsmApi().getBaseUrl().toLowerCase(Locale.ENGLISH).trim();
        if (isProductionApiUrl(url)) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }

        if (createGui) {
            GuiHelper.runInEDTAndWaitWithException(new Runnable() {
                @Override
                public void run() {
                    setupGUI();
                }
            });
        }
    }

    private static boolean isProductionApiUrl(String url) {
        return url.startsWith("http://www.openstreetmap.org") || url.startsWith("http://api.openstreetmap.org")
            || url.startsWith("https://www.openstreetmap.org") || url.startsWith("https://api.openstreetmap.org");
    }

    private void setupGUI() {
        JOSMTestRules.cleanLayerEnvironment();
        assertTrue(Main.getLayerManager().getLayers().isEmpty());
        assertNull(Main.getLayerManager().getEditLayer());
        assertNull(Main.getLayerManager().getActiveLayer());

        initContentPane();
        initMainPanel();
        initToolbar();
        if (Main.main == null) {
            new MainApplication().initialize();
        } else {
            if (Main.main.panel == null) {
                initMainPanel();
                Main.main.panel = Main.mainPanel;
            }
            Main.main.panel.reAddListeners();
        }
        // Add a test layer to the layer manager to get the MapFrame
        Main.getLayerManager().addLayer(new TestLayer());
    }

    /**
     * Make sure {@code Main.contentPanePrivate} is initialized.
     */
    public static void initContentPane() {
        if (Main.contentPanePrivate == null) {
            Main.contentPanePrivate = new JPanel(new BorderLayout());
        }
    }

    /**
     * Make sure {@code Main.mainPanel} is initialized.
     */
    public static void initMainPanel() {
        if (Main.mainPanel == null) {
            Main.mainPanel = new MainPanel(Main.getLayerManager());
        }
    }

    /**
     * Make sure {@code Main.toolbar} is initialized.
     */
    public static void initToolbar() {
        if (Main.toolbar == null) {
            Main.toolbar = new ToolbarPreferences();
        }
    }
}
