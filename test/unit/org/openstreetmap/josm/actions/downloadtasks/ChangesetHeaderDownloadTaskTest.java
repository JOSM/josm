// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertNotNull;

import java.awt.Component;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Changeset;

/**
 * Unit tests for class {@link ChangesetHeaderDownloadTask}.
 */
public class ChangesetHeaderDownloadTaskTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@code ChangesetHeaderDownloadTask#buildTaskForChangesets}.
     */
    @Test
    public void testBuildTaskForChangesets() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, Collections.singleton(new Changeset(1))));
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, Collections.<Changeset>singleton(null)));
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, null));
    }

    /**
     * Unit test of {@code ChangesetHeaderDownloadTask#buildTaskForChangesets} - null parent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildTaskForChangesetsNullParent() {
        ChangesetHeaderDownloadTask.buildTaskForChangesets(Collections.singleton(new Changeset(1)));
    }
}
