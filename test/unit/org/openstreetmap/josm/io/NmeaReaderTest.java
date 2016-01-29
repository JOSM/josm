// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.junit.Test;
import org.openstreetmap.josm.io.NmeaReader.NMEA_TYPE;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link NmeaReader} class.
 */
public class NmeaReaderTest {

    /**
     * Unit test of methods {@link NMEA_TYPE#equals} and {@link NMEA_TYPE#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(NMEA_TYPE.class).verify();
    }
}
