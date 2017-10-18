// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.TimeZone;

import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MemoryManagerTest;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.date.DateUtils;

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
    private int timeout = isDebugMode() ? -1 : 10 * 1000;
    private TemporaryFolder josmHome;
    private boolean usePreferences = false;
    private APIType useAPI = APIType.NONE;
    private String i18n = null;
    private boolean platform;
    private boolean useProjection;
    private boolean useProjectionNadGrids;
    private boolean commands;
    private boolean allowMemoryManagerLeaks;
    private boolean useMapStyles;
    private boolean usePresets;
    private boolean useHttps;
    private boolean territories;
    private boolean rlTraffic;
    private boolean main;

    /**
     * Disable the default timeout for this test. Use with care.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules noTimeout() {
        timeout = -1;
        return this;
    }

    /**
     * Set a timeout for all tests in this class. Local method timeouts may only reduce this timeout.
     * @param millis The timeout duration in milliseconds.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules timeout(int millis) {
        timeout = isDebugMode() ? -1 : millis;
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

    /**
     * Set up loading of NTV2 grit shift files to support projections that need them.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules projectionNadGrids() {
        useProjectionNadGrids = true;
        return this;
    }

    /**
     * Set up HTTPS certificates
     * @return this instance, for easy chaining
     */
    public JOSMTestRules https() {
        useHttps = true;
        platform = true;
        return this;
    }

    /**
     * Allow the execution of commands using {@link Main#undoRedo}
     * @return this instance, for easy chaining
     */
    public JOSMTestRules commands() {
        commands = true;
        return this;
    }

    /**
     * Allow the memory manager to contain items after execution of the test cases.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules memoryManagerLeaks() {
        allowMemoryManagerLeaks = true;
        return this;
    }

    /**
     * Use map styles in this test.
     * @return this instance, for easy chaining
     * @since 11777
     */
    public JOSMTestRules mapStyles() {
        preferences();
        useMapStyles = true;
        return this;
    }

    /**
     * Use presets in this test.
     * @return this instance, for easy chaining
     * @since 12568
     */
    public JOSMTestRules presets() {
        preferences();
        usePresets = true;
        return this;
    }

    /**
     * Use boundaries dataset in this test.
     * @return this instance, for easy chaining
     * @since 12545
     */
    public JOSMTestRules territories() {
        territories = true;
        return this;
    }

    /**
     * Use right and lefthand traffic dataset in this test.
     * @return this instance, for easy chaining
     * @since 12556
     */
    public JOSMTestRules rlTraffic() {
        territories();
        rlTraffic = true;
        return this;
    }

    /**
     * Use the {@link Main#main}, {@code Main.contentPanePrivate}, {@code Main.mainPanel},
     *         {@link Main#menu}, {@link Main#toolbar} global variables in this test.
     * @return this instance, for easy chaining
     * @since 12557
     */
    public JOSMTestRules main() {
        platform();
        main = true;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Statement statement = base;
        if (timeout > 0) {
            // TODO: new DisableOnDebug(timeout)
            statement = new FailOnTimeoutStatement(statement, timeout);
        }
        statement = new CreateJosmEnvironment(statement);
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
        // Tests are running headless by default.
        System.setProperty("java.awt.headless", "true");

        cleanUpFromJosmFixture();

        Config.setPreferencesInstance(Main.pref);
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        // All tests use the same timezone.
        TimeZone.setDefault(DateUtils.UTC);
        // Set log level to info
        Logging.setLogLevel(Logging.LEVEL_INFO);
        // Assume anonymous user
        UserIdentityManager.getInstance().setAnonymous();
        User.clearUserMap();
        // Setup callbacks
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
        OsmConnection.setOAuthAccessTokenFetcher(OAuthAuthorizationWizard::obtainAccessToken);

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
            Main.pref.resetToInitialState();
            Main.pref.enableSaveOnPut(false);
            // No pref init -> that would only create the preferences file.
            // We force the use of a wrong API server, just in case anyone attempts an upload
            Config.getPref().put("osm-server.url", "http://invalid");
        }

        // Set Platform
        if (platform) {
            Main.determinePlatformHook();
        }

        if (useHttps) {
            try {
                CertificateAmendment.addMissingCertificates();
            } catch (IOException | GeneralSecurityException ex) {
                throw new JosmRuntimeException(ex);
            }
        }

        if (useProjection) {
            Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        }

        if (useProjectionNadGrids) {
            MainApplication.setupNadGridSources();
        }

        // Set API
        if (useAPI == APIType.DEV) {
            Config.getPref().put("osm-server.url", "http://api06.dev.openstreetmap.org/api");
        } else if (useAPI == APIType.FAKE) {
            FakeOsmApi api = FakeOsmApi.getInstance();
            Config.getPref().put("osm-server.url", api.getServerUrl());
        }

        // Initialize API
        if (useAPI != APIType.NONE) {
            try {
                OsmApi.getOsmApi().initialize(null);
            } catch (OsmTransferCanceledException | OsmApiInitializationException e) {
                throw new InitializationError(e);
            }
        }

        if (useMapStyles) {
            // Reset the map paint styles.
            MapPaintStyles.readFromPreferences();
        }

        if (usePresets) {
            // Reset the presets.
            TaggingPresets.readFromPreferences();
        }

        if (territories) {
            Territories.initialize();
        }

        if (rlTraffic) {
            RightAndLefthandTraffic.initialize();
        }

        if (commands) {
            // TODO: Implement a more selective version of this once Main is restructured.
            JOSMFixture.createUnitTestFixture().init(true);
        } else {
            if (main) {
                new MainApplication();
                JOSMFixture.initContentPane();
                JOSMFixture.initMainPanel(true);
                JOSMFixture.initToolbar();
                JOSMFixture.initMainMenu();
            }
        }
    }

    /**
     * Clean up what test not using these test rules may have broken.
     */
    @SuppressFBWarnings("DM_GC")
    private void cleanUpFromJosmFixture() {
        MemoryManagerTest.resetState(true);
        cleanLayerEnvironment();
        Main.pref.resetToInitialState();
        Main.platform = null;
        System.gc();
    }

    /**
     * Cleans the Layer manager and the SelectionEventManager.
     * You don't need to call this during tests, the test environment will do it for you.
     * @since 12070
     */
    public static void cleanLayerEnvironment() {
        // Get the instance before cleaning - this ensures that it is initialized.
        SelectionEventManager eventManager = SelectionEventManager.getInstance();
        MainApplication.getLayerManager().resetState();
        eventManager.resetState();
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
        cleanLayerEnvironment();
        MemoryManagerTest.resetState(allowMemoryManagerLeaks);

        // TODO: Remove global listeners and other global state.
        Main.pref.resetToInitialState();
        Main.platform = null;
        // Parts of JOSM uses weak references - destroy them.
        System.gc();
    }

    private final class CreateJosmEnvironment extends Statement {
        private final Statement base;

        private CreateJosmEnvironment(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            before();
            try {
                base.evaluate();
            } finally {
                after();
            }
        }
    }

    enum APIType {
        NONE, FAKE, DEV
    }

    /**
     * The junit timeout statement has problems when switchting timezones. This one does not.
     * @author Michael Zangl
     */
    private static class FailOnTimeoutStatement extends Statement {

        private int timeout;
        private Statement original;

        FailOnTimeoutStatement(Statement original, int timeout) {
            this.original = original;
            this.timeout = timeout;
        }

        @Override
        public void evaluate() throws Throwable {
            TimeoutThread thread = new TimeoutThread(original);
            thread.setDaemon(true);
            thread.start();
            thread.join(timeout);
            thread.interrupt();
            if (!thread.isDone) {
                Throwable exception = thread.getExecutionException();
                if (exception != null) {
                    throw exception;
                } else {
                    throw new Exception(MessageFormat.format("Test timed out after {0}ms", timeout));
                }
            }
        }
    }

    private static final class TimeoutThread extends Thread {
        public boolean isDone;
        private Statement original;
        private Throwable exceptionCaught;

        private TimeoutThread(Statement original) {
            super("Timeout runner");
            this.original = original;
        }

        public Throwable getExecutionException() {
            return exceptionCaught;
        }

        @Override
        public void run() {
            try {
                original.evaluate();
                isDone = true;
            } catch (Throwable e) {
                exceptionCaught = e;
            }
        }
    }

    private boolean isDebugMode() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
    }
}
