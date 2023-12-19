// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests of {@link JoinAreasAction} class.
 */
@BasicPreferences
@Main
@Projection
class JoinAreasActionTest {
    /**
     * Non-regression test for bug #9599.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    void testTicket9599() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(9599, "ex5.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            Layer layer = new OsmDataLayer(ds, null, null);
            MainApplication.getLayerManager().addLayer(layer);
            try {
                new JoinAreasAction(false).join(ds.getWays());
                Collection<IPrimitive> found = SearchAction.searchAndReturn("type:way", SearchMode.replace);
                assertEquals(1, found.size());
                assertEquals(257786939, found.iterator().next().getUniqueId());
            } finally {
                // Ensure we clean the place before leaving, even if test fails.
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }

    /**
     * Non-regression test for bug #9599.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    void testTicket9599Simple() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(9599, "three_old.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            ds.addDataSource(new DataSource(new Bounds(-90, -180, 90, 180), "Everywhere"));
            Layer layer = new OsmDataLayer(ds, null, null);
            MainApplication.getLayerManager().addLayer(layer);
            try {
                new JoinAreasAction(false).join(ds.getWays());
                Collection<IPrimitive> found = SearchAction.searchAndReturn("type:way", SearchMode.replace);
                assertEquals(1, found.size());
                assertEquals(31567319, found.iterator().next().getUniqueId());
            } finally {
                // Ensure we clean the place before leaving, even if test fails.
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }

    /**
     * Non-regression test for bug #10511.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    void testTicket10511() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(10511, "10511_mini.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            Layer layer = new OsmDataLayer(ds, null, null);
            MainApplication.getLayerManager().addLayer(layer);
            try {
                new JoinAreasAction(false).join(ds.getWays());
                Collection<IPrimitive> found = SearchAction.searchAndReturn("type:way", SearchMode.replace);
                assertEquals(1, found.size());
            } finally {
                // Ensure we clean the place before leaving, even if test fails.
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }

    /**
     * Non-regression test for bug #11992.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    void testTicket11992() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(11992, "shapes.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            assertEquals(10, ds.getWays().size());
            Layer layer = new OsmDataLayer(ds, null, null);
            MainApplication.getLayerManager().addLayer(layer);
            for (String ref : new String[]{"A", "B", "C", "D", "E"}) {
                System.out.print("Joining ways " + ref);
                Collection<IPrimitive> found = SearchAction.searchAndReturn("type:way ref="+ref, SearchMode.replace);
                assertEquals(2, found.size());

                MainApplication.getMenu().joinAreas.join(Utils.filteredCollection(found, Way.class));

                Collection<IPrimitive> found2 = SearchAction.searchAndReturn("type:way ref="+ref, SearchMode.replace);
                assertEquals(1, found2.size());
                System.out.println(" ==> OK");
            }
        }
    }

    /**
     * Non-regression test for bug #18744.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    void testTicket18744() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18744, "18744-sample.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            ds.addDataSource(new DataSource(new Bounds(-90, -180, 90, 180), "Everywhere"));
            Layer layer = new OsmDataLayer(ds, null, null);
            MainApplication.getLayerManager().addLayer(layer);
            try {
                assertEquals(3, ds.getWays().size());
                new JoinAreasAction(false).join(ds.getWays());
                // join should not have changed anything
                assertEquals(3, ds.getWays().size());
            } finally {
                // Ensure we clean the place before leaving, even if test fails.
                MainApplication.getLayerManager().removeLayer(layer);
            }
        }
    }


    /**
     * Non-regression test which checks example files in nodist/data
     * @throws Exception if an error occurs
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void testExamples() throws Exception {
        DataSet dsToJoin, dsExpected;
        try (InputStream is = Files.newInputStream(Paths.get("nodist/data/Join_Areas_Tests.osm"))) {
            dsToJoin = OsmReader.parseDataSet(is, NullProgressMonitor.INSTANCE);
        }
        try (InputStream is = Files.newInputStream(Paths.get("nodist/data/Join_Areas_Tests_joined.osm"))) {
            dsExpected = OsmReader.parseDataSet(is, NullProgressMonitor.INSTANCE);
        }

        // set current edit layer
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(dsToJoin, "join", null));

        Collection<OsmPrimitive> testPrims = dsToJoin.getPrimitives(osm -> osm.get("test") != null);
        MultiMap<String, OsmPrimitive> tests = new MultiMap<>();
        for (OsmPrimitive testPrim : testPrims) {
            tests.put(testPrim.get("test"), testPrim);
        }
        for (String test : tests.keySet()) {
            Collection<OsmPrimitive> primitives = tests.get(test);
            for (OsmPrimitive osm : primitives) {
                assertInstanceOf(Way.class, osm, test + "; expected way, but got: " + osm);
            }
            new JoinAreasAction(false).join((Collection) primitives);
            Collection<OsmPrimitive> joinedCol = dsToJoin.getPrimitives(osm -> !osm.isDeleted() && Objects.equals(osm.get("test"), test));
            assertEquals(1, joinedCol.size(), "in test " + test + ":");
            Collection<OsmPrimitive> expectedCol = dsExpected.getPrimitives(osm -> !osm.isDeleted() && Objects.equals(osm.get("test"), test));
            assertEquals(1, expectedCol.size(), "in test " + test + ":");
            OsmPrimitive osmJoined = joinedCol.iterator().next();
            OsmPrimitive osmExpected = expectedCol.iterator().next();
            assertTrue(isSemanticallyEqual(osmExpected, osmJoined), "difference in test " + test);
        }
    }

    /**
     * Check if 2 primitives are semantically equal as result of a join areas
     * operation.
     * @param osm1 first primitive
     * @param osm2 second primitive
     * @return true if both primitives are semantically equal
     */
    private boolean isSemanticallyEqual(OsmPrimitive osm1, OsmPrimitive osm2) {
        if (osm1 instanceof Node && osm2 instanceof Node)
            return isSemanticallyEqualNode((Node) osm1, (Node) osm2);
        if (osm1 instanceof Way && osm2 instanceof Way)
            return isSemanticallyEqualWay((Way) osm1, (Way) osm2);
        if (osm1 instanceof Relation && osm2 instanceof Relation)
            return isSemanticallyEqualRelation((Relation) osm1, (Relation) osm2);
        return false;
    }

    private boolean isSemanticallyEqualRelation(Relation r1, Relation r2) {
        if (!r1.getKeys().equals(r2.getKeys())) return false;
        if (r1.getMembersCount() != r2.getMembersCount()) return false;
        Set<RelationMember> matchCandidates = new HashSet<>(r2.getMembers());
        for (RelationMember rm : r1.getMembers()) {
            RelationMember matched = matchCandidates.stream()
                    .filter(m -> rm.getRole().equals(m.getRole()))
                    .filter(m -> isSemanticallyEqual(rm.getMember(), m.getMember()))
                    .findFirst().orElse(null);
            if (matched == null) return false;
            matchCandidates.remove(matched);
        }
        return true;
    }

    private boolean isSemanticallyEqualWay(Way w1, Way w2) {
        if (!w1.isClosed() || !w2.isClosed()) throw new UnsupportedOperationException();
        if (!w1.getKeys().equals(w2.getKeys())) return false;
        if (w1.getNodesCount() != w2.getNodesCount()) return false;
        int n = w1.getNodesCount() - 1;
        for (int dir : Arrays.asList(1, -1)) {
            for (int i = 0; i < n; i++) {
                boolean different = false;
                for (int j = 0; j < n; j++) {
                    Node n1 = w1.getNode(j);
                    Node n2 = w2.getNode(Utils.mod(i + dir*j, n));
                    if (!isSemanticallyEqualNode(n1, n2)) {
                        different = true;
                        break;
                    }
                }
                if (!different)
                    return true;
            }
        }
        return false;
    }

    private boolean isSemanticallyEqualNode(Node n1, Node n2) {
        return n1.hasEqualSemanticAttributes(n2);
    }
}
