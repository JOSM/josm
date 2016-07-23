// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.gui.datatransfer.RelationMemberTransferable.RELATION_MEMBER_DATA;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Collection;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;

/**
 * Unit tests of {@link RelationMemberTransferable} class.
 */
public class RelationMemberTransferableTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferDataFlavors()} method.
     */
    @Test
    public void testGetTransferDataFlavors() {
        DataFlavor[] flavors = new RelationMemberTransferable(Collections.<RelationMember>emptyList()).getTransferDataFlavors();
        assertEquals(2, flavors.length);
        assertEquals(RELATION_MEMBER_DATA, flavors[0]);
        assertEquals(DataFlavor.stringFlavor, flavors[1]);
    }

    /**
     * Test of {@link RelationMemberTransferable#isDataFlavorSupported} method.
     */
    @Test
    public void testIsDataFlavorSupported() {
        RelationMemberTransferable transferable = new RelationMemberTransferable(Collections.<RelationMember>emptyList());
        assertTrue(transferable.isDataFlavorSupported(RELATION_MEMBER_DATA));
        assertFalse(transferable.isDataFlavorSupported(null));
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferData} method - nominal case.
     * @throws UnsupportedFlavorException never
     */
    @Test
    public void testGetTransferDataNominal() throws UnsupportedFlavorException {
        RelationMemberTransferable rmt = new RelationMemberTransferable(Collections.singleton(new RelationMember("test", new Node(1))));
        assertEquals("node 1 test # incomplete\n", rmt.getTransferData(DataFlavor.stringFlavor));
        Collection<RelationMemberData> td = ((RelationMemberTransferable.Data) rmt.getTransferData(RELATION_MEMBER_DATA))
                .getRelationMemberData();
        assertEquals(1, td.size());
        assertNotNull(td.iterator().next());
    }

    /**
     * Test of {@link RelationMemberTransferable#getTransferData} method - error case.
     * @throws UnsupportedFlavorException always
     */
    @Test(expected = UnsupportedFlavorException.class)
    public void testGetTransferDataError() throws UnsupportedFlavorException {
        new RelationMemberTransferable(Collections.singleton(new RelationMember(null, new Node(1)))).getTransferData(null);
    }
}
