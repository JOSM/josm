// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link MultiValueCellEditor} class.
 */
public class MultiValueCellEditorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MultiValueCellEditor#MultiValueCellEditor}.
     */
    @Test
    public void testMultiValueCellEditor() {
        assertNotNull(new MultiValueCellEditor().getTableCellEditorComponent(null, new MultiValueResolutionDecision(), false, 0, 0));
    }
}
