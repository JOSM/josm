// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Unit tests of {@link ValidatorTreePanel} class.
 */
public class ValidatorTreePanelTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link ValidatorTreePanel#ValidatorTreePanel}.
     */
    @Test
    public void testValidatorTreePanel() {
        assertNotNull(new ValidatorTreePanel());

        ValidatorTreePanel vtp = new ValidatorTreePanel(new ArrayList<>(Arrays.asList(
                new TestError(null, Severity.ERROR, "err", 0, new Node(1)),
                new TestError(null, Severity.WARNING, "warn", 0, new Node(2)))));
        assertNotNull(vtp);
        assertEquals(2, vtp.getErrors().size());
        vtp.setVisible(true);
        vtp.setVisible(false);
        Node n = new Node(10);
        vtp.setErrors(Arrays.asList(new TestError(null, Severity.ERROR, "", 0, n)));
        assertEquals(1, vtp.getErrors().size());
        vtp.selectRelatedErrors(Collections.<OsmPrimitive>singleton(n));
        vtp.expandAll();
        assertNotNull(vtp.getRoot());
        vtp.resetErrors();
        Set<? extends OsmPrimitive> filter = new HashSet<>(Arrays.asList(n));
        vtp.setFilter(filter);
        assertEquals(filter, vtp.getFilter());
        vtp.setFilter(new HashSet<OsmPrimitive>());
        assertNull(vtp.getFilter());
        vtp.setFilter(null);
        assertNull(vtp.getFilter());
        vtp.destroy();
    }
}
