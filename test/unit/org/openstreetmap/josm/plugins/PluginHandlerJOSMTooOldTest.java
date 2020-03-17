// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.PluginServer;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test parts of {@link PluginHandler} class when the reported JOSM version is too old for the plugin.
 */
public class PluginHandlerJOSMTooOldTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().main().assumeRevision(
        "Revision: 6000\n"
    );

    /**
     * Plugin server mock.
     */
    @Rule
    public WireMockRule pluginServerRule = new WireMockRule(
        options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot())
    );

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        Config.getPref().putInt("pluginmanager.version", 999);
        Config.getPref().put("pluginmanager.lastupdate", "999");
        Config.getPref().putList("pluginmanager.sites",
            ImmutableList.of(String.format("http://localhost:%s/plugins", this.pluginServerRule.port()))
        );

        this.referenceDummyJarOld = new File(TestUtils.getTestDataRoot(), "__files/plugin/dummy_plugin.v31701.jar");
        this.referenceDummyJarNew = new File(TestUtils.getTestDataRoot(), "__files/plugin/dummy_plugin.v31772.jar");
        this.referenceBazJarOld = new File(TestUtils.getTestDataRoot(), "__files/plugin/baz_plugin.v6.jar");
        this.referenceBazJarNew = new File(TestUtils.getTestDataRoot(), "__files/plugin/baz_plugin.v7.jar");
        this.referenceQuxJarOld = new File(TestUtils.getTestDataRoot(), "__files/" + referencePathQuxJarOld);
        this.referenceQuxJarNewer = new File(TestUtils.getTestDataRoot(), "__files/" + referencePathQuxJarNewer);
        this.pluginDir = Preferences.main().getPluginsDirectory();
        this.targetDummyJar = new File(this.pluginDir, "dummy_plugin.jar");
        this.targetDummyJarNew = new File(this.pluginDir, "dummy_plugin.jar.new");
        this.targetBazJar = new File(this.pluginDir, "baz_plugin.jar");
        this.targetBazJarNew = new File(this.pluginDir, "baz_plugin.jar.new");
        this.targetQuxJar = new File(this.pluginDir, "qux_plugin.jar");
        this.targetQuxJarNew = new File(this.pluginDir, "qux_plugin.jar.new");
        this.pluginDir.mkdirs();
    }

    private static final String referencePathQuxJarOld = "plugin/qux_plugin.v345.jar";
    private static final String referencePathQuxJarNewer = "plugin/qux_plugin.v432.jar";

    private File pluginDir;
    private File referenceDummyJarOld;
    private File referenceDummyJarNew;
    private File referenceBazJarOld;
    private File referenceBazJarNew;
    private File referenceQuxJarOld;
    private File referenceQuxJarNewer;
    private File targetDummyJar;
    private File targetDummyJarNew;
    private File targetBazJar;
    private File targetBazJarNew;
    private File targetQuxJar;
    private File targetQuxJarNew;

    private final String bazPluginVersionReqString = "JOSM version 8,001 required for plugin baz_plugin.";
    private final String dummyPluginVersionReqString = "JOSM version 7,001 required for plugin dummy_plugin.";
    private final String dummyPluginFailedString = "<html>Updating the following plugin has failed:<ul><li>dummy_plugin</li></ul>"
        + "Please open the Preference Dialog after JOSM has started and try to update it manually.</html>";

    /**
     * test update of plugins when those plugins turn out to require a higher JOSM version, but the
     * user chooses to update them anyway.
     * @throws IOException never
     */
    @Test
    public void testUpdatePluginsDownloadBoth() throws IOException {
        TestUtils.assumeWorkingJMockit();
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew)
        );
        pluginServer.applyToWireMockServer(this.pluginServerRule);
        Config.getPref().putList("plugins", ImmutableList.of("dummy_plugin", "baz_plugin"));

        final ExtendedDialogMocker edMocker = new ExtendedDialogMocker();
        edMocker.getMockResultMap().put(this.bazPluginVersionReqString, "Download Plugin");
        edMocker.getMockResultMap().put(this.dummyPluginVersionReqString, "Download Plugin");

        Files.copy(this.referenceDummyJarOld.toPath(), this.targetDummyJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            MainApplication.getMainFrame(),
            null,
            null,
            false
        ).stream().sorted((a, b) -> a.name.compareTo(b.name)).collect(ImmutableList.toImmutableList());

        assertEquals(
            ImmutableList.of(
                this.dummyPluginVersionReqString,
                this.bazPluginVersionReqString
            ),
            edMocker.getInvocationLog().stream().map(
                invocationEntry -> invocationEntry[1]
            ).sorted().collect(ImmutableList.toImmutableList())
        );

        assertEquals(2, updatedPlugins.size());

        assertEquals(updatedPlugins.get(0).name, "baz_plugin");
        assertEquals("7", updatedPlugins.get(0).localversion);

        assertEquals(updatedPlugins.get(1).name, "dummy_plugin");
        assertEquals("31772", updatedPlugins.get(1).localversion);

        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        TestUtils.assertFileContentsEqual(this.referenceDummyJarNew, this.targetDummyJar);
        TestUtils.assertFileContentsEqual(this.referenceBazJarNew, this.targetBazJar);

        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v7.jar")));

        assertEquals(Config.getPref().getInt("pluginmanager.version", 111), 6000);
        // not mocking the time so just check it's not its original value
        assertNotEquals(Config.getPref().get("pluginmanager.lastupdate", "999"), "999");
    }

    /**
     * test update of plugins when those plugins turn out to require a higher JOSM version, but the
     * user chooses to update one and skip the other.
     * @throws IOException never
     */
    @Test
    public void testUpdatePluginsSkipOne() throws IOException {
        TestUtils.assumeWorkingJMockit();
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew)
        );
        pluginServer.applyToWireMockServer(this.pluginServerRule);
        Config.getPref().putList("plugins", ImmutableList.of("dummy_plugin", "baz_plugin"));

        final ExtendedDialogMocker edMocker = new ExtendedDialogMocker();
        edMocker.getMockResultMap().put(this.bazPluginVersionReqString, "Download Plugin");
        edMocker.getMockResultMap().put(this.dummyPluginVersionReqString, "Skip Download");
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker();
        haMocker.getMockResultMap().put(this.dummyPluginFailedString, "OK");

        Files.copy(this.referenceDummyJarOld.toPath(), this.targetDummyJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            MainApplication.getMainFrame(),
            null,
            null,
            false
        ).stream().sorted((a, b) -> a.name.compareTo(b.name)).collect(ImmutableList.toImmutableList());

        assertEquals(
            ImmutableList.of(
                this.dummyPluginVersionReqString,
                this.bazPluginVersionReqString
            ),
            edMocker.getInvocationLog().stream().map(
                invocationEntry -> invocationEntry[1]
            ).sorted().collect(ImmutableList.toImmutableList())
        );

        assertEquals(
            ImmutableList.of(
                this.dummyPluginFailedString
            ),
            haMocker.getInvocationLog().stream().map(
                invocationEntry -> invocationEntry[1]
            ).sorted().collect(ImmutableList.toImmutableList())
        );

        assertEquals(2, updatedPlugins.size());

        assertEquals(updatedPlugins.get(0).name, "baz_plugin");
        assertEquals("7", updatedPlugins.get(0).localversion);

        assertEquals(updatedPlugins.get(1).name, "dummy_plugin");
        assertEquals("31701", updatedPlugins.get(1).localversion);

        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        TestUtils.assertFileContentsEqual(this.referenceDummyJarOld, this.targetDummyJar);
        TestUtils.assertFileContentsEqual(this.referenceBazJarNew, this.targetBazJar);

        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        this.pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v7.jar")));

        // shouldn't have been updated
        assertEquals(Config.getPref().getInt("pluginmanager.version", 111), 999);
        assertEquals(Config.getPref().get("pluginmanager.lastupdate", "999"), "999");
    }

    /**
     * When the plugin list suggests that the jar file at the provided URL *doesn't* require a newer JOSM
     * but in fact the plugin served *does*, it is installed anyway.
     *
     * This is probably NOT desirable and should be fixed, however this test documents the behaviour.
     * @throws IOException never
     */
    @Test
    public void testUpdatePluginsUnexpectedlyJOSMTooOld() throws IOException {
        TestUtils.assumeWorkingJMockit();
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew, Collections.singletonMap("Plugin-Mainversion", "5500"))
        );
        pluginServer.applyToWireMockServer(this.pluginServerRule);
        Config.getPref().putList("plugins", ImmutableList.of("baz_plugin"));

        // setting up blank ExtendedDialogMocker which would raise an exception if any attempt to show
        // and ExtendedDialog were made
        new ExtendedDialogMocker();

        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final List<PluginInformation> updatedPlugins = ImmutableList.copyOf(PluginHandler.updatePlugins(
            MainApplication.getMainFrame(),
            null,
            null,
            false
        ));

        // questionably correct
        assertEquals(1, updatedPlugins.size());

        // questionably correct
        assertEquals(updatedPlugins.get(0).name, "baz_plugin");
        assertEquals("7", updatedPlugins.get(0).localversion);

        assertFalse(targetBazJarNew.exists());

        // questionably correct
        TestUtils.assertFileContentsEqual(this.referenceBazJarNew, this.targetBazJar);

        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        // questionably correct
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v7.jar")));

        // should have been updated
        assertEquals(Config.getPref().getInt("pluginmanager.version", 111), 6000);
        assertNotEquals(Config.getPref().get("pluginmanager.lastupdate", "999"), "999");
    }

    /**
     * When a plugin advertises several versions for compatibility with older JOSMs, but even the
     * oldest of those is newer than our JOSM version, the user is prompted to upgrade to the newest
     * version anyway.
     *
     * While this behaviour is not incorrect, it's probably less helpful than it could be - the
     * version that's most likely to work best in this case will be the "oldest" still-available
     * version, however this test documents the behaviour.
     * @throws IOException never
     */
    @Test
    @JOSMTestRules.OverrideAssumeRevision("Revision: 7200\n")
    public void testUpdatePluginsMultiVersionInsufficient() throws IOException {
        TestUtils.assumeWorkingJMockit();

        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(this.referenceQuxJarNewer, Collections.singletonMap(
                "7499_Plugin-Url", "346;http://localhost:" + this.pluginServerRule.port() + "/dont/bother.jar"
            ))
        );
        pluginServer.applyToWireMockServer(this.pluginServerRule);
        Config.getPref().putList("plugins", ImmutableList.of("qux_plugin", "baz_plugin"));

        new ExtendedDialogMocker(Collections.singletonMap("JOSM version 7,500 required for plugin qux_plugin.", "Download Plugin"));

        Files.copy(this.referenceQuxJarOld.toPath(), this.targetQuxJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            MainApplication.getMainFrame(),
            null,
            null,
            false
        ).stream().sorted((a, b) -> a.name.compareTo(b.name)).collect(ImmutableList.toImmutableList());

        assertEquals(2, updatedPlugins.size());

        assertEquals("baz_plugin", updatedPlugins.get(0).name);
        assertEquals("6", updatedPlugins.get(0).localversion);

        assertEquals("qux_plugin", updatedPlugins.get(1).name);
        // questionably correct
        assertEquals("432", updatedPlugins.get(1).localversion);

        assertFalse(targetQuxJarNew.exists());

        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);
        // questionably correct
        TestUtils.assertFileContentsEqual(this.referenceQuxJarNewer, this.targetQuxJar);

        assertEquals(2, WireMock.getAllServeEvents().size());
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        // questionably correct
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/qux_plugin.v432.jar")));

        assertEquals(7200, Config.getPref().getInt("pluginmanager.version", 111));
        // not mocking the time so just check it's not its original value
        assertNotEquals("999", Config.getPref().get("pluginmanager.lastupdate", "999"));
    }
}
