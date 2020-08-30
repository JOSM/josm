// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openstreetmap.josm.testutils.ThrowableRootCauseMatcher.hasRootCause;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
import org.openstreetmap.josm.tools.bugreport.ReportedException;

/**
 * Test the {@link LayerManager} class.
 * @author Michael Zangl
 *
 */
public class LayerManagerTest {
    /**
     * This is a layer that can be used in tests. It does not do anything and provides a simple, fake implementation.
     * @author Michael Zangl
     */
    public static class TestLayer extends Layer {
        /**
         * Create a new test layer.
         */
        public TestLayer() {
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
            return new Action[0];
        }

        @Override
        public Object getInfoComponent() {
            return null;
        }

        @Override
        public Icon getIcon() {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    // nop
                }

                @Override
                public int getIconWidth() {
                    return 10;
                }

                @Override
                public int getIconHeight() {
                    return 10;
                }
            };
        }

        @Override
        public LayerPositionStrategy getDefaultLayerPosition() {
            return LayerPositionStrategy.afterLast(o -> true);
        }
    }

    protected static class TestLayer2 extends TestLayer {}

    /**
     * Intercepts the events for easier testing.
     * @author Michael Zangl
     *
     */
    protected class CapturingLayerChangeListener implements LayerChangeListener {
        protected LayerAddEvent layerAdded;
        protected LayerRemoveEvent layerRemoved;
        protected LayerOrderChangeEvent layerOrderChanged;

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

    private final class ResetStateChangeListener extends CapturingLayerChangeListener {
        int removed = 0;

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            // count only
            removed++;
        }
    }

    /** the tested layer manager */
    protected LayerManager layerManager;

    /**
     * Set up test layer manager.
     */
    @Before
    public void setUp() {
        layerManager = new LayerManager();
    }

    /**
     * {@link LayerManager#addLayer(Layer)}
     */
    @Test
    public void testAddLayer() {
        Layer layer1 = new TestLayer() {
            @Override
            public LayerPositionStrategy getDefaultLayerPosition() {
                return LayerPositionStrategy.IN_FRONT;
            }

            @Override
            public boolean isBackgroundLayer() {
                return true;
            }
        };
        Layer layer2 = new TestLayer() {
            @Override
            public LayerPositionStrategy getDefaultLayerPosition() {
                return LayerPositionStrategy.IN_FRONT;
            }
        };
        Layer layer3 = new TestLayer() {
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
        TestLayer layer4 = new TestLayer();
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
    @Test
    public void testAddLayerFails() {
        Exception e = assertThrows(ReportedException.class, () -> {
            TestLayer layer1 = new TestLayer();
            layerManager.addLayer(layer1);
            layerManager.addLayer(layer1);
        });
        assertThat(e.getCause(), is(instanceOf(InvocationTargetException.class)));
        assertThat(e.getCause(), hasRootCause(is(instanceOf(IllegalArgumentException.class))));
    }

    /**
     * {@link LayerManager#addLayer(Layer)}: illegal default layer position
     */
    @Test
    public void testAddLayerIllegalPosition() {
        Exception e = assertThrows(ReportedException.class, () -> {
            TestLayer layer1 = new TestLayer() {
                @Override
                public LayerPositionStrategy getDefaultLayerPosition() {
                    return manager -> 42;
                }
            };
            layerManager.addLayer(layer1);
        });
        assertThat(e.getCause(), is(instanceOf(InvocationTargetException.class)));
        assertThat(e.getCause(), hasRootCause(is(instanceOf(IndexOutOfBoundsException.class))));
    }

    /**
     * {@link LayerManager#removeLayer(Layer)}
     */
    @Test
    public void testRemoveLayer() {
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
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
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
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
    @Test
    public void testMoveLayerFailsRange() {
        Exception e = assertThrows(ReportedException.class, () -> {
            TestLayer layer1 = new TestLayer();
            TestLayer layer2 = new TestLayer();
            layerManager.addLayer(layer1);
            layerManager.addLayer(layer2);
            layerManager.moveLayer(layer2, 2);
        });
        assertThat(e.getCause(), is(instanceOf(InvocationTargetException.class)));
        assertThat(e.getCause(), hasRootCause(is(instanceOf(IndexOutOfBoundsException.class))));
    }

    /**
     * {@link LayerManager#moveLayer(Layer, int)} fails for wrong layer
     */
    @Test
    public void testMoveLayerFailsNotInList() {
        Exception e = assertThrows(ReportedException.class, () -> {
            TestLayer layer1 = new TestLayer();
            TestLayer layer2 = new TestLayer();
            layerManager.addLayer(layer1);
            layerManager.moveLayer(layer2, 0);
        });
        assertThat(e.getCause(), is(instanceOf(InvocationTargetException.class)));
        assertThat(e.getCause(), hasRootCause(is(instanceOf(IllegalArgumentException.class))));
    }

    /**
     * {@link LayerManager#getLayers()} unmodifiable
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testGetLayers() {
        // list should be immutable
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        layerManager.getLayers().remove(0);
    }

    /**
     * {@link LayerManager#getLayersOfType(Class)}
     */
    @Test
    public void testGetLayersOfType() {
        TestLayer2 layer1 = new TestLayer2();
        TestLayer2 layer2 = new TestLayer2();
        layerManager.addLayer(layer1);
        layerManager.addLayer(new TestLayer());
        layerManager.addLayer(layer2);

        assertEquals(layerManager.getLayersOfType(TestLayer2.class), Arrays.asList(layer1, layer2));
    }

    /**
     * {@link LayerManager#containsLayer(Layer)}
     */
    @Test
    public void testContainsLayer() {
        TestLayer layer = new TestLayer();
        layerManager.addLayer(layer);
        layerManager.addLayer(new TestLayer());

        assertTrue(layerManager.containsLayer(layer));
        assertFalse(layerManager.containsLayer(new TestLayer()));
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
    public void testAddLayerChangeListenerDuplicates() {
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.addLayerChangeListener(l);
    }

    /**
     * {@link LayerManager#addAndFireLayerChangeListener(LayerChangeListener)} fires fake add events
     */
    @Test
    public void testAddLayerChangeListenerFire() {
        final ArrayList<Layer> fired = new ArrayList<>();
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);
        layerManager.addAndFireLayerChangeListener(new LayerChangeListener() {
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
        });

        assertEquals(Arrays.asList(layer1, layer2), fired);
    }

    /**
     * {@link LayerManager#removeLayerChangeListener(LayerChangeListener)}
     */
    @Test
    public void testRemoveLayerChangeListener() {
        CapturingLayerChangeListener l = new CapturingLayerChangeListener();
        layerManager.addLayerChangeListener(l);
        layerManager.addLayer(new TestLayer());
        layerManager.removeLayerChangeListener(l);
        layerManager.addLayer(new TestLayer());
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
     * {@link LayerManager#removeAndFireLayerChangeListener(LayerChangeListener)} fires fake remove events
     */
    @Test
    public void testRemoveLayerChangeListenerFire() {
        final ArrayList<Layer> fired = new ArrayList<>();
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
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
        layerManager.addLayerChangeListener(listener);
        layerManager.removeAndFireLayerChangeListener(listener);

        assertEquals(Arrays.asList(layer1, layer2), fired);
    }

    /**
     * Test {@link LayerRemoveEvent#scheduleRemoval(java.util.Collection)}
     */
    @Test
    public void testLayerRemoveScheduleRemoval() {
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
        layerManager.addLayer(layer1);
        layerManager.addLayer(layer2);

        layerManager.addLayerChangeListener(new LayerChangeListener() {
            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                    if (e.getRemovedLayer() == layer1) {
                        e.scheduleRemoval(Collections.singleton(layer2));
                    }
            }

            @Override
            public void layerOrderChanged(LayerOrderChangeEvent e) {
                fail();
            }

            @Override
            public void layerAdded(LayerAddEvent e) {
                fail();
            }
        });

        layerManager.removeLayer(layer1);
        assertEquals(0, layerManager.getLayers().size());
    }

    /**
     * Test {@link LayerManager#resetState()}
     */
    @Test
    public void testResetState() {
        ResetStateChangeListener changeListener = new ResetStateChangeListener();
        layerManager.addLayer(new TestLayer());
        layerManager.addLayerChangeListener(changeListener);
        layerManager.addLayer(new TestLayer());
        assertEquals(2, layerManager.getLayers().size());
        assertNotNull(changeListener.layerAdded);

        layerManager.resetState();
        changeListener.layerAdded = null;

        assertEquals(2, changeListener.removed);
        assertEquals(0, layerManager.getLayers().size());
        layerManager.addLayer(new TestLayer());
        assertNull(changeListener.layerAdded);
    }

}
