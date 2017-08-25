// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.AboutAction;
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
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MainApplication} class.
 */
public class MainApplicationTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().https().devAPI().timeout(20000);

    /**
     * Make sure {@link MainApplication#contentPanePrivate} is initialized.
     */
    public static void initContentPane() {
        if (MainApplication.contentPanePrivate == null) {
            MainApplication.contentPanePrivate = new JPanel(new BorderLayout());
        }
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
    @SuppressWarnings("deprecation")
    public static void initMainPanel(boolean reAddListeners) {
        if (MainApplication.mainPanel == null) {
            MainApplication.mainPanel = new MainPanel(MainApplication.getLayerManager());
        }
        if (reAddListeners) {
            MainApplication.mainPanel.reAddListeners();
        }
        if (Main.main != null) {
            Main.main.panel = MainApplication.mainPanel;
        }
    }

    /**
     * Make sure {@link MainApplication#toolbar} is initialized.
     */
    @SuppressWarnings("deprecation")
    public static void initToolbar() {
        if (MainApplication.toolbar == null) {
            MainApplication.toolbar = new ToolbarPreferences();
        }
        if (Main.toolbar == null) {
            Main.toolbar = MainApplication.getToolbar();
        }
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
    public void testShowVersion() throws Exception {
        testShow("--version", Version.getInstance().getAgentString());
    }

    /**
     * Test of {@link MainApplication#main} with argument {@code --help}.
     * @throws Exception in case of error
     */
    @Test
    public void testShowHelp() throws Exception {
        testShow("--help", MainApplication.getHelp().trim());
    }

    /**
     * Unit test of {@link DownloadParamType#paramType} method.
     */
    @Test
    public void testParamType() {
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
    public void testUpdateAndLoadPlugins() throws PluginListParseException {
        final String old = System.getProperty("josm.plugins");
        try {
            System.setProperty("josm.plugins", "buildings_tools,plastic_laf");
            SplashProgressMonitor monitor = new SplashProgressMonitor("foo", e -> {
                // Do nothing
            });
            Collection<PluginInformation> plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
            if (plugins.isEmpty()) {
                PluginHandlerTestIT.downloadPlugins(Arrays.asList(
                        newPluginInformation("buildings_tools"),
                        newPluginInformation("plastic_laf")));
                plugins = MainApplication.updateAndLoadEarlyPlugins(null, monitor);
            }
            assertEquals(2, plugins.size());
            assertNotNull(PluginHandler.getPlugin("plastic_laf"));
            assertNull(PluginHandler.getPlugin("buildings_tools"));
            MainApplication.loadLatePlugins(null, monitor, plugins);
            assertNotNull(PluginHandler.getPlugin("buildings_tools"));
        } finally {
            if (old != null) {
                System.setProperty("josm.plugins", old);
            } else {
                System.clearProperty("josm.plugins");
            }
        }
    }

    /**
     * Unit test of {@link MainApplication#setupUIManager}.
     */
    @Test
    public void testSetupUIManager() {
        MainApplication.setupUIManager();
        assertEquals(Main.pref.get("laf", Main.platform.getDefaultStyle()), UIManager.getLookAndFeel().getClass().getCanonicalName());
    }

    private static PluginInformation newPluginInformation(String plugin) throws PluginListParseException {
        return PluginListParser.createInfo(plugin+".jar", "https://svn.openstreetmap.org/applications/editors/josm/dist/"+plugin+".jar",
                "");
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - empty case.
     */
    @Test
    public void testPostConstructorProcessCmdLineEmpty() {
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
    public void testPostConstructorProcessCmdLineBounds() {
        doTestPostConstructorProcessCmdLine(
                "0.01,0.01,0.05,0.05",
                "51.35,-0.4,51.60,0.2", true);
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with http/https URLs.
     * This test assumes the DEV API contains nodes around 0,0 and GPX tracks around London
     */
    @Test
    public void testPostConstructorProcessCmdLineHttpUrl() {
        doTestPostConstructorProcessCmdLine(
                "http://api06.dev.openstreetmap.org/api/0.6/map?bbox=0.01,0.01,0.05,0.05",
                "https://master.apis.dev.openstreetmap.org/api/0.6/trackpoints?bbox=-0.4,51.35,0.2,51.6&page=0", true);
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with file URLs.
     * @throws MalformedURLException if an error occurs
     */
    @Test
    public void testPostConstructorProcessCmdLineFileUrl() throws MalformedURLException {
        doTestPostConstructorProcessCmdLine(
                Paths.get(TestUtils.getTestDataRoot() + "multipolygon.osm").toUri().toURL().toExternalForm(),
                Paths.get(TestUtils.getTestDataRoot() + "minimal.gpx").toUri().toURL().toExternalForm(), false);
    }

    /**
     * Unit test of {@link MainApplication#postConstructorProcessCmdLine} - nominal case with file names.
     * @throws MalformedURLException if an error occurs
     */
    @Test
    public void testPostConstructorProcessCmdLineFilename() throws MalformedURLException {
        doTestPostConstructorProcessCmdLine(
                Paths.get(TestUtils.getTestDataRoot() + "multipolygon.osm").toFile().getAbsolutePath(),
                Paths.get(TestUtils.getTestDataRoot() + "minimal.gpx").toFile().getAbsolutePath(), false);
    }

    /**
     * Unit test of {@link MainApplication#getRegisteredActionShortcut}.
     */
    @Test
    public void testGetRegisteredActionShortcut() {
        Shortcut noKeystroke = Shortcut.registerShortcut("no", "keystroke", 0, 0);
        assertNull(noKeystroke.getKeyStroke());
        assertNull(MainApplication.getRegisteredActionShortcut(noKeystroke));
        Shortcut noAction = Shortcut.registerShortcut("foo", "bar", KeyEvent.VK_AMPERSAND, Shortcut.SHIFT);
        assertNotNull(noAction.getKeyStroke());
        assertNull(MainApplication.getRegisteredActionShortcut(noAction));
        AboutAction about = new AboutAction();
        assertEquals(about, MainApplication.getRegisteredActionShortcut(about.getShortcut()));
    }

    /**
     * Unit test of {@link MainApplication#addMapFrameListener} and {@link MainApplication#removeMapFrameListener}.
     */
    @Test
    public void testMapFrameListener() {
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
    public void testEnumDownloadParamType() {
        TestUtils.superficialEnumCodeCoverage(DownloadParamType.class);
    }
}
