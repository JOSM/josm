// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.swing.UIManager;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main.DownloadParamType;
import org.openstreetmap.josm.Main.MasterWindowListener;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Main} class.
 */
public class MainTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform();

    /**
     * Unit test of {@link DownloadParamType#paramType} method.
     */
    @Test
    public void testParamType() {
        assertEquals(DownloadParamType.bounds, DownloadParamType.paramType("48.000,16.000,48.001,16.001"));
        assertEquals(DownloadParamType.fileName, DownloadParamType.paramType("data.osm"));
        assertEquals(DownloadParamType.fileUrl, DownloadParamType.paramType("file:///home/foo/data.osm"));
        assertEquals(DownloadParamType.fileUrl, DownloadParamType.paramType("file://C:\\Users\\foo\\data.osm"));
        assertEquals(DownloadParamType.httpUrl, DownloadParamType.paramType("http://somewhere.com/data.osm"));
        assertEquals(DownloadParamType.httpUrl, DownloadParamType.paramType("https://somewhere.com/data.osm"));
    }

    /**
     * Unit tests on log messages.
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void testLogs() {

        assertNull(Main.getErrorMessage(null));

        // Correct behaviour with errors
        Main.error(new Exception("exception_error"));
        Main.error("Error message on one line");
        Main.error("First line of error message on several lines\nline2\nline3\nline4");
        Collection<String> errors = Main.getLastErrorAndWarnings();
        assertTrue(errors.contains("E: java.lang.Exception: exception_error"));
        assertTrue(errors.contains("E: Error message on one line"));
        assertTrue(errors.contains("E: First line of error message on several lines"));

        // Correct behaviour with warnings
        Main.warn(new Exception("exception_warn", new Exception("root_cause")));
        Main.warn("Warning message on one line");
        Main.warn("First line of warning message on several lines\nline2\nline3\nline4");
        Collection<String> warnings = Main.getLastErrorAndWarnings();
        assertTrue(warnings.contains("W: java.lang.Exception: exception_warn. Cause: java.lang.Exception: root_cause"));
        assertTrue(warnings.contains("W: Warning message on one line"));
        assertTrue(warnings.contains("W: First line of warning message on several lines"));
    }

    /**
     * Unit test of {@link Main#preConstructorInit}.
     */
    @Test
    public void testPreConstructorInit() {
        Main.preConstructorInit();
        assertNotNull(Main.getProjection());
        assertEquals(Main.pref.get("laf", Main.platform.getDefaultStyle()), UIManager.getLookAndFeel().getClass().getCanonicalName());
        assertNotNull(Main.toolbar);
    }

    /**
     * Unit test of {@link Main.MasterWindowListener}.
     */
    @Test
    public void testMasterWindowListener() {
        MasterWindowListener.setup();
        MasterWindowListener.teardown();
        assertNotNull(MasterWindowListener.getInstance());
    }
}
