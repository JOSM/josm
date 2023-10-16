// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link OsmTransferHandler} class.
 */
@BasicPreferences
@Main
@Projection
class OsmTransferHandlerTest {
    private final OsmTransferHandler transferHandler = new OsmTransferHandler();

    /**
     * Test of {@link OsmTransferHandler#pasteOn} method
     */
    @Test
    void testPasteOn() {
        DataSet ds1 = new DataSet();
        Node n1 = new Node(new LatLon(43, 1));
        ds1.addPrimitive(n1);
        OsmDataLayer source = new OsmDataLayer(ds1, "source", null);

        CopyAction.copy(source, Collections.singleton(n1));

        DataSet ds2 = new DataSet();
        OsmDataLayer target = new OsmDataLayer(ds2, "target", null);

        transferHandler.pasteOn(target, null);
        assertTrue(n1.equalsEpsilon(ds2.getNodes().iterator().next()));

        ds2.clear();
        assertTrue(ds2.getNodes().isEmpty());

        LatLon pos = new LatLon(55, -5);
        transferHandler.pasteOn(target, ProjectionRegistry.getProjection().latlon2eastNorth(pos));
        assertTrue(pos.equalsEpsilon(ds2.getNodes().iterator().next()));
    }

    /**
     * Test of {@link OsmTransferHandler#pasteTags} method
     */
    @Test
    void testPasteTags() {
        Node n = new Node(LatLon.ZERO);
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(n), "testPasteTags", null));

        ClipboardUtils.copyString("test=ok");
        transferHandler.pasteTags(Collections.singleton(n));

        assertEquals("ok", n.get("test"));
    }

    static Stream<Arguments> testNonRegression21324() {
        return Stream.of(
                Arguments.of((Runnable) () -> ClipboardUtils.copyString("test=ok\rsomething=else\nnew=line")),
                Arguments.of((Runnable) () -> {
                    Node nData = new Node();
                    nData.put("test", "ok");
                    nData.put("something", "else");
                    nData.put("new", "line");
                    ClipboardUtils.copy(new PrimitiveTransferable(
                            PrimitiveTransferData.getDataWithReferences(Collections.singletonList(nData)), null));
                })
        );
    }

    /**
     * Non-regression test for #21324: Command stack says "pasting 1 tag to [number] objects" regardless of how many tags are pasted
     */
    @ParameterizedTest
    @MethodSource
    void testNonRegression21324(Runnable clipboardCopy) {
        clipboardCopy.run();
        Node n = new Node(LatLon.ZERO);
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(n), "testNonRegression21324", null));

        transferHandler.pasteTags(Collections.singleton(n));
        Command command = UndoRedoHandler.getInstance().getLastCommand();

        assertAll(() -> assertEquals("ok", n.get("test")),
                () -> assertEquals("else", n.get("something")),
                () -> assertEquals("line", n.get("new")),
                () -> assertEquals(1, UndoRedoHandler.getInstance().getUndoCommands().size()),
                () -> assertTrue((command instanceof SequenceCommand
                        && command.getChildren().stream().allMatch(ChangePropertyCommand.class::isInstance))
                || command instanceof ChangePropertyCommand),
                () -> assertEquals("Sequence: Pasting 3 tags to 1 object", command.getDescriptionText()));
    }
}
