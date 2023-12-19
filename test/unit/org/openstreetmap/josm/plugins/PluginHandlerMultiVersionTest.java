// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.PluginServer;
import org.openstreetmap.josm.testutils.annotations.AssumeRevision;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Test parts of {@link PluginHandler} class with plugins that advertise multiple versions for compatibility.
 */
@FullPreferences
@Main
class PluginHandlerMultiVersionTest {
    /**
     * Plugin server mock.
     */
    @RegisterExtension
    static WireMockExtension pluginServerRule = WireMockExtension.newInstance()
            .options(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot())).build();

    /**
     * Setup test.
     */
    @BeforeEach
    void setUp() {
        Config.getPref().putInt("pluginmanager.version", 999);
        Config.getPref().put("pluginmanager.lastupdate", "999");
        Config.getPref().putList("pluginmanager.sites",
                Collections.singletonList(pluginServerRule.url("/plugins"))
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
    @AssumeRevision("Revision: 7501\n")
    @Test
    void testUpdatePluginsOneMultiVersion(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        TestUtils.assumeWorkingJMockit();

        final String quxNewerServePath = "/qux/newer.jar";
        final Map<String, String> attrOverrides = new HashMap<String, String>() {{
            put("7500_Plugin-Url", "432;" + pluginServerRule.url(quxNewerServePath));
            put("7499_Plugin-Url", "346;" + pluginServerRule.url("/not/served.jar"));
            put("6999_Plugin-Url", "345;" + pluginServerRule.url("/not/served/eithejar"));
        }};
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(this.referenceQuxJarNewest, attrOverrides)
        );
        pluginServer.applyToWireMockServer(wireMockRuntimeInfo);
        // need to actually serve this older jar from somewhere
        wireMockRuntimeInfo.getWireMock().register(
            WireMock.get(WireMock.urlEqualTo(quxNewerServePath)).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/java-archive").withBodyFile(
                    referencePathQuxJarNewer
                )
            )
        );
        Config.getPref().putList("plugins", Arrays.asList("qux_plugin", "baz_plugin"));

        // catch any (unexpected) attempts to show us an ExtendedDialog
        new ExtendedDialogMocker();

        Files.copy(this.referenceQuxJarOld.toPath(), this.targetQuxJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            MainApplication.getMainFrame(),
            null,
            null,
            false
        ).stream().sorted(Comparator.comparing(a -> a.name)).collect(Collectors.toList());

        assertEquals(2, updatedPlugins.size());

        assertEquals("baz_plugin", updatedPlugins.get(0).name);
        assertEquals("6", updatedPlugins.get(0).localversion);

        assertEquals("qux_plugin", updatedPlugins.get(1).name);
        assertEquals("432", updatedPlugins.get(1).localversion);

        assertFalse(targetBazJarNew.exists());
        assertFalse(targetQuxJarNew.exists());

        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);
        TestUtils.assertFileContentsEqual(this.referenceQuxJarNewer, this.targetQuxJar);

        assertEquals(2, pluginServerRule.getAllServeEvents().size());
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(quxNewerServePath)));

        assertEquals(7501, Config.getPref().getInt("pluginmanager.version", 111));
        // not mocking the time so just check it's not its original value
        assertNotEquals("999", Config.getPref().get("pluginmanager.lastupdate", "999"));
    }

    /**
     * test update of plugins when our current JOSM version prevents us from using all but the version
     * we already have, which is still listed.
     * @throws Exception on failure
     */
    @AssumeRevision("Revision: 7000\n")
    @Test
    void testUpdatePluginsExistingVersionLatestPossible(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        TestUtils.assumeWorkingJMockit();

        final Map<String, String> attrOverrides = new HashMap<String, String>() {{
            put("7500_Plugin-Url", "432;" + pluginServerRule.url("/dont.jar"));
            put("7499_Plugin-Url", "346;" + pluginServerRule.url("/even.jar"));
            put("6999_Plugin-Url", "345;" + pluginServerRule.url("/bother.jar"));
        }};
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(this.referenceQuxJarNewest, attrOverrides)
        );
        pluginServer.applyToWireMockServer(wireMockRuntimeInfo);
        Config.getPref().putList("plugins", Arrays.asList("qux_plugin", "baz_plugin"));

        // catch any (unexpected) attempts to show us an ExtendedDialog
        new ExtendedDialogMocker();

        Files.copy(this.referenceQuxJarOld.toPath(), this.targetQuxJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            MainApplication.getMainFrame(),
            null,
            null,
            false
        ).stream().sorted(Comparator.comparing(a -> a.name)).collect(Collectors.toList());

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
        assertEquals(1, pluginServerRule.getAllServeEvents().size());
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));

        assertEquals(7000, Config.getPref().getInt("pluginmanager.version", 111));
        // not mocking the time so just check it's not its original value
        assertNotEquals("999", Config.getPref().get("pluginmanager.lastupdate", "999"));
    }
}
