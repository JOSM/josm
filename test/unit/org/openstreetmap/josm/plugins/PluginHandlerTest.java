// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreferenceTest;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.plugins.PluginHandler.DeprecatedPlugin;
import org.openstreetmap.josm.plugins.PluginHandler.PluginInformationAction;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.tools.Utils;

import com.google.common.collect.ImmutableMap;

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
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of methods {@link DeprecatedPlugin#equals} and {@link DeprecatedPlugin#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(DeprecatedPlugin.class).usingGetClass().verify();
    }

    /**
     * Unit test of {@link PluginHandler#buildListOfPluginsToLoad}.
     */
    @Test
    public void testBuildListOfPluginsToLoad() {
        TestUtils.assumeWorkingJMockit();
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker() {
            @Override
            public String getStringFromMessage(final Object message) {
                return ((String) message).substring(0, 66);
            }
        };
        haMocker.getMockResultMap().put(
            "<html>JOSM could not find information about the following plugins:",
            "OK"
        );
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

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link PluginHandler#filterDeprecatedPlugins}.
     */
    @Test
    public void testFilterDeprecatedPlugins() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            ImmutableMap.<String, Object>of(
                "<html>The following plugin is no longer necessary and has been deactivated:" +
                    "<ul><li>imagery (integrated into main program)</li></ul></html>",
                0
            )
        );

        List<String> plugins = new ArrayList<>(Arrays.asList("foo", "bar", "imagery"));
        PluginHandler.filterDeprecatedPlugins(MainApplication.getMainFrame(), plugins);
        assertEquals(2, plugins.size());
        assertFalse(plugins.contains("imagery"));

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Warning", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link PluginHandler#filterUnmaintainedPlugins}.
     */
    @Test
    public void testFilterUnmaintainedPlugins() {
        TestUtils.assumeWorkingJMockit();
        final HelpAwareOptionPaneMocker haMocker = new HelpAwareOptionPaneMocker(
            ImmutableMap.<String, Object>of(
                "<html>Loading of the plugin \"gpsbabelgui\" was requested.<br>" +
                    "This plugin is no longer developed and very likely will produce errors.<br>" +
                    "It should be disabled.<br>Delete from preferences?</html>",
                "Disable plugin"
            )
        );

        List<String> plugins = new ArrayList<>(Arrays.asList("foo", "bar", "gpsbabelgui"));
        PluginHandler.filterUnmaintainedPlugins(MainApplication.getMainFrame(), plugins);
        assertEquals(2, plugins.size());
        assertFalse(plugins.contains("gpsbabelgui"));

        assertEquals(1, haMocker.getInvocationLog().size());
        Object[] invocationLogEntry = haMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Disable plugin", invocationLogEntry[2]);
    }

    /**
     * Unit test of {@link PluginInformationAction} class.
     * @throws PluginException if an error occurs
     */
    @Test
    public void testPluginInformationAction() throws PluginException {
        TestUtils.assumeWorkingJMockit();
        final String expectedText = "Ant-Version: Apache Ant 1.9.6\n" +
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
            "Plugin-Version: 31772\n";
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker() {
            @Override
            public String getStringFromMessage(final Object message) {
                return ((JosmTextArea) ((JScrollPane) message).getViewport().getView()).getText();
            }
        };
        jopsMocker.getMockResultMap().put(expectedText, 0);

        PluginInformationAction action = new PluginInformationAction(PluginPreferenceTest.getDummyPluginInformation());
        assertEquals(expectedText, action.getText());
        action.actionPerformed(null);

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("Plugin information", invocationLogEntry[2]);
    }
}
