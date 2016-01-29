// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for class {@link OsmPrivileges}.
 */
public class OsmPrivilegesTest {

    /**
     * Unit test of getters/setters.
     */
    @Test
    public void testGettersSetters() {
        OsmPrivileges p = new OsmPrivileges();
        assertFalse(p.isAllowModifyNotes());
        assertFalse(p.isAllowReadGpx());
        assertFalse(p.isAllowReadPrefs());
        assertFalse(p.isAllowWriteApi());
        assertFalse(p.isAllowWriteGpx());
        assertFalse(p.isAllowWritePrefs());
        p.setAllowModifyNotes(true);
        assertTrue(p.isAllowModifyNotes());
        p.setAllowReadGpx(true);
        assertTrue(p.isAllowReadGpx());
        p.setAllowReadPrefs(true);
        assertTrue(p.isAllowReadPrefs());
        p.setAllowWriteApi(true);
        assertTrue(p.isAllowWriteApi());
        p.setAllowWriteGpx(true);
        assertTrue(p.isAllowWriteGpx());
        p.setAllowWritePrefs(true);
        assertTrue(p.isAllowWritePrefs());
    }
}
