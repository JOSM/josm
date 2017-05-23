// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests of {@link DNSName} class.
 */
public class DNSNameTest {

    /**
     * Unit test of {@link DNSName#DNSName} - null check.
     * @throws IOException always (expected with null name)
     */
    @Test(expected = IOException.class)
    public void testDNSNameNull() throws IOException {
        new DNSName(null);
    }

    /**
     * Unit test of {@link DNSName#DNSName} - nominal cases.
     * @throws IOException never
     */
    @Test
    public void testDNSNameNominal() throws IOException {
        assertEquals("localhost", new DNSName("localhost").getName());
        assertEquals("127.0.0.1", new DNSName("127.0.0.1").getName());
    }
}
