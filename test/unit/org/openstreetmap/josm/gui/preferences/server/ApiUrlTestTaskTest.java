// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;

import javax.swing.JLabel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ApiUrlTestTask} class.
 */
public class ApiUrlTestTaskTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(30000);

    private static final Component PARENT = new JLabel();

    /**
     * Unit test of {@link ApiUrlTestTask#ApiUrlTestTask} - null url.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullApiUrl() {
        new ApiUrlTestTask(PARENT, null);
    }

    /**
     * Unit test of {@link ApiUrlTestTask} - nominal url.
     */
    @Test
    public void testNominalUrl() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, Config.getUrls().getDefaultOsmApiUrl());
        task.run();
        assertTrue(task.isSuccess());
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidUrl} - malformed url.
     */
    @Test
    public void testAlertInvalidUrl() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, "malformed url");
        task.run();
        assertFalse(task.isSuccess());
    }

    /**
     * Unit test of {@link ApiUrlTestTask} - unknown host.
     */
    @Test
    public void testUnknownHost() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, "http://unknown");
        task.run();
        assertFalse(task.isSuccess());
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidServerResult} - http 404.
     */
    @Test
    public void testAlertInvalidServerResult() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, "http://www.openstreetmap.org");
        task.run();
        assertFalse(task.isSuccess());
    }

    /**
     * Unit test of {@link ApiUrlTestTask#alertInvalidCapabilities} - invalid contents.
     */
    @Test
    public void testAlertInvalidCapabilities() {
        ApiUrlTestTask task = new ApiUrlTestTask(PARENT, "https://josm.openstreetmap.de/export/10979/josm/trunk/test/data/invalid_api");
        task.run();
        assertFalse(task.isSuccess());
    }
}
