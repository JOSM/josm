// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests for class {@link DownloadTaskList}.
 */
public class DownloadTaskListTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@code DownloadTaskList#DownloadTaskList}.
     */
    @Test
    public void testDownloadTaskList() {
        assertTrue(new DownloadTaskList().getDownloadedPrimitives().isEmpty());
    }
}
