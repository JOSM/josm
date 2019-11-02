// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test of the base {@link Layer} class
 * @author Michael Zangl
 */
public class LayerTest {
    /**
     * We need projection
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    private Layer testLayer;

    /**
     * Create test layer
     */
    @Before
    public void setUp() {
        testLayer = new LayerManagerTest.TestLayer();
    }

    /**
     * Test of {@link Layer#isInfoResizable}
     */
    @Test
    public void testIsInfoResizable() {
        assertFalse(testLayer.isInfoResizable());
    }

    /**
     * Test of {@link Layer#getAssociatedFile()} and {@link Layer#setAssociatedFile(java.io.File)}
     */
    @Test
    public void testAssociatedFile() {
        assertNull(testLayer.getAssociatedFile());

        File file = new File("test");
        testLayer.setAssociatedFile(file);
        assertEquals(file, testLayer.getAssociatedFile());
    }

    /**
     * Test {@link Layer#getName()}
     */
    @Test
    public void testGetName() {
        assertEquals("Test Layer", testLayer.getName());
    }

    /**
     * Test {@link Layer#setName(String)}
     */
    @Test
    public void testSetName() {
        testLayer.setName("Test Layer2");
        assertEquals("Test Layer2", testLayer.getName());

        testLayer = new LayerManagerTest.TestLayer();

        testLayer.setName("Test Layer2");
        testLayer.setName(null);
        assertEquals("", testLayer.getName());
        testLayer.setName("Test Layer3");
        assertEquals("Test Layer3", testLayer.getName());
    }

    /**
     * Test {@link Layer#rename(String)} and {@link Layer#isRenamed()}
     */
    @Test
    public void testRename() {
        assertFalse(testLayer.isRenamed());
        testLayer.rename("Test Layer2");
        assertEquals("Test Layer2", testLayer.getName());
        assertTrue(testLayer.isRenamed());
    }

    /**
     * Test {@link Layer#isBackgroundLayer()} and {@link Layer#setBackgroundLayer(boolean)}
     */
    @Test
    public void testBackgroundLayer() {
        assertFalse(testLayer.isBackgroundLayer());
        testLayer.setBackgroundLayer(true);
        assertTrue(testLayer.isBackgroundLayer());
    }

    /**
     * Test {@link Layer#isVisible()} and {@link Layer#setVisible(boolean)}
     */
    @Test
    public void testVisible() {
        assertTrue(testLayer.isVisible());
        testLayer.setVisible(false);
        assertFalse(testLayer.isVisible());
        testLayer.setVisible(true);
        assertTrue(testLayer.isVisible());
    }

    /**
     * Test {@link Layer#toggleVisible()}
     */
    @Test
    public void testToggleVisible() {
        assertTrue(testLayer.isVisible());
        testLayer.toggleVisible();
        assertFalse(testLayer.isVisible());
        testLayer.toggleVisible();
        assertTrue(testLayer.isVisible());
    }

    /**
     * Test {@link Layer#setOpacity(double)} and {@link Layer#getOpacity()}
     */
    @Test
    public void testOpacity() {
        assertEquals(1, testLayer.getOpacity(), 1e-3);

        testLayer.setOpacity(0.5);
        assertEquals(0.5, testLayer.getOpacity(), 1e-3);

        testLayer.setOpacity(0);
        assertFalse(testLayer.isVisible());

        testLayer.setVisible(true);
        assertTrue(testLayer.isVisible());
        assertEquals(1, testLayer.getOpacity(), 1e-3);
    }

    /**
     * Test {@link Layer#isProjectionSupported(org.openstreetmap.josm.data.projection.Projection)}
     */
    @Test
    public void testIsProjectionSupported() {
        assertFalse(testLayer.isProjectionSupported(null));
        assertTrue(testLayer.isProjectionSupported(ProjectionRegistry.getProjection()));
    }

    /**
     * Test {@link Layer#nameSupportedProjections()}
     */
    @Test
    public void testNameSupportedProjections() {
        assertNotNull(testLayer.nameSupportedProjections());
    }

    /**
     * Test {@link Layer#isSavable()}
     */
    @Test
    public void testIsSavable() {
        assertFalse(testLayer.isSavable());
    }

    /**
     * Test {@link Layer#checkSaveConditions()}
     */
    @Test
    public void testCheckSaveConditions() {
        assertTrue(testLayer.checkSaveConditions());
    }
}
