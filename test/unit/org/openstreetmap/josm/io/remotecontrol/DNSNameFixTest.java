// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests of {@link DNSNameFix} class.
 */
public class DNSNameFixTest {

    /**
     * Unit test of {@link DNSNameFix#DNSNameFix} - null check.
     * @throws IOException always (expected with null name)
     */
    @Test(expected = IOException.class)
    public void testDNSNameNull() throws IOException {
        new DNSNameFix(null);
    }

    /**
     * Unit test of {@link DNSNameFix#DNSNameFix} - nominal cases.
     * @throws IOException never
     */
    @Test
    public void testDNSNameNominal() throws IOException {
        assertEquals("localhost", new DNSNameFix("localhost").getName());
        assertEquals("127.0.0.1", new DNSNameFix("127.0.0.1").getName());
    }
}
