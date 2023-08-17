// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Test class for {@link SelectSharedChildObjectsAction}
 */
class SelectSharedChildObjectsActionTest {
    private static SelectSharedChildObjectsAction action;
    private DataSet ds;

    @BeforeAll
    static void classSetup() {
        action = new SelectSharedChildObjectsAction();
    }

    @BeforeEach
    void setup() {
        ds = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "SelectSharedChildObjectsActionTest", null));
    }

    @Test
    void testNoIntersection() {
        Way way1 = TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""), TestUtils.newNode(""));
        Way way2 = TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""), TestUtils.newNode(""));
        ds.addPrimitiveRecursive(way1);
        ds.addPrimitiveRecursive(way2);
        ds.setSelected(way1, way2);
        assertAll("Sanity check that the current selection code works before we check the selection action",
                () -> assertEquals(2, ds.getSelected().size()),
                () -> assertTrue(ds.getSelected().contains(way1)),
                () -> assertTrue(ds.getSelected().contains(way2)));

        action.actionPerformed(null);
        assertEquals(0, ds.getSelected().size(), "Nothing should be selected");
    }

    @Test
    void testBasicIntersection() {
        Way way1 = TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""), TestUtils.newNode(""));
        Way way2 = TestUtils.newWay("", TestUtils.newNode(""), TestUtils.newNode(""));
        ds.addPrimitiveRecursive(way1);
        ds.addPrimitiveRecursive(way2);
        way2.addNode(1, way1.getNode(1));
        ds.setSelected(way1, way2);
        assertAll("Sanity check that the current selection code works before we check the selection action",
                () -> assertEquals(2, ds.getSelected().size()),
                () -> assertTrue(ds.getSelected().contains(way1)),
                () -> assertTrue(ds.getSelected().contains(way2)));

        action.actionPerformed(null);
        assertAll("Check that the selected object is the common node",
                () -> assertEquals(1, ds.getSelected().size()),
                () -> assertSame(way1.getNode(1), ds.getSelected().iterator().next()));
    }
}
