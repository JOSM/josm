// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.mockers.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.mockers.HelpAwareOptionPaneMocker;
import org.openstreetmap.josm.tools.Utils;

/**
 * Unit tests for class {@link SimplifyWayAction}.
 */
@Main
@Projection
final class SimplifyWayActionTest {

    /** Class under test. */
    private static SimplifyWayAction action;

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        if (action == null) {
            action = MainApplication.getMenu().simplifyWay;
            action.setEnabled(true);
        }
    }

    private DataSet getDs(String file) throws IllegalDataException, IOException {
        return OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "tracks/" + file + ".osm")), null);
    }

    /**
     * Tests simplification
     * @throws Exception in case of error
     */
    @Test
    void testSimplify() throws Exception {
        DataSet DsSimplify = getDs("tracks");
        DataSet DsExpected = getDs("tracks-simplify15");
        SimplifyWayAction.simplifyWays(new ArrayList<>(DsSimplify.getWays()), 15);
        DsSimplify.cleanupDeletedPrimitives();
        //compare sorted Coordinates and total amount of primitives, because IDs and order will vary after reload
        List<LatLon> CoorSimplify = DsSimplify.getNodes().stream()
                .map(Node::getCoor)
                .sorted(Comparator.comparing(LatLon::hashCode))
                .collect(Collectors.toList());
        List<LatLon> CoorExpected = DsExpected.getNodes().stream()
                .map(Node::getCoor)
                .sorted(Comparator.comparing(LatLon::hashCode))
                .collect(Collectors.toList());
        assertEquals(CoorExpected, CoorSimplify);
        assertEquals(DsExpected.allPrimitives().size(), DsSimplify.allPrimitives().size());
    }

    /**
     * Tests that also the first node may be simplified, see #13094.
     */
    @Test
    void testSimplifyFirstNode() {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(new LatLon(47.26269614984, 11.34044231149));
        final Node n2 = new Node(new LatLon(47.26274590831, 11.34053120859));
        final Node n3 = new Node(new LatLon(47.26276562382, 11.34034715039));
        final Node n4 = new Node(new LatLon(47.26264639132, 11.34035341438));
        final Way w = new Way();
        Stream.of(n1, n2, n3, n4, w).forEach(ds::addPrimitive);
        Stream.of(n1, n2, n3, n4, n1).forEach(w::addNode);
        final SequenceCommand command = SimplifyWayAction.createSimplifyCommand(w, 3);
        assertNotNull(command);
        assertEquals(2, command.getChildren().size());
        final Collection<DeleteCommand> deleteCommands = Utils.filteredCollection(command.getChildren(), DeleteCommand.class);
        assertEquals(1, deleteCommands.size());
        assertEquals(Collections.singleton(n1), deleteCommands.iterator().next().getParticipatingPrimitives());
    }

    /**
     * Non-regression test for #23399
     */
    @Test
    void testNonRegression23399() {
        TestUtils.assumeWorkingJMockit();
        new ExtendedDialogMocker(Collections.singletonMap("Simplify way", "Simplify")) {
            @Override
            protected String getString(ExtendedDialog instance) {
                return instance.getTitle();
            }
        };
        new HelpAwareOptionPaneMocker(Collections.singletonMap(
                tr("The selection contains {0} ways. Are you sure you want to simplify them all?", 1000), "Yes"));
        final ArrayList<Way> ways = new ArrayList<>(1000);
        final DataSet ds = new DataSet();
        for (int i = 0; i < 1000; i++) {
            final Way way = TestUtils.newWay("", new Node(new LatLon(0, 0)), new Node(new LatLon(0, 0.001)),
                    new Node(new LatLon(0, 0.002)));
            ways.add(way);
            ds.addPrimitiveRecursive(way);
        }
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "SimplifyWayActionTest#testNonRegression23399", null));
        GuiHelper.runInEDTAndWait(() -> ds.setSelected(ds.allPrimitives()));
        assertEquals(ds.allPrimitives().size(), ds.getAllSelected().size());
        assertDoesNotThrow(() -> GuiHelper.runInEDTAndWaitWithException(() -> action.actionPerformed(null)));
        assertAll(ways.stream().map(way -> () -> assertEquals(2, way.getNodesCount())));
        assertAll(ds.getAllSelected().stream().map(p -> () -> assertFalse(p.isDeleted())));
        assertEquals(3000, ds.getAllSelected().size());
    }
}
