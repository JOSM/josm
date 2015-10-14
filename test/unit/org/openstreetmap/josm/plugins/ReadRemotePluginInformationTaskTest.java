// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;

/**
 * Unit tests of {@link ReadRemotePluginInformationTask} class.
 */
public class ReadRemotePluginInformationTaskTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of plugin list download.
     */
    @Test
    public void testDownloadPluginList() {
        ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(
                Main.pref.getOnlinePluginSites());
        pluginInfoDownloadTask.run();
        List<PluginInformation> list = pluginInfoDownloadTask.getAvailablePlugins();
        assertFalse(list.isEmpty());
        PluginInformation info = list.get(0);
        assertFalse(info.getName().isEmpty());
        assertFalse(info.getClass().getName().isEmpty());
    }
}
