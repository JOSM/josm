// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PluginPreference} class.
 */
public class PluginPreferenceTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().assertionsInEDT();

    /**
     * Unit test of {@link PluginPreference#PluginPreference}.
     */
    @Test
    public void testPluginPreference() {
        assertNotNull(new PluginPreference.Factory().createPreferenceSetting());
    }

    /**
     * Returns a dummy plugin information.
     * @return a dummy plugin information
     * @throws PluginException if an error occurs
     */
    public static PluginInformation getDummyPluginInformation() throws PluginException {
        return new PluginInformation(
                new File(TestUtils.getTestDataRoot() + "__files/plugin/dummy_plugin.v31772.jar"), "dummy_plugin");
    }

    /**
     * Unit test of {@link PluginPreference#buildDownloadSummary}.
     * @throws Exception if an error occurs
     */
    @Test
    public void testBuildDownloadSummary() throws Exception {
        final PluginInformation dummy = getDummyPluginInformation();
        assertEquals("", PluginPreference.buildDownloadSummary(
                new PluginDownloadTask(NullProgressMonitor.INSTANCE, Collections.<PluginInformation>emptyList(), "")));
        assertEquals("", PluginPreference.buildDownloadSummary(
                new PluginDownloadTask(NullProgressMonitor.INSTANCE, Arrays.asList(dummy), "")));
        assertEquals("The following plugin has been downloaded <strong>successfully</strong>:<ul><li>dummy_plugin (31772)</li></ul>"+
                     "Downloading the following plugin has <strong>failed</strong>:<ul><li>dummy_plugin</li></ul>"+
                     "<br>Error message(untranslated): test",
                PluginPreference.buildDownloadSummary(
                        new PluginDownloadTask(NullProgressMonitor.INSTANCE, Arrays.asList(dummy), "") {
                    @Override
                    public Collection<PluginInformation> getFailedPlugins() {
                        return Collections.singleton(dummy);
                    }

                    @Override
                    public Collection<PluginInformation> getDownloadedPlugins() {
                        return Collections.singleton(dummy);
                    }

                    @Override
                    public Exception getLastException() {
                        return new Exception("test");
                    }
                }));
    }

    /**
     * Unit test of {@link PluginPreference#notifyDownloadResults}.
     */
    @Test
    public void testNotifyDownloadResults() {
        final HelpAwareOptionPaneMocker mocker = new HelpAwareOptionPaneMocker();
        mocker.getMockResultMap().put("<html></html>", "OK");  // (buildDownloadSummary() output was empty)
        mocker.getMockResultMap().put("<html>Please restart JOSM to activate the downloaded plugins.</html>", "OK");

        PluginDownloadTask task = new PluginDownloadTask(NullProgressMonitor.INSTANCE, Collections.<PluginInformation>emptyList(), "");
        PluginPreference.notifyDownloadResults(null, task, false);
        PluginPreference.notifyDownloadResults(null, task, true);
    }

    /**
     * Unit test of {@link PluginPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new PluginPreference.Factory(), null);
    }
}
