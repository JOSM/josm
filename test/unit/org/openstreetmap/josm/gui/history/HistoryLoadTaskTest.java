// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmHistoryReader;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link HistoryLoadTask} class.
 */
public class HistoryLoadTaskTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().devAPI().timeout(20000);

    /**
     * Unit test of {@link HistoryLoadTask#getLoadingMessage}
     */
    @Test
    public void testGetLoadingMessage() {
        assertEquals("Loading history for node {0}", HistoryLoadTask.getLoadingMessage(new Node().getPrimitiveId()));
        assertEquals("Loading history for way {0}", HistoryLoadTask.getLoadingMessage(new Way().getPrimitiveId()));
        assertEquals("Loading history for relation {0}", HistoryLoadTask.getLoadingMessage(new Relation().getPrimitiveId()));

        assertEquals("", HistoryLoadTask.getLoadingMessage(new SimplePrimitiveId(1, OsmPrimitiveType.CLOSEDWAY)));
        assertEquals("", HistoryLoadTask.getLoadingMessage(new SimplePrimitiveId(1, OsmPrimitiveType.MULTIPOLYGON)));
    }

    /**
     * Unit test of {@link HistoryLoadTask#loadHistory}
     * @throws OsmTransferException if an error occurs
     */
    @Test
    public void testLoadHistory() throws OsmTransferException {
        HistoryDataSet ds = HistoryLoadTask.loadHistory(new OsmServerHistoryReader(OsmPrimitiveType.NODE, 0) {
            @Override
            public HistoryDataSet parseHistory(ProgressMonitor progressMonitor) throws OsmTransferException {
                try (InputStream in = TestUtils.getRegressionDataStream(12639, "history.xml")) {
                    return new OsmHistoryReader(in).parse(NullProgressMonitor.INSTANCE);
                } catch (IOException | SAXException e) {
                    throw new OsmTransferException(e);
                }
            }
        }, NullProgressMonitor.INSTANCE);
        assertEquals(113, ds.getChangesetIds().size());
        History h = ds.getHistory(1350901, OsmPrimitiveType.RELATION);
        assertEquals(115, h.getNumVersions());
    }
}
