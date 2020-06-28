// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Handler;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreferenceTestIT;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CertificateAmendment;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmConnection;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.testutils.mockers.EDTAssertionMocker;
import org.openstreetmap.josm.testutils.mockers.WindowlessMapViewStateMocker;
import org.openstreetmap.josm.testutils.mockers.WindowlessNavigatableComponentMocker;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MemoryManagerTest;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.bugreport.ReportedException;
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
public class JOSMTestRules implements TestRule, AfterEachCallback, BeforeEachCallback, AfterAllCallback, BeforeAllCallback {
    private int timeout = isDebugMode() ? -1 : 10 * 1000;
    private TemporaryFolder josmHome;
    private boolean usePreferences = false;
    private APIType useAPI = APIType.NONE;
    private String i18n = null;
    private TileSourceRule tileSourceRule;
    private String assumeRevisionString;
    private Version originalVersion;
    private Runnable mapViewStateMockingRunnable;
    private Runnable navigableComponentMockingRunnable;
    private Runnable edtAssertionMockingRunnable;
    private boolean useProjection;
    private boolean useProjectionNadGrids;
    private boolean commands;
    private boolean allowMemoryManagerLeaks;
    private boolean useMapStyles;
    private boolean usePresets;
    private boolean useHttps;
    private boolean territories;
    private boolean metric;
    private boolean main;
    /**
     * This boolean is only used to indicate if JUnit5 is used in a test. If it is,
     * we must not call after in {@link JOSMTestRules.CreateJosmEnvironment#evaluate}.
     * TODO: Remove JUnit4 as a whole sometime after 2021-01-01 (~6 month lead time for plugins)
     */
    private boolean junit5;

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
     * Mock this test's assumed JOSM version (as reported by {@link Version}).
     * @param revisionProperties mock contents of JOSM's {@code REVISION} properties file
     * @return this instance, for easy chaining
     */
    public JOSMTestRules assumeRevision(final String revisionProperties) {
        this.assumeRevisionString = revisionProperties;
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
        return this;
    }

    /**
     * Allow the execution of commands using {@code UndoRedoHandler}
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
     * @deprecated Use {@link #territories}
     */
    @Deprecated
    public JOSMTestRules rlTraffic() {
        territories();
        return this;
    }

    /**
     * Force metric measurement system.
     * @return this instance, for easy chaining
     * @since 15400
     */
    public JOSMTestRules metricSystem() {
        metric = true;
        return this;
    }

    /**
     * Re-raise AssertionErrors thrown in the EDT where they would have normally been swallowed.
     * @return this instance, for easy chaining
     */
    public JOSMTestRules assertionsInEDT() {
        return this.assertionsInEDT(EDTAssertionMocker::new);
    }

    /**
     * Re-raise AssertionErrors thrown in the EDT where they would have normally been swallowed.
     * @param edtAssertionMockingRunnable Runnable for initializing this functionality
     *
     * @return this instance, for easy chaining
     */
    public JOSMTestRules assertionsInEDT(final Runnable edtAssertionMockingRunnable) {
        this.edtAssertionMockingRunnable = edtAssertionMockingRunnable;
        return this;
    }

    /**
     * Replace imagery sources with a default set of mock tile sources
     *
     * @return this instance, for easy chaining
     */
    public JOSMTestRules fakeImagery() {
        return this.fakeImagery(
            new TileSourceRule(
                true,
                true,
                true,
                new TileSourceRule.ColorSource(Color.WHITE, "White Tiles", 256),
                new TileSourceRule.ColorSource(Color.BLACK, "Black Tiles", 256),
                new TileSourceRule.ColorSource(Color.MAGENTA, "Magenta Tiles", 256),
                new TileSourceRule.ColorSource(Color.GREEN, "Green Tiles", 256)
            )
        );
    }

    /**
     * Replace imagery sources with those from specific mock tile server setup
     * @param tileSourceRule Tile source rule
     *
     * @return this instance, for easy chaining
     */
    public JOSMTestRules fakeImagery(TileSourceRule tileSourceRule) {
        this.preferences();
        this.tileSourceRule = tileSourceRule;
        return this;
    }

    /**
     * Use the {@code Main#main}, {@code Main.contentPanePrivate}, {@code Main.mainPanel},
     *         global variables in this test.
     * @return this instance, for easy chaining
     * @since 12557
     */
    public JOSMTestRules main() {
        return this.main(
            WindowlessMapViewStateMocker::new,
            WindowlessNavigatableComponentMocker::new
        );
    }

    /**
     * Use the {@code Main#main}, {@code Main.contentPanePrivate}, {@code Main.mainPanel},
     *         global variables in this test.
     * @param mapViewStateMockingRunnable Runnable to use for mocking out any required parts of
     *        {@link org.openstreetmap.josm.gui.MapViewState}, null to skip.
     * @param navigableComponentMockingRunnable Runnable to use for mocking out any required parts
     *        of {@link org.openstreetmap.josm.gui.NavigatableComponent}, null to skip.
     *
     * @return this instance, for easy chaining
     */
    public JOSMTestRules main(
        final Runnable mapViewStateMockingRunnable,
        final Runnable navigableComponentMockingRunnable
    ) {
        this.main = true;
        this.mapViewStateMockingRunnable = mapViewStateMockingRunnable;
        this.navigableComponentMockingRunnable = navigableComponentMockingRunnable;
        return this;
    }

    /**
     * Must be called if test run with Junit parameters
     * @return this instance, for easy chaining
     */
    public JOSMTestRules parameters() {
        try {
            apply(new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    // Do nothing. Hack needed because @Parameters are computed before anything else
                }
            }, Description.createSuiteDescription(ImageryPreferenceTestIT.class)).evaluate();
        } catch (Throwable e) {
            Logging.error(e);
        }
        return this;
    }

    private static class MockVersion extends Version {
        MockVersion(final String propertiesString) {
            super.initFromRevisionInfo(
                new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8))
            );
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        // First process any Override* annotations for per-test overrides.
        // The following only work because "option" methods modify JOSMTestRules in-place
        final OverrideAssumeRevision overrideAssumeRevision = description.getAnnotation(OverrideAssumeRevision.class);
        if (overrideAssumeRevision != null) {
            this.assumeRevision(overrideAssumeRevision.value());
        }
        final OverrideTimeout overrideTimeout = description.getAnnotation(OverrideTimeout.class);
        if (overrideTimeout != null) {
            this.timeout(overrideTimeout.value());
        }
        Statement statement = base;
        // counter-intuitively, Statements which need to have their setup routines performed *after* another one need to
        // be added into the chain *before* that one, so that it ends up on the "inside".
        if (timeout > 0) {
            // TODO: new DisableOnDebug(timeout)
            statement = new FailOnTimeoutStatement(statement, timeout);
        }

        // this half of TileSourceRule's initialization must happen after josm is set up
        if (this.tileSourceRule != null) {
            statement = this.tileSourceRule.applyRegisterLayers(statement, description);
        }

        statement = new CreateJosmEnvironment(statement);
        if (josmHome != null) {
            statement = josmHome.apply(statement, description);
        }

        // run mock tile server as the outermost Statement (started first) so it can hopefully be initializing in
        // parallel with other setup
        if (this.tileSourceRule != null) {
            statement = this.tileSourceRule.applyRunServer(statement, description);
        }
        return statement;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        this.junit5 = true;
        Statement temporaryStatement = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // do nothing
            }
        };
        try {
            this.apply(temporaryStatement,
                    Description.createTestDescription(this.getClass(), "JOSMTestRules JUnit5 Compatibility"))
                    .evaluate();
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        after();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        beforeEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        afterEach(context);
    }

    /**
     * Set up before running a test
     * @throws InitializationError If an error occurred while creating the required environment.
     * @throws ReflectiveOperationException if a reflective access error occurs
     */
    protected void before() throws InitializationError, ReflectiveOperationException {
        cleanUpFromJosmFixture();

        if (this.assumeRevisionString != null) {
            this.originalVersion = Version.getInstance();
            final Version replacementVersion = new MockVersion(this.assumeRevisionString);
            TestUtils.setPrivateStaticField(Version.class, "instance", replacementVersion);
        }

        // Add JOSM home
        if (josmHome != null) {
            try {
                File home = josmHome.newFolder();
                System.setProperty("josm.home", home.getAbsolutePath());
                JosmBaseDirectories.getInstance().clearMemos();
            } catch (IOException e) {
                throw new InitializationError(e);
            }
        }

        Preferences pref = Preferences.main();
        Config.setPreferencesInstance(pref);
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        Config.setUrlsProvider(JosmUrls.getInstance());
        // All tests use the same timezone.
        TimeZone.setDefault(DateUtils.UTC);

        // Force log handlers to reacquire reference to (junit's fake) stdout/stderr
        for (Handler handler : Logging.getLogger().getHandlers()) {
            if (handler instanceof Logging.ReacquiringConsoleHandler) {
                handler.flush();
                ((Logging.ReacquiringConsoleHandler) handler).reacquireOutputStream();
            }
        }
        // Set log level to info
        Logging.setLogLevel(Logging.LEVEL_INFO);

        // Assume anonymous user
        UserIdentityManager.getInstance().setAnonymous();
        User.clearUserMap();
        // Setup callbacks
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
        OsmConnection.setOAuthAccessTokenFetcher(OAuthAuthorizationWizard::obtainAccessToken);
        HttpClient.setFactory(Http1Client::new);

        // Set up i18n
        if (i18n != null) {
            I18n.set(i18n);
        }

        // Add preferences
        if (usePreferences) {
            @SuppressWarnings("unchecked")
            final Map<String, Setting<?>> defaultsMap = (Map<String, Setting<?>>) TestUtils.getPrivateField(pref, "defaultsMap");
            defaultsMap.clear();
            pref.resetToInitialState();
            pref.enableSaveOnPut(false);
            // No pref init -> that would only create the preferences file.
            // We force the use of a wrong API server, just in case anyone attempts an upload
            Config.getPref().put("osm-server.url", "http://invalid");
        }

        // Make sure we're using the metric system
        if (metric) {
            SystemOfMeasurement.setSystemOfMeasurement(SystemOfMeasurement.METRIC.getName());
        }

        if (useHttps) {
            try {
                CertificateAmendment.addMissingCertificates();
            } catch (IOException | GeneralSecurityException ex) {
                throw new JosmRuntimeException(ex);
            }
        }

        if (useProjection) {
            ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        }

        if (useProjectionNadGrids) {
            MainApplication.setupNadGridSources();
        }

        // Set API
        if (useAPI == APIType.DEV) {
            Config.getPref().put("osm-server.url", "https://api06.dev.openstreetmap.org/api");
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
            Territories.initializeInternalData();
        }

        if (this.edtAssertionMockingRunnable != null) {
            this.edtAssertionMockingRunnable.run();
        }

        if (commands) {
            // TODO: Implement a more selective version of this once Main is restructured.
            JOSMFixture.createUnitTestFixture().init(true);
        } else {
            if (main) {
                // apply mockers to MapViewState and NavigableComponent whether we're headless or not
                // as we generally don't create the josm main window even in non-headless mode.
                if (this.mapViewStateMockingRunnable != null) {
                    this.mapViewStateMockingRunnable.run();
                }
                if (this.navigableComponentMockingRunnable != null) {
                    this.navigableComponentMockingRunnable.run();
                }

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
        Preferences.main().resetToInitialState();
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
     * @return TileSourceRule which is automatically started by this rule
     */
    public TileSourceRule getTileSourceRule() {
        return this.tileSourceRule;
    }

    /**
     * Clean up after running a test
     * @throws ReflectiveOperationException if a reflective access error occurs
     */
    @SuppressFBWarnings("DM_GC")
    protected void after() throws ReflectiveOperationException {
        // Sync AWT Thread
        GuiHelper.runInEDTAndWait(() -> { });
        // Sync worker thread
        final boolean[] queueEmpty = {false};
        MainApplication.worker.submit(() -> queueEmpty[0] = true);
        Awaitility.await().forever().until(() -> queueEmpty[0]);
        // Remove all layers
        cleanLayerEnvironment();
        MemoryManagerTest.resetState(allowMemoryManagerLeaks);

        // TODO: Remove global listeners and other global state.
        ProjectionRegistry.clearProjectionChangeListeners();
        Preferences.main().resetToInitialState();

        if (this.assumeRevisionString != null && this.originalVersion != null) {
            TestUtils.setPrivateStaticField(Version.class, "instance", this.originalVersion);
        }

        Window[] windows = Window.getWindows();
        if (windows.length != 0) {
            Logging.info(
                "Attempting to close {0} windows left open by tests: {1}",
                windows.length,
                Arrays.toString(windows)
            );
        }
        GuiHelper.runInEDTAndWait(() -> {
            for (Window window : windows) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                window.dispose();
            }
        });

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
                if (!junit5) {
                    after();
                }
            }
        }
    }

    enum APIType {
        NONE, FAKE, DEV
    }

    /**
     * The junit timeout statement has problems when switching timezones. This one does not.
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
                    if (Logging.isLoggingEnabled(Logging.LEVEL_DEBUG)) {
                        // i.e. skip expensive formatting of stack trace if it won't be shown
                        final StringWriter sw = new StringWriter();
                        new ReportedException(exception).printReportThreadsTo(new PrintWriter(sw));
                        Logging.debug("Thread state at timeout: {0}", sw);
                    }
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

    /**
     * Override this test's assumed JOSM version (as reported by {@link Version}).
     * @see JOSMTestRules#assumeRevision(String)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OverrideAssumeRevision {
        /**
         * Returns overridden assumed JOSM version.
         * @return overridden assumed JOSM version
         */
        String value();
    }

    /**
     * Override this test's timeout.
     * @see JOSMTestRules#timeout(int)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OverrideTimeout {
        /**
         * Returns overridden timeout value.
         * @return overridden timeout value
         */
        int value();
    }
}
