// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.nio.file.Files;
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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test parts of {@link PluginHandler} class with plugins that advertise multiple versions for compatibility.
 */
public class PluginHandlerMultiVersionTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().main();

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

        this.referenceBazJarOld = new File(TestUtils.getTestDataRoot(), "__files/plugin/baz_plugin.v6.jar");
        this.referenceQuxJarOld = new File(TestUtils.getTestDataRoot(), "__files/" + referencePathQuxJarOld);
        this.referenceQuxJarNewer = new File(TestUtils.getTestDataRoot(), "__files/" + referencePathQuxJarNewer);
        this.referenceQuxJarNewest = new File(TestUtils.getTestDataRoot(), "__files/" + referencePathQuxJarNewest);
        this.pluginDir = Preferences.main().getPluginsDirectory();
        this.targetBazJar = new File(this.pluginDir, "baz_plugin.jar");
        this.targetBazJarNew = new File(this.pluginDir, "baz_plugin.jar.new");
        this.targetQuxJar = new File(this.pluginDir, "qux_plugin.jar");
        this.targetQuxJarNew = new File(this.pluginDir, "qux_plugin.jar.new");
        this.pluginDir.mkdirs();
    }

    private static final String referencePathQuxJarOld = "plugin/qux_plugin.v345.jar";
    private static final String referencePathQuxJarNewer = "plugin/qux_plugin.v432.jar";
    private static final String referencePathQuxJarNewest = "plugin/qux_plugin.v435.jar";

    private File pluginDir;
    private File referenceBazJarOld;
    private File referenceQuxJarOld;
    private File referenceQuxJarNewer;
    private File referenceQuxJarNewest;
    private File targetBazJar;
    private File targetBazJarNew;
    private File targetQuxJar;
    private File targetQuxJarNew;

    /**
     * test update of plugins when our current JOSM version prevents us from using the latest version,
     * but an additional version is listed which *does* support our version
     * @throws Exception on failure
     */
    @JOSMTestRules.OverrideAssumeRevision("Revision: 7501\n")
    @Test
    public void testUpdatePluginsOneMultiVersion() throws Exception {
        TestUtils.assumeWorkingJMockit();

        final String quxNewerServePath = "/qux/newer.jar";
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(this.referenceQuxJarNewest, ImmutableMap.of(
                "7500_Plugin-Url", "432;http://localhost:" + this.pluginServerRule.port() + quxNewerServePath,
                "7499_Plugin-Url", "346;http://localhost:" + this.pluginServerRule.port() + "/not/served.jar",
                "6999_Plugin-Url", "345;http://localhost:" + this.pluginServerRule.port() + "/not/served/either.jar"
            ))
        );
        pluginServer.applyToWireMockServer(this.pluginServerRule);
        // need to actually serve this older jar from somewhere
        this.pluginServerRule.stubFor(
            WireMock.get(WireMock.urlEqualTo(quxNewerServePath)).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/java-archive").withBodyFile(
                    referencePathQuxJarNewer
                )
            )
        );
        Config.getPref().putList("plugins", ImmutableList.of("qux_plugin", "baz_plugin"));

        // catch any (unexpected) attempts to show us an ExtendedDialog
        new ExtendedDialogMocker();

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
        assertEquals("432", updatedPlugins.get(1).localversion);

        assertFalse(targetBazJarNew.exists());
        assertFalse(targetQuxJarNew.exists());

        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);
        TestUtils.assertFileContentsEqual(this.referenceQuxJarNewer, this.targetQuxJar);

        assertEquals(2, WireMock.getAllServeEvents().size());
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(quxNewerServePath)));

        assertEquals(7501, Config.getPref().getInt("pluginmanager.version", 111));
        // not mocking the time so just check it's not its original value
        assertNotEquals("999", Config.getPref().get("pluginmanager.lastupdate", "999"));
    }

    /**
     * test update of plugins when our current JOSM version prevents us from using all but the version
     * we already have, which is still listed.
     * @throws Exception on failure
     */
    @JOSMTestRules.OverrideAssumeRevision("Revision: 7000\n")
    @Test
    public void testUpdatePluginsExistingVersionLatestPossible() throws Exception {
        TestUtils.assumeWorkingJMockit();

        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(this.referenceQuxJarNewest, ImmutableMap.of(
                "7500_Plugin-Url", "432;http://localhost:" + this.pluginServerRule.port() + "/dont.jar",
                "7499_Plugin-Url", "346;http://localhost:" + this.pluginServerRule.port() + "/even.jar",
                "6999_Plugin-Url", "345;http://localhost:" + this.pluginServerRule.port() + "/bother.jar"
            ))
        );
        pluginServer.applyToWireMockServer(this.pluginServerRule);
        Config.getPref().putList("plugins", ImmutableList.of("qux_plugin", "baz_plugin"));

        // catch any (unexpected) attempts to show us an ExtendedDialog
        new ExtendedDialogMocker();

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
        assertEquals("345", updatedPlugins.get(1).localversion);

        assertFalse(targetBazJarNew.exists());
        assertFalse(targetQuxJarNew.exists());

        // should be as before
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);
        TestUtils.assertFileContentsEqual(this.referenceQuxJarOld, this.targetQuxJar);

        // only the plugins list should have been downloaded
        assertEquals(1, WireMock.getAllServeEvents().size());
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));

        assertEquals(7000, Config.getPref().getInt("pluginmanager.version", 111));
        // not mocking the time so just check it's not its original value
        assertNotEquals("999", Config.getPref().get("pluginmanager.lastupdate", "999"));
    }
}
