// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.bugreport.BugReport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the {@link BugReportSettingsPanel} class.
 */
public class BugReportSettingsPanelTest {

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link BugReportSettingsPanel}
     */
    @Test
    public void testBugReportSettingsPanel() {
        assertNotNull(new BugReportSettingsPanel(new BugReport(BugReport.intercept(new Exception()))));
    }
}
