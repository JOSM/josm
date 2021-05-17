// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.time.Instant;

/**
 * Unit tests of {@link OsmServerHistoryReader} class.
 */
public class OsmServerHistoryReaderTest {

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()));

    /**
     * Setup tests.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        Config.getPref().put("osm-server.url", wireMockRule.url("/__files/api"));
    }

    /**
     * Tests node history fetching.
     * @throws OsmTransferException if any error occurs
     */
    @Test
    public void testNode() throws OsmTransferException {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(OsmPrimitiveType.NODE, 266187);
        HistoryDataSet ds = reader.parseHistory(NullProgressMonitor.INSTANCE);
        History h = ds.getHistory(266187, OsmPrimitiveType.NODE);
        assertEquals(5, h.getNumVersions());
        assertEquals(1, h.getLatest().getNumKeys());
        assertEquals(65565982, h.getLatest().getChangesetId());
        assertEquals(Instant.ofEpochMilli(1545089885000L), h.getLatest().getInstant());
    }

    /**
     * Tests way history fetching.
     * @throws OsmTransferException if any error occurs
     */
    @Test
    public void testWay() throws OsmTransferException {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(OsmPrimitiveType.WAY, 3058844);
        HistoryDataSet ds = reader.parseHistory(NullProgressMonitor.INSTANCE);
        History h = ds.getHistory(3058844, OsmPrimitiveType.WAY);
        assertEquals(14, h.getNumVersions());
        assertEquals(10, h.getLatest().getNumKeys());
        assertEquals(26368284, h.getLatest().getChangesetId());
        assertEquals(Instant.ofEpochMilli(1414429134000L), h.getLatest().getInstant());
        assertEquals(11, h.getWhichChangedTag(h.getByVersion(14), "bicycle", false).getVersion());
        assertEquals(1, h.getWhichChangedTag(h.getByVersion(10), "bicycle", false).getVersion());
        assertEquals(5, h.getWhichChangedTag(h.getByVersion(14), "created_by", false).getVersion());
        assertEquals(2, h.getWhichChangedTag(h.getByVersion(4), "created_by", false).getVersion());
        assertEquals(1, h.getWhichChangedTag(h.getByVersion(1), "highway", false).getVersion());
    }

    /**
     * Tests relation history fetching.
     * @throws OsmTransferException if any error occurs
     */
    @Test
    public void testRelation() throws OsmTransferException {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(OsmPrimitiveType.RELATION, 49);
        HistoryDataSet ds = reader.parseHistory(NullProgressMonitor.INSTANCE);
        History h = ds.getHistory(49, OsmPrimitiveType.RELATION);
        assertEquals(3, h.getNumVersions());
        assertEquals(0, h.getLatest().getNumKeys());
        assertEquals(486501, h.getLatest().getChangesetId());
        assertEquals(Instant.ofEpochMilli(1194886166000L), h.getLatest().getInstant());
    }
}
