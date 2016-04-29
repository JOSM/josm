// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertNotNull;

import java.awt.Component;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.io.ChangesetQuery;

/**
 * Unit tests for class {@link ChangesetQueryTask}.
 */
public class ChangesetQueryTaskTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask}.
     */
    @Test
    public void testChangesetQueryTask() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(new ChangesetQueryTask(parent, new ChangesetQuery()));
    }

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask} - null parent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChangesetQueryTaskNullParent() {
        new ChangesetQueryTask(new ChangesetQuery());
    }
}
