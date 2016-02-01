// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.gui.datatransfer.PrimitiveTransferable.PRIMITIVE_DATA;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Collection;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.PrimitiveData;

/**
 * Unit tests of {@link PrimitiveTransferable} class.
 */
public class PrimitiveTransferableTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link PrimitiveTransferable#getTransferDataFlavors()} method.
     */
    @Test
    public void testGetTransferDataFlavors() {
        DataFlavor[] flavors = new PrimitiveTransferable(null).getTransferDataFlavors();
        assertEquals(2, flavors.length);
        assertEquals(PRIMITIVE_DATA, flavors[0]);
        assertEquals(DataFlavor.stringFlavor, flavors[1]);
    }

    /**
     * Test of {@link PrimitiveTransferable#isDataFlavorSupported} method.
     */
    @Test
    public void testIsDataFlavorSupported() {
        assertTrue(new PrimitiveTransferable(null).isDataFlavorSupported(PRIMITIVE_DATA));
        assertFalse(new PrimitiveTransferable(null).isDataFlavorSupported(null));
    }

    /**
     * Test of {@link PrimitiveTransferable#getTransferData} method - nominal case.
     * @throws UnsupportedFlavorException never
     */
    @Test
    public void testGetTransferDataNominal() throws UnsupportedFlavorException {
        PrimitiveTransferable pt = new PrimitiveTransferable(Collections.singleton(new Node(1)));
        assertEquals("node 1 # incomplete\n", pt.getTransferData(DataFlavor.stringFlavor));
        Collection<PrimitiveData> td = ((PrimitiveTransferable.Data) pt.getTransferData(PRIMITIVE_DATA)).getPrimitiveData();
        assertEquals(1, td.size());
        assertTrue(td.iterator().next() instanceof PrimitiveData);
    }

    /**
     * Test of {@link PrimitiveTransferable#getTransferData} method - error case.
     * @throws UnsupportedFlavorException always
     */
    @Test(expected = UnsupportedFlavorException.class)
    public void testGetTransferDataError() throws UnsupportedFlavorException {
        new PrimitiveTransferable(Collections.singleton(new Node(1))).getTransferData(null);
    }
}
