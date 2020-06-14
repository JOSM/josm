// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.CustomMatchers.hasSize;
import static org.CustomMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@code UntaggedNode} class.
 */
public class UntaggedNodeTest {

    private final UntaggedNode test = new UntaggedNode();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules();

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12436">Bug #12436</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket12436() throws Exception {
        test.initialize();
        test.startTest(null);
        try (InputStream fis = TestUtils.getRegressionDataStream(12436, "example.osm")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            test.visit(ds.allPrimitives());
            test.endTest();
            assertThat(test.getErrors(), isEmpty());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12464">Bug #12464</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket12464() throws Exception {
        test.initialize();
        test.startTest(null);
        try (InputStream fis = TestUtils.getRegressionDataStream(12464, "example.osm")) {
            final DataSet ds = OsmReader.parseDataSet(fis, NullProgressMonitor.INSTANCE);
            test.visit(ds.allPrimitives());
            test.endTest();
            assertThat(test.getErrors(), hasSize(1));
        }
    }
}
