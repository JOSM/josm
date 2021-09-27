// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.jar.Attributes;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.SplashScreen.SplashProgressMonitor;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginHandlerTestIT;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginListParseException;
import org.openstreetmap.josm.plugins.PluginListParser;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.JavaProperty;
import org.openstreetmap.josm.testutils.annotations.JosmHome;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.StaticClassCleanup;
import org.openstreetmap.josm.testutils.annotations.Users;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MainApplication} class.
 */
@HTTPS
@Main
@OsmApiType(OsmApiType.APIType.DEV)
@Projection
@Timeout(20)
@StaticClassCleanup(HttpClient.class)
@Users
public class MainApplicationTest {

    /**
     * Make sure {@link MainApplication#contentPanePrivate} is initialized.
     */
    public static void initContentPane() {
        // init every time to avoid "Keystroke %s is already assigned to %s"
        MainApplication.contentPanePrivate = new JPanel(new BorderLayout());
    }

    /**
     * Returns {@link MainApplication#contentPanePrivate} (not public).
     * @return {@link MainApplication#contentPanePrivate}
     */
    public static JComponent getContentPane() {
        return MainApplication.contentPanePrivate;
    }

    /**
     * Make sure {@code MainApplication.mainPanel} is initialized.
     * @param reAddListeners {@code true} to re-add listeners
     */
    public static void initMainPanel(boolean reAddListeners) {
        if (MainApplication.mainPanel == null) {
            MainApplication.mainPanel = new MainPanel(MainApplication.getLayerManager());
        }
        if (reAddListeners) {
            MainApplication.mainPanel.reAddListeners();
        }
    }

    /**
     * Make sure {@code MainApplication.menu} is initialized.
     */
    public static void initMainMenu() {
        MainApplication.menu = new MainMenu();
    }

    /**
     * Make sure {@link MainApplication#toolbar} is initialized.
     */
    public static void initToolbar() {
        // init every time to avoid "Registered toolbar action"
        MainApplication.toolbar = new ToolbarPreferences();
    }

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING")
    private void testShow(final String arg, String expected) throws InterruptedException, IOException {
        PrintStream old = System.out;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(baos));
            Thread t = new Thread() {
                @Override
                public void run() {
                    MainApplication.main(new String[] {arg});
                }
            };
            t.start();
            t.join();
            System.out.flush();
            assertEquals(expected, baos.toString(StandardCharsets.UTF_8.name()).trim());
        } finally {
            System.setOut(old);
        }
    }

    /**
     * Test of {@link MainApplication#main} with argument {@code --version}.
     * @throws Exception in case of error
     */
    @Test
    void testShowVersion() throws Exception {
        testShow("--version", Version.getInstance().getAgentString());
    }

    /**
     * Test of {@link MainApplication#main} with argument {@code --help}.
     * @throws Exception in case of error
     */
    @Test
    void testShowHelp() throws Exception {
        testShow("--help", MainApplication.getHelp().trim());
    }

    /**
     * Unit test of {@link DownloadParamType#paramType} method.
     */
    @Test
    void testParamType() {
        assertEquals(DownloadParamType.bounds, DownloadParamType.paramType("48.000,16.000,48.001,16.001"));
        assertEquals(DownloadParamType.fileName, DownloadParamType.paramType("data.osm"));
        assertEquals(DownloadParamType.fileUrl, DownloadParamType.paramType("file:///home/foo/data.osm"));
        assertEquals(DownloadParamType.fileUrl, DownloadParamType.paramType("file://C:\\Users\\foo\\data.osm"));
        assertEquals(DownloadParamType.httpUrl, DownloadParamType.paramType("http://somewhere.com/data.osm"));
        assertEquals(DownloadParamType.httpUrl, DownloadParamType.paramType("https://somewhere.com/data.osm"));
    }

    /**
     * Test of {@link MainApplication#updateAndLoadEarlyPlugins} and {@link MainApplication#loadLatePlugins} methods.
     * @throws PluginListParseException if an error occurs
     */
    @Test
    @JavaProperty
    // Reuse downloads from plugin handler test, if it has already run. Otherwise, pre-cache two plugins.
    @JosmHome("test/config/unit/pluginHandlerTestIT-josm.home")
    @StaticClassCleanup(PluginHandler.class)
    @Timeout(60)
    void testUpdateAndLoadPlugins() throws PluginListParseException {
        System.setProperty("josm.plugins", "buildings_tools,log4j");
        SplashProgressMonitor monitor = new SplashProgressMonitor("foo", e -> {
            // Do nothing
        });
        Collection<PluginInformation> plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
        if (plugins.isEmpty()) {
            PluginHandlerTestIT.downloadPlugins(Arrays.asList(
                    newPluginInformation("buildings_tools"),
                    newPluginInformation("log4j")));
            plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
        }
        assertEquals(2, plugins.size());
        assertNotNull(PluginHandler.getPlugin("log4j"));
        assertNull(PluginHandler.getPlugin("buildings_tools"));
        MainApplication.loadLatePlugins(null, monitor, plugins);
        assertNotNull(PluginHandler.getPlugin("buildings_tools"));
    }

    /**
     * Unit test of {@link MainApplication#setupUIManager}.
     */
    @Test
    void testSetupUIManager() {
        assumeFalse(PlatformManager.isPlatformWindows() && "True".equals(System.getenv("APPVEYOR")));
        MainApplication.setupUIManager();
        assertEquals(Config.getPref().get("laf", PlatformManager.getPlatform().getDefaultStyle()),
                UIManager.getLookAndFeel().getClass().getCanonicalName());
    }

    private static PluginInformation newPluginInformation(String plugin) throws PluginListParseException {
        return PluginListParser.createInfo(plugin+".jar", "https://josm.openstreetmap.de/osmsvn/applications/editors/josm/dist/"+plugin+".jar",
                new Attributes());
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - empty case.
     */
    @Test
    void testPostConstructorProcessCmdLineEmpty() {
        // Check the method accepts no arguments
        MainApplication.postConstructorProcessCmdLine(new ProgramArguments(new String[0]));
    }

    private static void doTestPostConstructorProcessCmdLine(String download, String downloadGps, boolean gpx) {
        assertNull(MainApplication.getLayerManager().getEditDataSet());
        for (Future<?> f : MainApplication.postConstructorProcessCmdLine(new ProgramArguments(new String[]{
                "--download=" + download,
                "--downloadgps=" + downloadGps,
                "--selection=type: node"}))) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                Logging.error(e);
            }
        }
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        assertNotNull(ds);
        assertFalse(ds.getSelected().isEmpty());
        MainApplication.getLayerManager().removeLayer(MainApplication.getLayerManager().getEditLayer());
        if (gpx) {
            List<GpxLayer> gpxLayers = MainApplication.getLayerManager().getLayersOfType(GpxLayer.class);
            assertEquals(1, gpxLayers.size());
            MainApplication.getLayerManager().removeLayer(gpxLayers.iterator().next());
        }
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with bounds.
     * This test assumes the DEV API contains nodes around 0,0 and GPX tracks around London
     */
    @Test
    void testPostConstructorProcessCmdLineBounds() {
        doTestPostConstructorProcessCmdLine(
                "-47.20,-126.75,-47.10,-126.65",
                "51.35,-0.4,51.60,0.2", true);
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with http/https URLs.
     * This test assumes the DEV API contains nodes around -47.15, -126.7 (R'lyeh) and GPX tracks around London
     */
    @Test
    void testPostConstructorProcessCmdLineHttpUrl() {
        doTestPostConstructorProcessCmdLine(
                "https://api06.dev.openstreetmap.org/api/0.6/map?bbox=-126.75,-47.20,-126.65,-47.10",
                "https://master.apis.dev.openstreetmap.org/api/0.6/trackpoints?bbox=-0.4,51.35,0.2,51.6&page=0", true);
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with file URLs.
     * @throws MalformedURLException if an error occurs
     */
    @Test
    void testPostConstructorProcessCmdLineFileUrl() throws MalformedURLException {
        doTestPostConstructorProcessCmdLine(
                Paths.get(TestUtils.getTestDataRoot() + "multipolygon.osm").toUri().toURL().toExternalForm(),
                Paths.get(TestUtils.getTestDataRoot() + "minimal.gpx").toUri().toURL().toExternalForm(), false);
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with file names.
     * @throws MalformedURLException if an error occurs
     */
    @Test
    void testPostConstructorProcessCmdLineFilename() throws MalformedURLException {
        doTestPostConstructorProcessCmdLine(
                Paths.get(TestUtils.getTestDataRoot() + "multipolygon.osm").toFile().getAbsolutePath(),
                Paths.get(TestUtils.getTestDataRoot() + "minimal.gpx").toFile().getAbsolutePath(), false);
    }

    /**
     * Unit test of {@link MainApplication#getRegisteredActionShortcut}.
     */
    @Test
    void testGetRegisteredActionShortcut() {
        Shortcut noKeystroke = Shortcut.registerShortcut("no", "keystroke", 0, 0);
        assertNull(noKeystroke.getKeyStroke());
        assertNull(MainApplication.getRegisteredActionShortcut(noKeystroke));
        Shortcut noAction = Shortcut.registerShortcut("foo", "bar", KeyEvent.VK_AMPERSAND, Shortcut.SHIFT);
        assertNotNull(noAction.getKeyStroke());
        assertNull(MainApplication.getRegisteredActionShortcut(noAction));
        JosmAction action = new OpenLocationAction();
        assertEquals(action, MainApplication.getRegisteredActionShortcut(action.getShortcut()));
    }

    /**
     * Unit test of {@link MainApplication#addMapFrameListener} and {@link MainApplication#removeMapFrameListener}.
     */
    @Test
    void testMapFrameListener() {
        MapFrameListener listener = (o, n) -> { };
        assertTrue(MainApplication.addMapFrameListener(listener));
        assertFalse(MainApplication.addMapFrameListener(null));
        assertTrue(MainApplication.removeMapFrameListener(listener));
        assertFalse(MainApplication.removeMapFrameListener(null));
    }

    /**
     * Unit test of {@link DownloadParamType} enum.
     */
    @Test
    void testEnumDownloadParamType() {
        TestUtils.superficialEnumCodeCoverage(DownloadParamType.class);
    }
}
