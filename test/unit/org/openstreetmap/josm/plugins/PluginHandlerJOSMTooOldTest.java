// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.PluginServer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.github.tomakehurst.wiremock.client.WireMock;

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
    public PluginServer.PluginServerRule pluginServerRule = new PluginServer(
        new PluginServer.RemotePlugin(new File(TestUtils.getTestDataRoot(), "__files/plugin/dummy_plugin.v31772.jar")),
        new PluginServer.RemotePlugin(
            new File(TestUtils.getTestDataRoot(), "__files/plugin/baz_plugin.v7.jar")
        )
    ).asWireMockRule();

    @Before
    public void setUp() throws Exception {
        Config.getPref().putInt("pluginmanager.version", 999);
        Config.getPref().put("pluginmanager.lastupdate", "999");
        Config.getPref().putList("plugins", ImmutableList.of("dummy_plugin", "baz_plugin"));
        Config.getPref().putList("pluginmanager.sites",
            ImmutableList.of(String.format("http://localhost:%s/plugins", this.pluginServerRule.port()))
        );
    }

    private final String bazPluginVersionReqString = "JOSM version 8,001 required for plugin baz_plugin.";
    private final String dummyPluginVersionReqString = "JOSM version 7,001 required for plugin dummy_plugin.";
    private final String dummyPluginFailedString = "<html>Updating the following plugin has failed:<ul><li>dummy_plugin</li></ul>"
        + "Please open the Preference Dialog after JOSM has started and try to update it manually.</html>";

    /**
     * test update of plugins when those plugins turn out to require a higher JOSM version, but the
     * user chooses to update them anyway.
     */
    @Test
    public void testUpdatePluginsDownloadBoth() {
        final ExtendedDialogMocker edMocker = new ExtendedDialogMocker(ImmutableMap.<String, Object>builder()
            .put(this.bazPluginVersionReqString, "Download Plugin")
            .put(this.dummyPluginVersionReqString, "Download Plugin")
            .build()
        );

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            Main.parent,
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
     */
    @Test
    public void testUpdatePluginsSkipOne() {
        final ExtendedDialogMocker edMocker = new ExtendedDialogMocker(ImmutableMap.<String, Object>builder()
            .put(this.bazPluginVersionReqString, "Download Plugin")
            .put(this.dummyPluginVersionReqString, "Skip Download")
            .build()
        );
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(ImmutableMap.<String, Object>builder()
            .put(this.dummyPluginFailedString, "OK")
            .build()
        );

        final List<PluginInformation> updatedPlugins = PluginHandler.updatePlugins(
            Main.parent,
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
        assertEquals(null, updatedPlugins.get(1).localversion);

        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        this.pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        this.pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v7.jar")));

        // shouldn't have been updated
        assertEquals(Config.getPref().getInt("pluginmanager.version", 111), 999);
        assertEquals(Config.getPref().get("pluginmanager.lastupdate", "999"), "999");
    }
}
