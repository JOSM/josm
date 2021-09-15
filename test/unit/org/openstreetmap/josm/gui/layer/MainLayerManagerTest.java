// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

/**
 * Tests {@link MainLayerManager}.
 * @author Michael Zangl
 */
@BasicPreferences
class MainLayerManagerTest extends LayerManagerTest {

    private MainLayerManager layerManagerWithActive;

    private class CapturingActiveLayerChangeListener implements ActiveLayerChangeListener {
        private ActiveLayerChangeEvent lastEvent;

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            assertSame(layerManager, e.getSource());
            lastEvent = e;
        }
    }

    private final class CapturingThreadCheckingActiveLayerChangeListener extends CapturingActiveLayerChangeListener {
        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            GuiHelper.assertCallFromEdt();
            super.activeOrEditLayerChanged(e);
        }
    }

    protected static class AbstractTestOsmLayer extends OsmDataLayer {
        public AbstractTestOsmLayer() {
            super(new DataSet(), "OSM layer", null);
        }

        @Override
        public LayerPositionStrategy getDefaultLayerPosition() {
            return LayerPositionStrategy.afterLast(o -> true);
        }
    }

    protected static class LoggingHandler extends Handler {

        private List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}

        public List<LogRecord> getRecords() {
            return records;
        }

    }

    @BeforeAll
    public static void setUpClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Override
    @BeforeEach
    public void setUp() {
        layerManager = layerManagerWithActive = new MainLayerManager();
    }

    @Test
    void testAddLayerSetsActiveLayer() {
        TestLayer layer1 = new TestLayer();
        AbstractTestOsmLayer layer2 = new AbstractTestOsmLayer();
        TestLayer layer3 = new TestLayer();
        assertNull(layerManagerWithActive.getActiveLayer());
        assertNull(layerManagerWithActive.getEditLayer());
        layerManagerWithActive.addLayer(layer1);
        assertSame(layer1, layerManagerWithActive.getActiveLayer());
        assertNull(layerManagerWithActive.getEditLayer());
        layerManagerWithActive.addLayer(layer2);
        assertSame(layer2, layerManagerWithActive.getActiveLayer());
        assertSame(layer2, layerManagerWithActive.getEditLayer());
        layerManagerWithActive.addLayer(layer3);
        assertSame(layer2, layerManagerWithActive.getActiveLayer());
        assertSame(layer2, layerManagerWithActive.getEditLayer());
    }

    @Test
    void testRemoveLayerUnsetsActiveLayer() {
        TestLayer layer1 = new TestLayer();
        AbstractTestOsmLayer layer2 = new AbstractTestOsmLayer();
        TestLayer layer3 = new TestLayer();
        AbstractTestOsmLayer layer4 = new AbstractTestOsmLayer();
        layerManagerWithActive.addLayer(layer1);
        layerManagerWithActive.addLayer(layer2);
        layerManagerWithActive.addLayer(layer3);
        layerManagerWithActive.addLayer(layer4);
        assertSame(layer4, layerManagerWithActive.getActiveLayer());
        assertSame(layer4, layerManagerWithActive.getEditLayer());
        layerManagerWithActive.removeLayer(layer4);
        //prefer osm layers
        assertSame(layer2, layerManagerWithActive.getActiveLayer());
        assertSame(layer2, layerManagerWithActive.getEditLayer());
        layerManagerWithActive.removeLayer(layer2);
        assertSame(layer1, layerManagerWithActive.getActiveLayer());
        assertNull(layerManagerWithActive.getEditLayer());

        layerManagerWithActive.removeLayer(layer1);
        layerManagerWithActive.removeLayer(layer3);
        assertNull(layerManagerWithActive.getActiveLayer());
        assertNull(layerManagerWithActive.getEditLayer());
    }

    /**
     * Test {@link MainLayerManager#addActiveLayerChangeListener(ActiveLayerChangeListener)} and
     * {@link MainLayerManager#addAndFireActiveLayerChangeListener(ActiveLayerChangeListener)}
     */
    @Test
    void testAddActiveLayerChangeListener() {
        TestLayer layer1 = new TestLayer();
        AbstractTestOsmLayer layer2 = new AbstractTestOsmLayer();
        layerManagerWithActive.addLayer(layer1);
        layerManagerWithActive.addLayer(layer2);

        CapturingActiveLayerChangeListener listener = new CapturingThreadCheckingActiveLayerChangeListener();
        layerManagerWithActive.addActiveLayerChangeListener(listener);
        assertNull(listener.lastEvent);

        CapturingActiveLayerChangeListener listener2 = new CapturingActiveLayerChangeListener();
        layerManagerWithActive.addAndFireActiveLayerChangeListener(listener2);
        assertSame(listener2.lastEvent.getPreviousActiveLayer(), null);
        assertSame(listener2.lastEvent.getPreviousDataLayer(), null);

        layerManagerWithActive.setActiveLayer(layer1);
        assertSame(listener2.lastEvent.getPreviousActiveLayer(), layer2);
        assertSame(listener2.lastEvent.getPreviousDataLayer(), layer2);

        layerManagerWithActive.setActiveLayer(layer2);
        assertSame(listener2.lastEvent.getPreviousActiveLayer(), layer1);
        assertSame(listener2.lastEvent.getPreviousDataLayer(), layer2);
    }

    /**
     * Test if {@link MainLayerManager#addActiveLayerChangeListener(ActiveLayerChangeListener)} prevents listener from being added twice.
     */
    @Test
    void testAddActiveLayerChangeListenerTwice() {
        CapturingActiveLayerChangeListener listener = new CapturingActiveLayerChangeListener();
        LoggingHandler handler = new LoggingHandler();
        Logging.getLogger().addHandler(handler);

        layerManagerWithActive.addActiveLayerChangeListener(listener);
        assertTrue(handler.getRecords().isEmpty());

        layerManagerWithActive.addActiveLayerChangeListener(listener);
        assertTrue(handler.getRecords().get(1).getMessage().startsWith("Attempted to add listener that was already in list"));

        Logging.getLogger().removeHandler(handler);
    }

    /**
     * Test if {@link MainLayerManager#removeActiveLayerChangeListener(ActiveLayerChangeListener)} works.
     */
    @Test
    void testRemoveActiveLayerChangeListener() {
        TestLayer layer1 = new TestLayer();
        AbstractTestOsmLayer layer2 = new AbstractTestOsmLayer();
        layerManagerWithActive.addLayer(layer1);
        layerManagerWithActive.addLayer(layer2);

        CapturingActiveLayerChangeListener listener = new CapturingActiveLayerChangeListener();
        layerManagerWithActive.addActiveLayerChangeListener(listener);
        layerManagerWithActive.removeActiveLayerChangeListener(listener);

        layerManagerWithActive.setActiveLayer(layer2);
        assertNull(listener.lastEvent);
    }

    /**
     * Test if {@link MainLayerManager#removeActiveLayerChangeListener(ActiveLayerChangeListener)} checks if listener is in list.
     */
    @Test
    void testRemoveActiveLayerChangeListenerNotInList() {
        LoggingHandler handler = new LoggingHandler();
        Logging.getLogger().addHandler(handler);

        layerManagerWithActive.removeActiveLayerChangeListener(new CapturingActiveLayerChangeListener());
        assertTrue(handler.getRecords().get(1).getMessage().startsWith("Attempted to remove listener that was not in list"));

        Logging.getLogger().removeHandler(handler);
    }

    /**
     * Tests {@link MainLayerManager#setActiveLayer(Layer)} and {@link MainLayerManager#getActiveLayer()}.
     * <p>
     * Edit and active layer getters are also tested in {@link #testAddLayerSetsActiveLayer()}
     */
    @Test
    void testSetGetActiveLayer() {
        TestLayer layer1 = new TestLayer();
        TestLayer layer2 = new TestLayer();
        layerManagerWithActive.addLayer(layer1);
        layerManagerWithActive.addLayer(layer2);

        layerManagerWithActive.setActiveLayer(layer1);
        assertSame(layer1, layerManagerWithActive.getActiveLayer());

        layerManagerWithActive.setActiveLayer(layer2);
        assertSame(layer2, layerManagerWithActive.getActiveLayer());
    }

    /**
     * Tests {@link MainLayerManager#getEditDataSet()}
     */
    @Test
    void testGetEditDataSet() {
        assertNull(layerManagerWithActive.getEditDataSet());
        TestLayer layer0 = new TestLayer();
        layerManagerWithActive.addLayer(layer0);
        assertNull(layerManagerWithActive.getEditDataSet());

        AbstractTestOsmLayer layer1 = new AbstractTestOsmLayer();
        AbstractTestOsmLayer layer2 = new AbstractTestOsmLayer();
        layerManagerWithActive.addLayer(layer1);
        layerManagerWithActive.addLayer(layer2);

        layerManagerWithActive.setActiveLayer(layer1);
        assertSame(layer1.data, layerManagerWithActive.getEditDataSet());

        layerManagerWithActive.setActiveLayer(layer2);
        assertSame(layer2.data, layerManagerWithActive.getEditDataSet());
    }

    /**
     * Tests {@link MainLayerManager#getVisibleLayersInZOrder()}
     */
    @Test
    void testGetVisibleLayersInZOrder() {
        AbstractTestOsmLayer layer1 = new AbstractTestOsmLayer();
        AbstractTestOsmLayer layer2 = new AbstractTestOsmLayer();
        TestLayer layer3 = new TestLayer();
        layer3.setVisible(false);
        AbstractTestOsmLayer layer4 = new AbstractTestOsmLayer();
        TestLayer layer5 = new TestLayer();
        AbstractTestOsmLayer layer6 = new AbstractTestOsmLayer();
        AbstractTestOsmLayer layer7 = new AbstractTestOsmLayer();
        layerManagerWithActive.addLayer(layer1);
        layerManagerWithActive.addLayer(layer2);
        layerManagerWithActive.addLayer(layer3);
        layerManagerWithActive.addLayer(layer4);
        layerManagerWithActive.addLayer(layer5);
        layerManagerWithActive.addLayer(layer6);
        layerManagerWithActive.addLayer(layer7);

        layerManagerWithActive.setActiveLayer(layer1);
        assertEquals(Arrays.asList(layer7, layer6, layer5, layer4, layer2, layer1),
                layerManagerWithActive.getVisibleLayersInZOrder());
        layerManagerWithActive.setActiveLayer(layer4);
        assertEquals(Arrays.asList(layer7, layer6, layer5, layer2, layer1, layer4),
                layerManagerWithActive.getVisibleLayersInZOrder());

        // should not be moved ouside edit layer block
        layerManagerWithActive.setActiveLayer(layer6);
        assertEquals(Arrays.asList(layer7, layer6, layer5, layer4, layer2, layer1),
                layerManagerWithActive.getVisibleLayersInZOrder());
        layerManagerWithActive.setActiveLayer(layer7);
        assertEquals(Arrays.asList(layer6, layer7, layer5, layer4, layer2, layer1),
                layerManagerWithActive.getVisibleLayersInZOrder());

        // ignored
        layerManagerWithActive.setActiveLayer(layer3);
        assertEquals(Arrays.asList(layer7, layer6, layer5, layer4, layer2, layer1),
                layerManagerWithActive.getVisibleLayersInZOrder());
        layerManagerWithActive.setActiveLayer(layer5);
        assertEquals(Arrays.asList(layer7, layer6, layer5, layer4, layer2, layer1),
                layerManagerWithActive.getVisibleLayersInZOrder());

    }

}
