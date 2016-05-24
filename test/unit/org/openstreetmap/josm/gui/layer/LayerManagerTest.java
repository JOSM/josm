// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Action;
import javax.swing.Icon;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Predicates;

/**
 * Test the {@link LayerManager} class.
 * @author Michael Zangl
 *
 */
public class LayerManagerTest {

    protected static class AbstractTestLayer extends Layer {
        protected AbstractTestLayer() {
            super("Test Layer");
        }

        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        }

        @Override
        public void visitBoundingBox(BoundingXYVisitor v) {
        }

        @Override
        public void mergeFrom(Layer from) {
        }

        @Override
        public boolean isMergable(Layer other) {
            return false;
        }

        @Override
        public String getToolTipText() {
            return null;
        }

        @Override
        public Action[] getMenuEntries() {
            return null;
        }

        @Override
        public Object getInfoComponent() {
            return null;
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public LayerPositionStrategy getDefaultLayerPosition() {
            return LayerPositionStrategy.afterLast(Predicates.<Layer> alwaysTrue());
        }
    }

    protected static class AbstractTestLayer2 extends AbstractTestLayer {}

    /**
     * Intercepts the events for easier testing.
     * @author Michael Zangl
     *
     */
    protected class CapturingLayerChangeListener implements LayerChangeListener {
        private LayerAddEvent layerAdded;
        private LayerRemoveEvent layerRemoved;
        private LayerOrderChangeEvent layerOrderChanged;

        @Override
        public void layerAdded(LayerAddEvent e) {
            GuiHelper.assertCallFromEdt();
            assertNull(layerAdded);
            assertSame(layerManager, e.getSource());
            layerAdded = e;
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            GuiHelper.assertCallFromEdt();
            assertNull(layerRemoved);
            assertSame(layerManager, e.getSource());
            layerRemoved = e;
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            GuiHelper.assertCallFromEdt();
            assertNull(layerOrderChanged);
            assertSame(layerManager, e.getSource());
            layerOrderChanged = e;
        }

    }

    protected LayerManager layerManager;

    /**
     * Set up test layer manager.
     */
    @Before
    public void setUp()   {
        layerManager = new LayerManager();
    }

    /**
     * {@link LayerManager#addLayer(Layer)}
     */
    @Test
    public void testAddLayer() {
        Layer layer1 = new AbstractTestLayer() {
            @Override
            public LayerPositionStrategy getDefaultLayerPosition() {
                return LayerPositionStrategy.IN_FRONT;
            }

            @Override
            public boolean isBackgroundLayer() {
                return true;
            }
        };
        Layer layer2 = new AbstractTestLayer() {
            @Override
            public LayerPositionStrategy getDefaultLayerPosition() {
                return LayerPositionStrategy.IN_FRONT;
            }
        };
        Layer layer3 = new AbstractTestLayer() {
            @Override
            public LayerPositionStrategy getDefaultLayerPosition() {
                return LayerPositionStrategy.BEFORE_FIRST_BACKGROUND_LAYER;
            }
        };

        layerManager.addLayer(layer1);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer1));
        layerManager.addLayer(layer2);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer2, layer1));
        layerManager.addLayer(layer3);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer2, layer3, layer1));

        // event
        AbstractTestLayer layer4 = new AbstractTestLayer();
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.addLayer(layer4);

        assertSame(layer4, l.layerAdded.getAddedLayer());
        assertNull(l.layerRemoved);
        assertNull(l.layerOrderChanged);
    }

    /**
     * {@link LayerManager#addLayer(Layer)}: duplicate layers
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddLayerFails() {
        AbstractTestLayer layer1 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer1);
    }

    /**
     * {@link LayerManager#addLayer(Layer)}: illegal default layer position
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddLayerIllegalPosition() {
        AbstractTestLayer layer1 = new AbstractTestLayer() {
            @Override
            public LayerPositionStrategy getDefaultLayerPosition() {
                return new LayerPositionStrategy() {
                    @Override
                    public int getPosition(LayerManager manager) {
                        return 42;
                    }
                };
            }
        };
        layerManager.addLayer(layer1);
    }

    /**
     * {@link LayerManager#removeLayer(Layer)}
     */
    @Test
    public void testRemoveLayer() {
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer1, layer2));

        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.removeLayer(layer2);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer1));

        assertNull(l.layerAdded);
        assertSame(layer2, l.layerRemoved.getRemovedLayer());
        assertNull(l.layerOrderChanged);
    }

    /**
     * {@link LayerManager#moveLayer(Layer, int)}
     */
    @Test
    public void testMoveLayer() {
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer1, layer2));

        layerManager.moveLayer(layer2, 0);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer2, layer1));

        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.moveLayer(layer2, 1);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer1, layer2));

        assertNull(l.layerAdded);
        assertNull(l.layerRemoved);
        assertNotNull(l.layerOrderChanged);

        // This should not change anything and not fire any event
        layerManager.moveLayer(layer2, 1);
        assertEquals(layerManager.getLayers(), Arrays.asList(layer1, layer2));
    }

    /**
     * {@link LayerManager#moveLayer(Layer, int)} fails for wrong index
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testMoveLayerFailsRange() {
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        layerManager.moveLayer(layer2, 2);
    }

    /**
     * {@link LayerManager#moveLayer(Layer, int)} fails for wrong layer
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMoveLayerFailsNotInList() {
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.moveLayer(layer2, 0);
    }

    /**
     * {@link LayerManager#getLayers()} unmodifiable
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testGetLayers() {
        // list should be immutable
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        layerManager.getLayers().remove(0);
    }

    /**
     * {@link LayerManager#getLayersOfType(Class)}
     */
    @Test
    public void testGetLayersOfType() {
        AbstractTestLayer2 layer1 = new AbstractTestLayer2();
        AbstractTestLayer2 layer2 = new AbstractTestLayer2();
        layerManager.addLayer(layer1);
        layerManager.addLayer(new AbstractTestLayer());
        layerManager.addLayer(layer2);

        assertEquals(layerManager.getLayersOfType(AbstractTestLayer2.class), Arrays.asList(layer1, layer2));
    }

    /**
     * {@link LayerManager#containsLayer(Layer)}
     */
    @Test
    public void testContainsLayer() {
        AbstractTestLayer layer = new AbstractTestLayer();
        layerManager.addLayer(layer);
        layerManager.addLayer(new AbstractTestLayer());

        assertTrue(layerManager.containsLayer(layer));
        assertFalse(layerManager.containsLayer(new AbstractTestLayer()));
    }

    /**
     * {@link LayerManager#addLayerChangeListener(LayerChangeListener)}
     */
    @Test
    public void testAddLayerChangeListener() {
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        assertNull(l.layerAdded);
        assertNull(l.layerRemoved);
        assertNull(l.layerOrderChanged);
    }

    /**
     * {@link LayerManager#addLayerChangeListener(LayerChangeListener)} twice
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddLayerChangeListenerDupplicates() {
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.addLayerChangeListener(l);
    }

    /**
     * {@link LayerManager#addLayerChangeListener(LayerChangeListener, boolean)} fires fake add events
     */
    @Test
    public void testAddLayerChangeListenerFire() {
        final ArrayList<Layer> fired = new ArrayList<>();
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        layerManager.addLayerChangeListener(new LayerChangeListener() {
            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                fail();
            }

            @Override
            public void layerOrderChanged(LayerOrderChangeEvent e) {
                fail();
            }

            @Override
            public void layerAdded(LayerAddEvent e) {
                fired.add(e.getAddedLayer());
            }
        }, true);

        assertEquals(Arrays.asList(layer1, layer2), fired);
    }

    /**
     * {@link LayerManager#removeLayerChangeListener(LayerChangeListener)}
     */
    @Test
    public void testRemoveLayerChangeListener() {
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.addLayer(new AbstractTestLayer());
        layerManager.removeLayerChangeListener(l);
        layerManager.addLayer(new AbstractTestLayer());
        // threw exception when fired twice.
        assertNotNull(l.layerAdded);
        assertNull(l.layerRemoved);
        assertNull(l.layerOrderChanged);
    }

    /**
     * {@link LayerManager#removeLayerChangeListener(LayerChangeListener)} listener not in list
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveLayerChangeListenerNotAdded() {
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.removeLayerChangeListener(l);
    }

    /**
     * {@link LayerManager#removeLayerChangeListener(LayerChangeListener, boolean)} fires fake remove events
     */
    @Test
    public void testRemoveLayerChangeListenerFire() {
        final ArrayList<Layer> fired = new ArrayList<>();
        AbstractTestLayer layer1 = new AbstractTestLayer();
        AbstractTestLayer layer2 = new AbstractTestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        LayerChangeListener listener = new LayerChangeListener() {
            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                fired.add(e.getRemovedLayer());
            }

            @Override
            public void layerOrderChanged(LayerOrderChangeEvent e) {
                fail();
            }

            @Override
            public void layerAdded(LayerAddEvent e) {
                fail();
            }
        };
        layerManager.addLayerChangeListener(listener, false);
        layerManager.removeLayerChangeListener(listener, true);

        assertEquals(Arrays.asList(layer1, layer2), fired);
    }

}
