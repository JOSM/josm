// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.PluginServer;
import org.openstreetmap.josm.testutils.annotations.AssertionsInEDT;
import org.openstreetmap.josm.testutils.annotations.AssumeRevision;
import org.openstreetmap.josm.testutils.annotations.FullPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import mockit.MockUp;

/**
 * Higher level tests of {@link PluginPreference} class.
 */
@AssumeRevision("Revision: 10000\n")
@AssertionsInEDT
@FullPreferences
@Main
class PluginPreferenceHighLevelTest {
    /**
     * Plugin server mock.
     */
    @RegisterExtension
    static WireMockExtension pluginServerRule = WireMockExtension.newInstance()
            .options(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()))
            .build();

    /**
     * Setup test.
     * @throws ReflectiveOperationException never
     */
    @BeforeEach
    public void setUp() throws ReflectiveOperationException {

        // some other tests actually go ahead and load plugins (notably at time of writing,
        // MainApplicationTest$testUpdateAndLoadPlugins), which really isn't a reversible operation.
        // it is, however, possible to pretend to our tests temporarily that they *aren't* loaded by
        // setting the PluginHandler#pluginList to empty for the duration of this test. ideally these
        // other tests wouldn't be so badly behaved or would at least do this from a separate batch
        // but this works for now
        @SuppressWarnings("unchecked")
        final Collection<PluginProxy> pluginList = (Collection<PluginProxy>) TestUtils.getPrivateStaticField(
            PluginHandler.class,
            "pluginList"
        );
        this.originalPluginList = new ArrayList<>(pluginList);
        pluginList.clear();

        Config.getPref().putInt("pluginmanager.version", 999);
        Config.getPref().put("pluginmanager.lastupdate", "999");
        Config.getPref().putList("pluginmanager.sites",
            Collections.singletonList(pluginServerRule.url("/plugins"))
        );

        this.referenceDummyJarOld = new File(TestUtils.getTestDataRoot(), "__files/plugin/dummy_plugin.v31701.jar");
        this.referenceDummyJarNew = new File(TestUtils.getTestDataRoot(), "__files/plugin/dummy_plugin.v31772.jar");
        this.referenceBazJarOld = new File(TestUtils.getTestDataRoot(), "__files/plugin/baz_plugin.v6.jar");
        this.referenceBazJarNew = new File(TestUtils.getTestDataRoot(), "__files/plugin/baz_plugin.v7.jar");
        this.pluginDir = Preferences.main().getPluginsDirectory();
        this.targetDummyJar = new File(this.pluginDir, "dummy_plugin.jar");
        this.targetDummyJarNew = new File(this.pluginDir, "dummy_plugin.jar.new");
        this.targetBazJar = new File(this.pluginDir, "baz_plugin.jar");
        this.targetBazJarNew = new File(this.pluginDir, "baz_plugin.jar.new");
        this.pluginDir.mkdirs();
    }

    /**
     * Tear down.
     * @throws ReflectiveOperationException never
     */
    @AfterEach
    public void tearDown() throws ReflectiveOperationException {
        // restore actual PluginHandler#pluginList
        @SuppressWarnings("unchecked")
        final Collection<PluginProxy> pluginList = (Collection<PluginProxy>) TestUtils.getPrivateStaticField(
            PluginHandler.class,
            "pluginList"
        );
        pluginList.clear();
        pluginList.addAll(this.originalPluginList);
    }

    private Collection<PluginProxy> originalPluginList;

    private File pluginDir;
    private File referenceDummyJarOld;
    private File referenceDummyJarNew;
    private File referenceBazJarOld;
    private File referenceBazJarNew;
    private File targetDummyJar;
    private File targetDummyJarNew;
    private File targetBazJar;
    private File targetBazJarNew;

    /**
     * Tests choosing a new plugin to install without upgrading an already-installed plugin
     * @throws Exception never
     */
    @Test
    void testInstallWithoutUpdate() throws Exception {
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(null, Collections.singletonMap("Plugin-Version", "2"), "irrelevant_plugin")
        );
        pluginServer.applyToWireMockServer(pluginServerRule.getRuntimeInfo());
        Config.getPref().putList("plugins", Collections.singletonList("dummy_plugin"));

        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
            Collections.singletonMap(
                "<html>The following plugin has been downloaded <strong>successfully</strong>:"
                + "<ul><li>baz_plugin (6)</li></ul>"
                + "You have to restart JOSM for some settings to take effect."
                + "<br/><br/>Would you like to restart now?</html>",
                "Cancel"
            )
        );

        Files.copy(this.referenceDummyJarOld.toPath(), this.targetDummyJar.toPath());

        final PreferenceTabbedPane tabbedPane = new PreferenceTabbedPane();

        tabbedPane.buildGui();
        // PluginPreference is already added to PreferenceTabbedPane by default
        tabbedPane.selectTabByPref(PluginPreference.class);

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "downloadListButton")).doClick()
        );

        Awaitility.await().atMost(2000, MILLISECONDS).until(() -> Config.getPref().getInt("pluginmanager.version", 999) != 999);

        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.resetRequests();

        final PluginPreferencesModel model = (PluginPreferencesModel) TestUtils.getPrivateField(
            tabbedPane.getPluginPreference(),
            "model"
        );

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        // questionably correct
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());
        assertEquals(model.getDisplayedPlugins(), model.getAvailablePlugins());

        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin", "irrelevant_plugin"),
                model.getAvailablePlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Collections.singletonList("dummy_plugin"),
                model.getSelectedPlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("(null)", "31701", "(null)"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.localversion == null ? "(null)" : pi.localversion
        ).collect(Collectors.toList()));
        assertEquals(Arrays.asList("6", "31772", "2"),
                model.getAvailablePlugins().stream().map((pi) -> pi.version).collect(Collectors.toList()));

        // now we're going to choose to install baz_plugin
        model.setPluginSelected("baz_plugin", true);

        assertEquals(Collections.singletonList("baz_plugin"),
                model.getNewlyActivatedPlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        assertEquals(Collections.singletonList("baz_plugin"),
                model.getPluginsScheduledForUpdateOrDownload().stream().map(PluginInformation::getName).collect(Collectors.toList()));

        tabbedPane.savePreferences();

        TestUtils.syncEDTAndWorkerThreads();

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Restart", invocationLogEntry[2]);

        // dummy_plugin jar shouldn't have been updated
        TestUtils.assertFileContentsEqual(this.referenceDummyJarOld, this.targetDummyJar);
        // baz_plugin jar should have been installed
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);

        // neither of these .jar.new files should have been left hanging round
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // the advertised version of dummy_plugin shouldn't have been fetched
        pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        // but the advertized version of baz_plugin *should* have
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v6.jar")));

        // pluginmanager.version has been set to the current version
        // questionably correct
        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // however pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));

        // baz_plugin should have been added to the plugins list
        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"),
                Config.getPref().getList("plugins", null).stream().sorted().collect(Collectors.toList()));
    }

    /**
     * Tests a plugin being disabled without applying available upgrades
     * @throws Exception never
     */
    @Test
    void testDisablePluginWithUpdatesAvailable() throws Exception {
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew),
            new PluginServer.RemotePlugin(null, null, "irrelevant_plugin")
        );
        pluginServer.applyToWireMockServer(pluginServerRule.getRuntimeInfo());
        Config.getPref().putList("plugins", Arrays.asList("baz_plugin", "dummy_plugin"));

        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
            Collections.singletonMap(
                "<html>You have to restart JOSM for some settings to take effect."
                + "<br/><br/>Would you like to restart now?</html>",
                "Cancel"
            )
        );

        Files.copy(this.referenceDummyJarOld.toPath(), this.targetDummyJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final PreferenceTabbedPane tabbedPane = new PreferenceTabbedPane();

        tabbedPane.buildGui();
        // PluginPreference is already added to PreferenceTabbedPane by default
        tabbedPane.selectTabByPref(PluginPreference.class);

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "downloadListButton")).doClick()
        );

        Awaitility.await().atMost(2000, MILLISECONDS).until(() -> Config.getPref().getInt("pluginmanager.version", 999) != 999);

        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.resetRequests();

        final PluginPreferencesModel model = (PluginPreferencesModel) TestUtils.getPrivateField(
            tabbedPane.getPluginPreference(),
            "model"
        );

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        // questionably correct
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());
        assertEquals(model.getDisplayedPlugins(), model.getAvailablePlugins());

        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin", "irrelevant_plugin"),
                model.getAvailablePlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"),
                model.getSelectedPlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("6", "31701", "(null)"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.localversion == null ? "(null)" : pi.localversion
        ).collect(Collectors.toList()));
        assertEquals(Arrays.asList("7", "31772", "(null)"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.version == null ? "(null)" : pi.version
        ).collect(Collectors.toList()));

        // now we're going to choose to disable baz_plugin
        model.setPluginSelected("baz_plugin", false);

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertEquals(Collections.singletonList("baz_plugin"),
                model.getNewlyDeactivatedPlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        // questionably correct
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());

        tabbedPane.savePreferences();

        TestUtils.syncEDTAndWorkerThreads();

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Restart", invocationLogEntry[2]);

        // dummy_plugin jar shouldn't have been updated
        TestUtils.assertFileContentsEqual(this.referenceDummyJarOld, this.targetDummyJar);
        // baz_plugin jar shouldn't have been deleted
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);

        // neither of these .jar.new files have a reason to be here
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // neither of the new jars have been fetched
        pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v6.jar")));

        // pluginmanager.version has been set to the current version
        // questionably correct
        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // however pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));

        // baz_plugin should have been removed from the installed plugins list
        assertEquals(Collections.singletonList("dummy_plugin"),
                Config.getPref().getList("plugins", null).stream().sorted().collect(Collectors.toList()));
    }

    /**
     * Demonstrates behaviour exhibited when attempting to update a single plugin when multiple updates
     * are available by deselecting it before clicking the update button then reselecting it.
     *
     * This is probably NOT desirable and should be fixed, however this test documents the behaviour.
     * @throws Exception never
     */
    @Test
    void testUpdateOnlySelectedPlugin() throws Exception {
        TestUtils.assumeWorkingJMockit();
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew)
        );
        pluginServer.applyToWireMockServer(pluginServerRule.getRuntimeInfo());
        Config.getPref().putList("plugins", Arrays.asList("baz_plugin", "dummy_plugin"));

        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker();

        Files.copy(this.referenceDummyJarOld.toPath(), this.targetDummyJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final PreferenceTabbedPane tabbedPane = new PreferenceTabbedPane();

        tabbedPane.buildGui();
        // PluginPreference is already added to PreferenceTabbedPane by default
        tabbedPane.selectTabByPref(PluginPreference.class);

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "downloadListButton")).doClick()
        );

        TestUtils.syncEDTAndWorkerThreads();

        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.resetRequests();

        final PluginPreferencesModel model = (PluginPreferencesModel) TestUtils.getPrivateField(
            tabbedPane.getPluginPreference(),
            "model"
        );

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        // questionably correct
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());
        assertEquals(model.getDisplayedPlugins(), model.getAvailablePlugins());

        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"),
                model.getAvailablePlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"),
                model.getSelectedPlugins().stream().map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("6", "31701"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.localversion == null ? "(null)" : pi.localversion
        ).collect(Collectors.toList()));
        assertEquals(Arrays.asList("7", "31772"), model.getAvailablePlugins().stream().map((pi) -> pi.version).collect(Collectors.toList()));

        // now we're going to choose not to update baz_plugin
        model.setPluginSelected("baz_plugin", false);

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertEquals(Collections.singletonList("baz_plugin"), model.getNewlyDeactivatedPlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        // questionably correct
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());

        // prepare haMocker to handle this message
        haMocker.getMockResultMap().put(
            "<html>The following plugin has been downloaded <strong>successfully</strong>:"
            + "<ul><li>dummy_plugin (31772)</li></ul>Please restart JOSM to activate the "
            + "downloaded plugins.</html>",
            "OK"
        );

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "updatePluginsButton")).doClick()
        );

        TestUtils.syncEDTAndWorkerThreads();

        assertTrue(jopsMocker.getInvocationLog().isEmpty());
        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Update plugins", invocationLogEntry[2]);

        // dummy_plugin jar should have been updated
        TestUtils.assertFileContentsEqual(this.referenceDummyJarNew, this.targetDummyJar);
        // but baz_plugin jar shouldn't have been
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);

        // any .jar.new files should have been removed
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // the plugin list was rechecked
        // questionably necessary
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        // dummy_plugin has been fetched
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        // baz_plugin has not
        pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v7.jar")));
        pluginServerRule.resetRequests();

        // pluginmanager.version has been set to the current version
        // questionably correct
        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // however pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));

        // plugins list shouldn't have been altered, we haven't hit save yet
        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"),
                Config.getPref().getList("plugins", null).stream().sorted().collect(Collectors.toList()));

        // the model's selection state should be largely as before
        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertEquals(Collections.singletonList("baz_plugin"), model.getNewlyDeactivatedPlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());

        // but now we re-select baz_plugin so that it isn't removed/disabled
        model.setPluginSelected("baz_plugin", true);

        // this has caused baz_plugin to be interpreted as a plugin "for download"
        // questionably correct
        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        assertEquals(Collections.singletonList("baz_plugin"), model.getPluginsScheduledForUpdateOrDownload().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));

        // prepare jopsMocker to handle this message
        jopsMocker.getMockResultMap().put(
            "<html>The following plugin has been downloaded <strong>successfully</strong>:"
            + "<ul><li>baz_plugin (7)</li></ul></html>",
            JOptionPane.OK_OPTION
        );

        tabbedPane.savePreferences();

        TestUtils.syncEDTAndWorkerThreads();

        // from previous haMocker invocation
        assertEquals(1, haMocker.getInvocationLog().size());
        // we've been alerted that (the new version of) baz_plugin was installed
        // questionably correct
        assertEquals(1, jopsMocker.getInvocationLog().size());
        invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);

        // dummy_plugin jar is still the updated version
        TestUtils.assertFileContentsEqual(this.referenceDummyJarNew, this.targetDummyJar);
        // but now the baz_plugin jar has been too
        // questionably correct
        TestUtils.assertFileContentsEqual(this.referenceBazJarNew, this.targetBazJar);

        // all .jar.new files have been deleted
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // dummy_plugin was not fetched
        pluginServerRule.verify(0, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));
        // baz_plugin however was fetched
        // questionably correct
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.v7.jar")));

        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));
    }

    /**
     * Tests the effect of requesting a "plugin update" when everything is up to date
     * @throws Exception never
     */
    @Test
    void testUpdateWithNoAvailableUpdates() throws Exception {
        TestUtils.assumeWorkingJMockit();
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarOld),
            new PluginServer.RemotePlugin(this.referenceBazJarOld),
            new PluginServer.RemotePlugin(null, Collections.singletonMap("Plugin-Version", "123"), "irrelevant_plugin")
        );
        pluginServer.applyToWireMockServer(pluginServerRule.getRuntimeInfo());
        Config.getPref().putList("plugins", Arrays.asList("baz_plugin", "dummy_plugin"));

        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
            Collections.singletonMap(
                "All installed plugins are up to date. JOSM does not have to download newer versions.",
                "OK"
            )
        );
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker();

        Files.copy(this.referenceDummyJarOld.toPath(), this.targetDummyJar.toPath());
        Files.copy(this.referenceBazJarOld.toPath(), this.targetBazJar.toPath());

        final PreferenceTabbedPane tabbedPane = new PreferenceTabbedPane();

        tabbedPane.buildGui();
        // PluginPreference is already added to PreferenceTabbedPane by default
        tabbedPane.selectTabByPref(PluginPreference.class);

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "downloadListButton")).doClick()
        );

        TestUtils.syncEDTAndWorkerThreads();

        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.resetRequests();

        final PluginPreferencesModel model = (PluginPreferencesModel) TestUtils.getPrivateField(
            tabbedPane.getPluginPreference(),
            "model"
        );

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());
        assertEquals(model.getDisplayedPlugins(), model.getAvailablePlugins());

        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin", "irrelevant_plugin"), model.getAvailablePlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"), model.getSelectedPlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList("6", "31701", "(null)"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.localversion == null ? "(null)" : pi.localversion
        ).collect(Collectors.toList()));
        assertEquals(Arrays.asList("6", "31701", "123"), model.getAvailablePlugins()
                .stream().map((pi) -> pi.version).collect(Collectors.toList()));

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "updatePluginsButton")).doClick()
        );

        TestUtils.syncEDTAndWorkerThreads();

        assertTrue(jopsMocker.getInvocationLog().isEmpty());
        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Plugins up to date", invocationLogEntry[2]);

        // neither jar should have changed
        TestUtils.assertFileContentsEqual(this.referenceDummyJarOld, this.targetDummyJar);
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);

        // no reason for any .jar.new files
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // the plugin list was rechecked
        // questionably necessary
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        // that should have been the only request to our PluginServer
        assertEquals(1, pluginServerRule.getAllServeEvents().size());
        pluginServerRule.resetRequests();

        // pluginmanager.version has been set to the current version
        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));

        // plugins list shouldn't have been altered
        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"), Config.getPref().getList("plugins", null)
                .stream().sorted().collect(Collectors.toList()));

        // the model's selection state should be largely as before
        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());

        tabbedPane.savePreferences();

        TestUtils.syncEDTAndWorkerThreads();

        assertTrue(jopsMocker.getInvocationLog().isEmpty());
        assertEquals(1, haMocker.getInvocationLog().size());

        // both jars are still the original version
        TestUtils.assertFileContentsEqual(this.referenceDummyJarOld, this.targetDummyJar);
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);

        // no reason for any .jar.new files
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // none of PluginServer's URLs should have been touched
        assertEquals(0, pluginServerRule.getAllServeEvents().size());

        // pluginmanager.version has been set to the current version
        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));
    }

    /**
     * Tests installing a single plugin which is marked as "Canloadatruntime"
     * @throws Exception never
     */
    @Test
    void testInstallWithoutRestartRequired() throws Exception {
        TestUtils.assumeWorkingJMockit();
        final boolean[] loadPluginsCalled = new boolean[] {false};
        new MockUp<PluginHandler>() {
            @mockit.Mock
            private void loadPlugins(
                final Component parent,
                final Collection<org.openstreetmap.josm.plugins.PluginInformation> plugins,
                final org.openstreetmap.josm.gui.progress.ProgressMonitor monitor
            ) {
                assertEquals(1, plugins.size());
                assertEquals("dummy_plugin", plugins.iterator().next().name);
                assertEquals("31772", plugins.iterator().next().localversion);
                loadPluginsCalled[0] = true;
            }
        };

        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew)
        );
        pluginServer.applyToWireMockServer(pluginServerRule.getRuntimeInfo());
        Config.getPref().putList("plugins", Collections.emptyList());

        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(Collections.singletonMap(
            "<html>The following plugin has been downloaded <strong>successfully</strong>:"
            + "<ul><li>dummy_plugin (31772)</li></ul></html>",
            JOptionPane.OK_OPTION
        ));

        final PreferenceTabbedPane tabbedPane = new PreferenceTabbedPane();

        tabbedPane.buildGui();
        // PluginPreference is already added to PreferenceTabbedPane by default
        tabbedPane.selectTabByPref(PluginPreference.class);

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "downloadListButton")).doClick()
        );

        TestUtils.syncEDTAndWorkerThreads();

        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.resetRequests();

        final PluginPreferencesModel model = (PluginPreferencesModel) TestUtils.getPrivateField(
            tabbedPane.getPluginPreference(),
            "model"
        );

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());
        assertEquals(model.getDisplayedPlugins(), model.getAvailablePlugins());

        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"), model.getAvailablePlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertTrue(model.getSelectedPlugins().isEmpty());
        assertEquals(Arrays.asList("(null)", "(null)"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.localversion == null ? "(null)" : pi.localversion
        ).collect(Collectors.toList()));
        assertEquals(Arrays.asList("7", "31772"), model.getAvailablePlugins().stream().map((pi) -> pi.version).collect(Collectors.toList()));

        // now we select dummy_plugin
        model.setPluginSelected("dummy_plugin", true);

        // model should now reflect this
        assertEquals(Collections.singletonList("dummy_plugin"), model.getNewlyActivatedPlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());

        tabbedPane.savePreferences();

        TestUtils.syncEDTAndWorkerThreads();

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);

        assertTrue(haMocker.getInvocationLog().isEmpty());

        // any .jar.new files should have been deleted
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // dummy_plugin was fetched
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.v31772.jar")));

        // the dummy_plugin jar has been installed
        TestUtils.assertFileContentsEqual(this.referenceDummyJarNew, this.targetDummyJar);
        // the baz_plugin jar has not
        assertFalse(this.targetBazJar.exists());

        // loadPlugins(...) was called (with expected parameters)
        assertTrue(loadPluginsCalled[0]);

        // pluginmanager.version has been set to the current version
        assertEquals(10000, Config.getPref().getInt("pluginmanager.version", 111));
        // pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));
    }

    /**
     * Tests installing a single plugin which has multiple versions advertised, with our JOSM version
     * preventing us from using the latest version
     * @throws Exception on failure
     */
    @AssumeRevision("Revision: 7000\n")
    @Test
    void testInstallMultiVersion() throws Exception {
        TestUtils.assumeWorkingJMockit();

        final String bazOldServePath = "/baz/old.jar";
        final PluginServer pluginServer = new PluginServer(
            new PluginServer.RemotePlugin(this.referenceDummyJarNew),
            new PluginServer.RemotePlugin(this.referenceBazJarNew, Collections.singletonMap(
                "6800_Plugin-Url", "6;" + pluginServerRule.url(bazOldServePath)
            ))
        );
        pluginServer.applyToWireMockServer(pluginServerRule.getRuntimeInfo());
        // need to actually serve this older jar from somewhere
        pluginServerRule.stubFor(
            WireMock.get(WireMock.urlEqualTo(bazOldServePath)).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/java-archive").withBodyFile(
                    "plugin/baz_plugin.v6.jar"
                )
            )
        );
        Config.getPref().putList("plugins", Collections.emptyList());

        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(Collections.singletonMap(
            "<html>The following plugin has been downloaded <strong>successfully</strong>:"
            + "<ul><li>baz_plugin (6)</li></ul>"
            + "You have to restart JOSM for some settings to take effect.<br/><br/>"
            + "Would you like to restart now?</html>",
            "Cancel"
        ));
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker();

        final PreferenceTabbedPane tabbedPane = new PreferenceTabbedPane();

        tabbedPane.buildGui();
        // PluginPreference is already added to PreferenceTabbedPane by default
        tabbedPane.selectTabByPref(PluginPreference.class);

        GuiHelper.runInEDTAndWait(
            () -> ((javax.swing.JButton) TestUtils.getComponentByName(tabbedPane, "downloadListButton")).doClick()
        );

        TestUtils.syncEDTAndWorkerThreads();

        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        pluginServerRule.resetRequests();

        final PluginPreferencesModel model = (PluginPreferencesModel) TestUtils.getPrivateField(
            tabbedPane.getPluginPreference(),
            "model"
        );

        assertTrue(model.getNewlyActivatedPlugins().isEmpty());
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());
        assertTrue(model.getPluginsScheduledForUpdateOrDownload().isEmpty());
        assertEquals(model.getDisplayedPlugins(), model.getAvailablePlugins());

        assertEquals(Arrays.asList("baz_plugin", "dummy_plugin"), model.getAvailablePlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertTrue(model.getSelectedPlugins().isEmpty());
        assertEquals(Arrays.asList("(null)", "(null)"), model.getAvailablePlugins().stream().map(
            (pi) -> pi.localversion == null ? "(null)" : pi.localversion
        ).collect(Collectors.toList()));
        assertEquals(Arrays.asList("6", "31772"), model.getAvailablePlugins().stream().map((pi) -> pi.version).collect(Collectors.toList()));

        // now we select dummy_plugin
        model.setPluginSelected("baz_plugin", true);

        // model should now reflect this
        assertEquals(Collections.singletonList("baz_plugin"), model.getNewlyActivatedPlugins().stream()
                .map(PluginInformation::getName).collect(Collectors.toList()));
        assertTrue(model.getNewlyDeactivatedPlugins().isEmpty());

        tabbedPane.savePreferences();

        TestUtils.syncEDTAndWorkerThreads();

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Restart", invocationLogEntry[2]);

        assertTrue(jopsMocker.getInvocationLog().isEmpty());

        // any .jar.new files should have been deleted
        assertFalse(targetDummyJarNew.exists());
        assertFalse(targetBazJarNew.exists());

        // dummy_plugin was fetched
        pluginServerRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(bazOldServePath)));

        // the "old" baz_plugin jar has been installed
        TestUtils.assertFileContentsEqual(this.referenceBazJarOld, this.targetBazJar);
        // the dummy_plugin jar has not
        assertFalse(this.targetDummyJar.exists());

        // pluginmanager.version has been set to the current version
        assertEquals(7000, Config.getPref().getInt("pluginmanager.version", 111));
        // pluginmanager.lastupdate hasn't been updated
        // questionably correct
        assertEquals("999", Config.getPref().get("pluginmanager.lastupdate", "111"));
    }
}
