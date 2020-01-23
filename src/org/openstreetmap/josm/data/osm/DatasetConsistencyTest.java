// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Stopwatch;

/**
 * This class can be used to run consistency tests on dataset. Any errors found will be written to provided PrintWriter.
 * <br>
 * Texts here should not be translated because they're not intended for users but for josm developers.
 * @since 2500
 */
public class DatasetConsistencyTest {

    private static final int MAX_ERRORS = 100;
    private final DataSet dataSet;
    private final PrintWriter writer;
    private int errorCount;

    /**
     * Constructs a new {@code DatasetConsistencyTest}.
     * @param dataSet The dataset to test
     * @param writer The writer used to write results
     */
    public DatasetConsistencyTest(DataSet dataSet, Writer writer) {
        this.dataSet = dataSet;
        this.writer = new PrintWriter(writer);
    }

    private void printError(String type, String message, Object... args) {
        errorCount++;
        if (errorCount <= MAX_ERRORS) {
            writer.println('[' + type + "] " + String.format(message, args));
        }
    }

    /**
     * Checks that parent primitive is referred from its child members
     */
    public void checkReferrers() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        // It's also error when referred primitive's dataset is null but it's already covered by referredPrimitiveNotInDataset check
        for (Way way : dataSet.getWays()) {
            if (!way.isDeleted()) {
                for (Node n : way.getNodes()) {
                    if (n.getDataSet() != null && !n.getReferrers().contains(way)) {
                        printError("WAY NOT IN REFERRERS", "%s is part of %s but is not in referrers", n, way);
                    }
                }
            }
        }

        for (Relation relation : dataSet.getRelations()) {
            if (!relation.isDeleted()) {
                for (RelationMember m : relation.getMembers()) {
                    if (m.getMember().getDataSet() != null && !m.getMember().getReferrers().contains(relation)) {
                        printError("RELATION NOT IN REFERRERS", "%s is part of %s but is not in referrers", m.getMember(), relation);
                    }
                }
            }
        }
        printElapsedTime(stopwatch);
    }

    /**
     * Checks for womplete ways with incomplete nodes.
     */
    public void checkCompleteWaysWithIncompleteNodes() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (Way way : dataSet.getWays()) {
            if (way.isUsable()) {
                for (Node node : way.getNodes()) {
                    if (node.isIncomplete()) {
                        printError("USABLE HAS INCOMPLETE", "%s is usable but contains incomplete node '%s'", way, node);
                    }
                }
            }
        }
        printElapsedTime(stopwatch);
    }

    /**
     * Checks for complete nodes without coordinates.
     */
    public void checkCompleteNodesWithoutCoordinates() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (Node node : dataSet.getNodes()) {
            if (!node.isIncomplete() && node.isVisible() && !node.isLatLonKnown()) {
                printError("COMPLETE WITHOUT COORDINATES", "%s is not incomplete but has null coordinates", node);
            }
        }
        printElapsedTime(stopwatch);
    }

    /**
     * Checks that nodes can be retrieved through their coordinates.
     */
    public void searchNodes() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        dataSet.getReadLock().lock();
        try {
            for (Node n : dataSet.getNodes()) {
                // Call isDrawable() as an efficient replacement to previous checks (!deleted, !incomplete, getCoor() != null)
                if (n.isDrawable() && !dataSet.containsNode(n)) {
                    printError("SEARCH NODES", "%s not found using Dataset.containsNode()", n);
                }
            }
        } finally {
            dataSet.getReadLock().unlock();
        }
        printElapsedTime(stopwatch);
    }

    /**
     * Checks that ways can be retrieved through their bounding box.
     */
    public void searchWays() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        dataSet.getReadLock().lock();
        try {
            for (Way w : dataSet.getWays()) {
                if (!w.isIncomplete() && !w.isDeleted() && w.getNodesCount() >= 2 && !dataSet.containsWay(w)) {
                    printError("SEARCH WAYS", "%s not found using Dataset.containsWay()", w);
                }
            }
        } finally {
            dataSet.getReadLock().unlock();
        }
        printElapsedTime(stopwatch);
    }

    private void checkReferredPrimitive(OsmPrimitive primitive, OsmPrimitive parent) {
        if (primitive.getDataSet() == null) {
            printError("NO DATASET", "%s is referenced by %s but not found in dataset", primitive, parent);
        } else if (dataSet.getPrimitiveById(primitive) == null) {
            printError("REFERENCED BUT NOT IN DATA", "%s is referenced by %s but not found in dataset", primitive, parent);
        } else if (dataSet.getPrimitiveById(primitive) != primitive) {
            printError("DIFFERENT INSTANCE", "%s is different instance that referred by %s", primitive, parent);
        }

        if (primitive.isDeleted()) {
            printError("DELETED REFERENCED", "%s refers to deleted primitive %s", parent, primitive);
        }
    }

    /**
     * Checks that referred primitives are present in dataset.
     */
    public void referredPrimitiveNotInDataset() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (Way way : dataSet.getWays()) {
            for (Node node : way.getNodes()) {
                checkReferredPrimitive(node, way);
            }
        }

        for (Relation relation : dataSet.getRelations()) {
            for (RelationMember member : relation.getMembers()) {
                checkReferredPrimitive(member.getMember(), relation);
            }
        }
        printElapsedTime(stopwatch);
    }

    /**
     * Checks for zero and one-node ways.
     */
    public void checkZeroNodesWays() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (Way way : dataSet.getWays()) {
            if (way.isUsable() && way.getNodesCount() == 0) {
                printError("WARN - ZERO NODES", "Way %s has zero nodes", way);
            } else if (way.isUsable() && way.getNodesCount() == 1) {
                printError("WARN - NO NODES", "Way %s has only one node", way);
            }
        }
        printElapsedTime(stopwatch);
    }

    private void printElapsedTime(Stopwatch stopwatch) {
        if (Logging.isDebugEnabled()) {
            StackTraceElement item = Thread.currentThread().getStackTrace()[2];
            String operation = getClass().getSimpleName() + '.' + item.getMethodName();
            Logging.debug(tr("Test ''{0}'' completed in {1}",
                    operation, stopwatch));
        }
    }

    /**
     * Runs test.
     */
    public void runTest() {
        try {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            referredPrimitiveNotInDataset();
            checkReferrers();
            checkCompleteWaysWithIncompleteNodes();
            checkCompleteNodesWithoutCoordinates();
            searchNodes();
            searchWays();
            checkZeroNodesWays();
            printElapsedTime(stopwatch);
            if (errorCount > MAX_ERRORS) {
                writer.println((errorCount - MAX_ERRORS) + " more...");
            }

        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            writer.println("Exception during dataset integrity test:");
            e.printStackTrace(writer);
            Logging.warn(e);
        }
    }

    /**
     * Runs test on the given dataset.
     * @param dataSet the dataset to test
     * @return the errors as string
     */
    public static String runTests(DataSet dataSet) {
        StringWriter writer = new StringWriter();
        new DatasetConsistencyTest(dataSet, writer).runTest();
        return writer.toString();
    }
}
