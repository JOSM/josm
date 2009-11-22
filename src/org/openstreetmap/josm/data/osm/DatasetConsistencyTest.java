// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * This class can be used to run consistency tests on dataset. Any errors found will be written to provided PrintWriter
 * <br>
 * Texts here should not be translated because they're not intended for users but for josm developers
 *
 */
public class DatasetConsistencyTest {

    private final DataSet dataSet;
    private final PrintWriter writer;

    public DatasetConsistencyTest(DataSet dataSet, Writer writer) {
        this.dataSet = dataSet;
        this.writer = new PrintWriter(writer);
    }

    private void checkReferrers() {
        for (Way way:dataSet.getWays()) {
            for (Node n:way.getNodes()) {
                if (!n.getReferrers().contains(way)) {
                    writer.println(String.format("%s is part of %s but is not in referrers", n, way));
                }
            }
        }

        for (Relation relation:dataSet.getRelations()) {
            for (RelationMember m:relation.getMembers()) {
                if (!m.getMember().getReferrers().contains(relation)) {
                    writer.println(String.format("%s is part of %s but is not in referrers", m.getMember(), relation));
                }
            }
        }
    }

    private void checkCompleteWaysWithIncompleteNodes() {
        for (Way way:dataSet.getWays()) {
            if (!way.incomplete) {
                for (Node node:way.getNodes()) {
                    if (node.incomplete) {
                        writer.println(String.format("%s is complete but contains incomplete node '%s'", way, node));
                    }
                }
            }
        }
    }

    private void checkCompleteNodesWithoutCoordinates() {
        for (Node node:dataSet.getNodes()) {
            if (!node.incomplete && (node.getCoor() == null || node.getEastNorth() == null)) {
                writer.println(String.format("%s is not incomplete but has null coordinates", node));
            }
        }
    }

    private void searchNodes() {
        for (Node n:dataSet.getNodes()) {
            if (!n.incomplete) {
                LatLon c = n.getCoor();
                BBox box = new BBox(new LatLon(c.lat() - 0.0001, c.lon() - 0.0001), new LatLon(c.lat() + 0.0001, c.lon() + 0.0001));
                if (!dataSet.searchNodes(box).contains(n)) {
                    writer.println(String.format("%s not found using Dataset.searchNodes()", n));
                }
            }
        }
    }

    private void searchWays() {
        for (Way w:dataSet.getWays()) {
            if (!w.incomplete && !dataSet.searchWays(w.getBBox()).contains(w)) {
                writer.println(String.format("%s not found using Dataset.searchWays()", w));
            }
        }
    }

    private void checkReferredPrimitive(OsmPrimitive primitive, OsmPrimitive parent) {
        if (dataSet.getPrimitiveById(primitive) == null) {
            writer.println(String.format("%s is referenced by %s but not found in dataset", primitive, parent));
        }
        if (dataSet.getPrimitiveById(primitive) != primitive) {
            writer.println(String.format("%s is different instance that reffered by %s", primitive, parent));
        }
        if (primitive.isDeleted()) {
            writer.println(String.format("%s reffers to deleted primitive %s", parent, primitive));
        }
    }

    private void refferedPrimitiveNotInDataset() {
        for (Way way:dataSet.getWays()) {
            for (Node node:way.getNodes()) {
                checkReferredPrimitive(node, way);
            }
        }

        for (Relation relation:dataSet.getRelations()) {
            for (RelationMember member:relation.getMembers()) {
                checkReferredPrimitive(member.getMember(), relation);
            }
        }
    }

    public void runTest() {
        try {
            checkReferrers();
            checkCompleteWaysWithIncompleteNodes();
            checkCompleteNodesWithoutCoordinates();
            searchNodes();
            searchWays();
            refferedPrimitiveNotInDataset();
        } catch (Exception e) {
            writer.println("Exception during dataset integrity test:");
            e.printStackTrace(writer);
        }
    }

    public static String runTests(DataSet dataSet) {
        StringWriter writer = new StringWriter();
        new DatasetConsistencyTest(dataSet, writer).runTest();
        return writer.toString();
    }

}
