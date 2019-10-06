// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SimplifyWayAction}.
 */
public final class SimplifyWayActionTest {

    /** Class under test. */
    private static SimplifyWayAction action;

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main();

    /**
     * Setup test.
     */
    @Before
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
     * @throws IOException
     * @throws IllegalDataException
     */
    @Test
    public void testSimplify() throws IllegalDataException, IOException {
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
    public void testSimplifyFirstNode() {
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
}
