// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link PropertiesMerger} class.
 */
public class PropertiesMergerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link PropertiesMerger#PropertiesMerger}.
     */
    @Test
    public void testPropertiesMerger() {
        assertNotNull(new PropertiesMerger());
    }
}
