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

    private static final int MAX_ERRORS = 100;
    private final DataSet dataSet;
    private final PrintWriter writer;
    private int errorCount;

    public DatasetConsistencyTest(DataSet dataSet, Writer writer) {
        this.dataSet = dataSet;
        this.writer = new PrintWriter(writer);
    }

    private void printError(String type, String message, Object... args) {
        errorCount++;
        if (errorCount <= MAX_ERRORS) {
            writer.println("[" + type + "] " + String.format(message, args));
        }
    }

    public void checkReferrers() {
        // It's also error when referred primitive's dataset is null but it's already covered by referredPrimitiveNotInDataset check
        for (Way way:dataSet.getWays()) {
            if (!way.isDeleted()) {
                for (Node n:way.getNodes()) {
                    if (n.getDataSet() != null && !n.getReferrers().contains(way)) {
                        printError("WAY NOT IN REFERRERS", "%s is part of %s but is not in referrers", n, way);
                    }
                }
            }
        }

        for (Relation relation:dataSet.getRelations()) {
            if (!relation.isDeleted()) {
                for (RelationMember m:relation.getMembers()) {
                    if (m.getMember().getDataSet() != null && !m.getMember().getReferrers().contains(relation)) {
                        printError("RELATION NOT IN REFERRERS", "%s is part of %s but is not in referrers", m.getMember(), relation);
                    }
                }
            }
        }
    }

    public void checkCompleteWaysWithIncompleteNodes() {
        for (Way way:dataSet.getWays()) {
            if (way.isUsable()) {
                for (Node node:way.getNodes()) {
                    if (node.isIncomplete()) {
                        printError("USABLE HAS INCOMPLETE", "%s is usable but contains incomplete node '%s'", way, node);
                    }
                }
            }
        }
    }

    public void checkCompleteNodesWithoutCoordinates() {
        for (Node node:dataSet.getNodes()) {
            if (!node.isIncomplete() && node.isVisible() && (node.getCoor() == null || node.getEastNorth() == null)) {
                printError("COMPLETE WITHOUT COORDINATES", "%s is not incomplete but has null coordinates", node);
            }
        }
    }

    public void searchNodes() {
        for (Node n:dataSet.getNodes()) {
            if (!n.isIncomplete() && !n.isDeleted()) {
                LatLon c = n.getCoor();
                if (c != null) {
                    BBox box = c.toBBox(0.0001);
                    if (!dataSet.searchNodes(box).contains(n)) {
                        printError("SEARCH NODES", "%s not found using Dataset.searchNodes()", n);
                    }
                }
            }
        }
    }

    public void searchWays() {
        for (Way w:dataSet.getWays()) {
            if (!w.isIncomplete() && !w.isDeleted() && w.getNodesCount() >= 2 && !dataSet.searchWays(w.getBBox()).contains(w)) {
                printError("SEARCH WAYS", "%s not found using Dataset.searchWays()", w);
            }
        }
    }

    private void checkReferredPrimitive(OsmPrimitive primitive, OsmPrimitive parent) {
        if (primitive.getDataSet() == null) {
            printError("NO DATASET", "%s is referenced by %s but not found in dataset", primitive, parent);
        } else if (dataSet.getPrimitiveById(primitive) == null) {
            printError("REFERENCED BUT NOT IN DATA", "%s is referenced by %s but not found in dataset", primitive, parent);
        } else  if (dataSet.getPrimitiveById(primitive) != primitive) {
            printError("DIFFERENT INSTANCE", "%s is different instance that referred by %s", primitive, parent);
        }

        if (primitive.isDeleted()) {
            printError("DELETED REFERENCED", "%s refers to deleted primitive %s", parent, primitive);
        }
    }

    public void referredPrimitiveNotInDataset() {
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


    public void checkZeroNodesWays() {
        for (Way way:dataSet.getWays()) {
            if (way.isUsable() && way.getNodesCount() == 0) {
                printError("WARN - ZERO NODES", "Way %s has zero nodes", way);
            } else if (way.isUsable() && way.getNodesCount() == 1) {
                printError("WARN - NO NODES", "Way %s has only one node", way);
            }
        }
    }

    public void runTest() {
        try {
            referredPrimitiveNotInDataset();
            checkReferrers();
            checkCompleteWaysWithIncompleteNodes();
            checkCompleteNodesWithoutCoordinates();
            searchNodes();
            searchWays();
            checkZeroNodesWays();
            if (errorCount > MAX_ERRORS) {
                writer.println((errorCount - MAX_ERRORS) + " more...");
            }
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
