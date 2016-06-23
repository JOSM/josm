// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.tools.I18n;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class runs a test in an environment that resembles the one used by the JOSM main application.
 * <p>
 * The environment is reset before every test. You can specify the components to which you need access using the methods of this class.
 * For example, invoking {@link #preferences()} gives you access to the (default) preferences.
 *
 * @author Michael Zangl
 */
public class JOSMTestRules implements TestRule {
    private Timeout timeout = Timeout.seconds(10);
    private TemporaryFolder josmHome;
    private boolean usePreferences = false;
    private APIType useAPI = APIType.NONE;
    private String i18n = null;
    private boolean platform;
    private boolean useProjection;

    /**
     * Disable the default timeout for this test. Use with care.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules noTimeout() {
        timeout = null;
        return this;
    }

    /**
     * Set a timeout for all tests in this class. Local method timeouts may only reduce this timeout.
     * @param millis The timeout duration in milliseconds.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules timeout(int millis) {
        timeout = Timeout.millis(millis);
        return this;
    }

    /**
     * Enable the use of default preferences.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules preferences() {
        josmHome();
        usePreferences = true;
        return this;
    }

    /**
     * Set JOSM home to a valid, empty directory.
     * @return this instance, for easy chaining
     */
    private JOSMTestRules josmHome() {
        josmHome = new TemporaryFolder();
        return this;
    }

    /**
     * Enables the i18n module for this test in english.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules i18n() {
        return i18n("en");
    }

    /**
     * Enables the i18n module for this test.
     * @param language The language to use.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules i18n(String language) {
        i18n = language;
        return this;
    }

    /**
     * Enable {@link Main#platform} global variable.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules platform() {
        platform = true;
        return this;
    }

    /**
     * Enable the dev.openstreetmap.org API for this test.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules devAPI() {
        preferences();
        useAPI = APIType.DEV;
        return this;
    }

    /**
     * Use the {@link FakeOsmApi} for testing.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules fakeAPI() {
        useAPI = APIType.FAKE;
        return this;
    }

    /**
     * Set up default projection (Mercator)
     * @return this instance, for easy chaining
     */
    public JOSMTestRules projection() {
        useProjection = true;
        return this;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        Statement statement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
        if (timeout != null) {
            statement = new DisableOnDebug(timeout).apply(statement, description);
        }
        if (josmHome != null) {
            statement = josmHome.apply(statement, description);
        }
        return statement;
    }

    /**
     * Set up before running a test
     * @throws InitializationError If an error occured while creating the required environment.
     */
    protected void before() throws InitializationError {
        cleanUpFromJosmFixture();

        // Tests are running headless by default.
        System.setProperty("java.awt.headless", "true");
        // All tests use the same timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Set log level to info
        Main.logLevel = 3;

        // Set up i18n
        if (i18n != null) {
            I18n.set(i18n);
        }

        // Add JOSM home
        if (josmHome != null) {
            try {
                File home = josmHome.newFolder();
                System.setProperty("josm.home", home.getAbsolutePath());
            } catch (IOException e) {
                throw new InitializationError(e);
            }
        }

        // Add preferences
        if (usePreferences) {
            Main.initApplicationPreferences();
            Main.pref.enableSaveOnPut(false);
            // No pref init -> that would only create the preferences file.
            // We force the use of a wrong API server, just in case anyone attempts an upload
            Main.pref.put("osm-server.url", "http://invalid");
        }

        if (useProjection) {
            Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        }

        // Set API
        if (useAPI == APIType.DEV) {
            Main.pref.put("osm-server.url", "http://api06.dev.openstreetmap.org/api");
        } else if (useAPI == APIType.FAKE) {
            FakeOsmApi api = FakeOsmApi.getInstance();
            Main.pref.put("osm-server.url", api.getServerUrl());
        }

        // Initialize API
        if (useAPI != APIType.NONE) {
            try {
                OsmApi.getOsmApi().initialize(null);
            } catch (OsmTransferCanceledException | OsmApiInitializationException e) {
                throw new InitializationError(e);
            }
        }

        // Set Platform
        if (platform) {
            Main.determinePlatformHook();
        }
    }

    /**
     * Clean up what test not using these test rules may have broken.
     */
    @SuppressFBWarnings("DM_GC")
    private void cleanUpFromJosmFixture() {
        Main.getLayerManager().resetState();
        Main.pref = null;
        Main.platform = null;
        System.gc();
    }

    /**
     * Clean up after running a test
     */
    @SuppressFBWarnings("DM_GC")
    protected void after() {
        // Sync AWT Thread
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override
            public void run() {
            }
        });
        // Remove all layers
        Main.getLayerManager().resetState();

        // TODO: Remove global listeners and other global state.
        Main.pref = null;
        Main.platform = null;
        // Parts of JOSM uses weak references - destroy them.
        System.gc();
    }

    enum APIType {
        NONE, FAKE, DEV
    }
}
