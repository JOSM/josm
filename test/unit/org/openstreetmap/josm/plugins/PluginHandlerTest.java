// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreferenceTest;
import org.openstreetmap.josm.plugins.PluginHandler.DeprecatedPlugin;
import org.openstreetmap.josm.plugins.PluginHandler.PluginInformationAction;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link PluginHandler} class.
 */
public class PluginHandlerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * Unit test of methods {@link DeprecatedPlugin#equals} and {@link DeprecatedPlugin#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(DeprecatedPlugin.class).usingGetClass().verify();
    }

    /**
     * Unit test of {@link PluginHandler#buildListOfPluginsToLoad}.
     */
    @Test
    public void testBuildListOfPluginsToLoad() {
        final String old = System.getProperty("josm.plugins");
        try {
            System.setProperty("josm.plugins",
                    Utils.join(",", PluginHandler.DEPRECATED_PLUGINS) + "," +
                    Utils.join(",", Arrays.asList(PluginHandler.UNMAINTAINED_PLUGINS)));
            List<PluginInformation> list = PluginHandler.buildListOfPluginsToLoad(null, null);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        } finally {
            if (old != null) {
                System.setProperty("josm.plugins", old);
            } else {
                System.clearProperty("josm.plugins");
            }
        }
    }

    /**
     * Unit test of {@link PluginHandler#filterDeprecatedPlugins}.
     */
    @Test
    public void testFilterDeprecatedPlugins() {
        List<String> plugins = new ArrayList<>(Arrays.asList("foo", "bar", "imagery"));
        PluginHandler.filterDeprecatedPlugins(Main.parent, plugins);
        assertEquals(2, plugins.size());
        assertFalse(plugins.contains("imagery"));
    }

    /**
     * Unit test of {@link PluginHandler#filterUnmaintainedPlugins}.
     */
    @Test
    public void testFilterUnmaintainedPlugins() {
        List<String> plugins = new ArrayList<>(Arrays.asList("foo", "bar", "gpsbabelgui"));
        PluginHandler.filterUnmaintainedPlugins(Main.parent, plugins);
        assertEquals(2, plugins.size());
        assertFalse(plugins.contains("gpsbabelgui"));
    }

    /**
     * Unit test of {@link PluginInformationAction} class.
     * @throws PluginException if an error occurs
     */
    @Test
    public void testPluginInformationAction() throws PluginException {
        PluginInformationAction action = new PluginInformationAction(PluginPreferenceTest.getDummyPluginInformation());
        assertEquals(
                "Ant-Version: Apache Ant 1.9.6\n" +
                "Author: Don-vip\n" +
                "Created-By: 1.7.0_91-b02 (Oracle Corporation)\n" +
                "Manifest-Version: 1.0\n" +
                "Plugin-Canloadatruntime: true\n" +
                "Plugin-Class: org.openstreetmap.josm.plugins.fr.epci.EpciPlugin\n" +
                "Plugin-Date: 2015-11-19T08:21:07.645033Z\n" +
                "Plugin-Description: Handling of French EPCIs (boundary=local_authority)\n" +
                "Plugin-Early: true\n" +
                "Plugin-Link: http://wiki.openstreetmap.org/wiki/FR:JOSM/Fr:Plugin/EPCI-fr\n" +
                "Plugin-Mainversion: 7001\n" +
                "Plugin-Version: 31772\n", action.getText());
        action.actionPerformed(null);
    }
}
