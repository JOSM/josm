// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.bbox.SourceButton;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.ImagePatternMatching;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MinimapDialog} class.
 */
public class MinimapDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules josmTestRules = new JOSMTestRules().main().projection().fakeImagery();

    /**
     * Unit test of {@link MinimapDialog} class.
     */
    @Test
    public void testMinimapDialog() {
        MinimapDialog dlg = new MinimapDialog();
        dlg.showDialog();
        assertTrue(dlg.isVisible());
        dlg.hideDialog();
        assertFalse(dlg.isVisible());
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Throwable;
    }

    protected static Runnable uncheckExceptions(final ThrowingRunnable tr) {
        return (() -> {
            try {
                tr.run();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected void assertSingleSelectedSourceLabel(final String label) {
        JPopupMenu menu = this.sourceButton.getPopupMenu();
        boolean found = false;
        for (Component c: menu.getComponents()) {
            if (JPopupMenu.Separator.class.isInstance(c)) {
                break;
            } else {
                boolean equalText = ((JMenuItem) c).getText().equals(label);
                boolean isSelected = ((JMenuItem) c).isSelected();
                assertEquals(equalText, isSelected);
                if (equalText) {
                    assertFalse("Second selected source found", found);
                    found = true;
                }
            }
        }
        assertTrue("Selected source not found in menu", found);
    }

    protected void clickSourceMenuItemByLabel(final String label) {
        try {
            GuiHelper.runInEDTAndWaitWithException(() -> {
                JPopupMenu menu = this.sourceButton.getPopupMenu();
                for (Component c: menu.getComponents()) {
                    if (JPopupMenu.Separator.class.isInstance(c)) {
                        // sources should all come before any separators
                        break;
                    } else if (Objects.equals(((JMenuItem) c).getText(), label)) {
                        ((JMenuItem) c).doClick();
                        return;
                    }
                    // else continue...
                }
                fail();
            });
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to find menu item with label %s: %s", label, e), e);
        }
    }

    protected void assertSourceLabelsVisible(final String... labels) {
        GuiHelper.runInEDTAndWaitWithException(() -> {
            final ArrayList<String> menuLabels = new ArrayList<>();
            final JPopupMenu menu = this.sourceButton.getPopupMenu();
            for (Component c: menu.getComponents()) {
                if (c instanceof JPopupMenu.Separator) {
                    break;
                }
                menuLabels.add(((JMenuItem) c).getText());
            }

            assertArrayEquals(
                labels,
                menuLabels.toArray()
            );
        });
    }

    private MinimapDialog minimap;
    private SlippyMapBBoxChooser slippyMap;
    private SourceButton sourceButton;
    private Callable<Boolean> slippyMapTasksFinished;

    private static BufferedImage paintedSlippyMap;

    protected void setUpMiniMap() {
        GuiHelper.runInEDTAndWaitWithException(uncheckExceptions(() -> {
            this.minimap = new MinimapDialog();
            this.minimap.setSize(300, 200);
            this.minimap.showDialog();
            this.slippyMap = (SlippyMapBBoxChooser) TestUtils.getPrivateField(this.minimap, "slippyMap");
            this.sourceButton = (SourceButton) TestUtils.getPrivateField(this.slippyMap, "iSourceButton");

            // get minimap in a paintable state
            this.minimap.addNotify();
            this.minimap.doLayout();
        }));

        this.slippyMapTasksFinished = () -> !this.slippyMap.getTileController().getTileLoader().hasOutstandingTasks();
    }

    protected void paintSlippyMap() {
        if (paintedSlippyMap == null ||
            paintedSlippyMap.getWidth() != this.slippyMap.getSize().width ||
            paintedSlippyMap.getHeight() != this.slippyMap.getSize().height) {
            paintedSlippyMap = new BufferedImage(
                this.slippyMap.getSize().width,
                this.slippyMap.getSize().height,
                BufferedImage.TYPE_INT_RGB
            );
        } // else reuse existing one - allocation is expensive

        // clear background to a recognizably "wrong" color & dispose our Graphics2D so we don't risk carrying over
        // any state
        Graphics2D g = paintedSlippyMap.createGraphics();
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, paintedSlippyMap.getWidth(), paintedSlippyMap.getHeight());
        g.dispose();

        g = paintedSlippyMap.createGraphics();
        this.slippyMap.paintAll(g);
    }

    /**
     * Tests to switch imagery source.
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourceSwitching() throws Exception {
        // relevant prefs starting out empty, should choose the first source and have shown download area enabled
        // (not that there's a data layer for it to use)

        this.setUpMiniMap();

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffffffff, paintedSlippyMap.getRGB(0, 0));

        this.assertSingleSelectedSourceLabel("White Tiles");

        this.clickSourceMenuItemByLabel("Magenta Tiles");
        this.assertSingleSelectedSourceLabel("Magenta Tiles");
        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffff00ff, paintedSlippyMap.getRGB(0, 0));

        this.clickSourceMenuItemByLabel("Green Tiles");
        this.assertSingleSelectedSourceLabel("Green Tiles");
        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xff00ff00, paintedSlippyMap.getRGB(0, 0));

        assertEquals("Green Tiles", Config.getPref().get("slippy_map_chooser.mapstyle", "Fail"));
    }

    /**
     * Tests that the apparently-selected TileSource survives the tile sources being refreshed.
     * @throws Exception if any error occurs
     */
    @Test
    public void testRefreshSourcesRetainsSelection() throws Exception {
        // relevant prefs starting out empty, should choose the first source and have shown download area enabled
        // (not that there's a data layer for it to use)

        this.setUpMiniMap();

        this.clickSourceMenuItemByLabel("Magenta Tiles");
        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffff00ff, paintedSlippyMap.getRGB(0, 0));

        this.slippyMap.refreshTileSources();

        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffff00ff, paintedSlippyMap.getRGB(0, 0));
    }

    /**
     * Tests that the currently selected source being removed from ImageryLayerInfo will remain present and
     * selected in the source menu even after the tile sources have been refreshed.
     * @throws Exception if any error occurs
     */
    @Test
    public void testRemovedSourceStillSelected() throws Exception {
        // relevant prefs starting out empty, should choose the first source and have shown download area enabled
        // (not that there's a data layer for it to use)

        this.setUpMiniMap();

        this.clickSourceMenuItemByLabel("Green Tiles");

        ImageryLayerInfo.instance.remove(
            ImageryLayerInfo.instance.getLayers().stream().filter(i -> i.getName().equals("Green Tiles")).findAny().get()
        );

        this.assertSingleSelectedSourceLabel("Green Tiles");

        this.slippyMap.refreshTileSources();

        this.assertSingleSelectedSourceLabel("Green Tiles");

        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xff00ff00, paintedSlippyMap.getRGB(0, 0));
    }

    /**
     * Tests the tile source list includes sources only present in the LayerManager
     * @throws Exception if any error occurs
     */
    @Test
    public void testTileSourcesFromCurrentLayers() throws Exception {
        // relevant prefs starting out empty, should choose the first (ImageryLayerInfo) source and have shown download area enabled
        // (not that there's a data layer for it to use)

        final ImageryInfo magentaTilesInfo = ImageryLayerInfo.instance.getLayers().stream().filter(
            i -> i.getName().equals("Magenta Tiles")
        ).findAny().get();
        final ImageryInfo blackTilesInfo = ImageryLayerInfo.instance.getLayers().stream().filter(
            i -> i.getName().equals("Black Tiles")
        ).findAny().get();

        // first we will remove "Magenta Tiles" from ImageryLayerInfo
        ImageryLayerInfo.instance.remove(magentaTilesInfo);

        this.setUpMiniMap();

        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles"
        );

        final ImageryLayer magentaTilesLayer = ImageryLayer.create(magentaTilesInfo);
        GuiHelper.runInEDT(() -> MainApplication.getLayerManager().addLayer(magentaTilesLayer));

        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles",
            "Magenta Tiles"
        );

        this.clickSourceMenuItemByLabel("Magenta Tiles");
        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffff00ff, paintedSlippyMap.getRGB(0, 0));

        final ImageryLayer blackTilesLayer = ImageryLayer.create(blackTilesInfo);
        GuiHelper.runInEDT(() -> MainApplication.getLayerManager().addLayer(blackTilesLayer));

        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles",
            "Magenta Tiles"
        );

        this.clickSourceMenuItemByLabel("Black Tiles");
        this.assertSingleSelectedSourceLabel("Black Tiles");

        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xff000000, paintedSlippyMap.getRGB(0, 0));

        // removing magentaTilesLayer while it is *not* the selected TileSource should make it disappear
        // immediately
        GuiHelper.runInEDT(() -> MainApplication.getLayerManager().removeLayer(magentaTilesLayer));

        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles"
        );
        this.assertSingleSelectedSourceLabel("Black Tiles");

        final ImageryLayer magentaTilesLayer2 = ImageryLayer.create(magentaTilesInfo);
        GuiHelper.runInEDT(() -> MainApplication.getLayerManager().addLayer(magentaTilesLayer2));

        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles",
            "Magenta Tiles"
        );

        this.clickSourceMenuItemByLabel("Magenta Tiles");
        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffff00ff, paintedSlippyMap.getRGB(0, 0));

        // removing magentaTilesLayer while it *is* the selected TileSource...
        GuiHelper.runInEDT(() -> MainApplication.getLayerManager().removeLayer(magentaTilesLayer2));

        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles",
            "Magenta Tiles"
        );
        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        this.clickSourceMenuItemByLabel("Green Tiles");
        this.assertSingleSelectedSourceLabel("Green Tiles");
        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles"
        );

        // removing blackTilesLayer shouldn't remove it from the menu as it is already in ImageryLayerInfo
        GuiHelper.runInEDT(() -> MainApplication.getLayerManager().removeLayer(blackTilesLayer));

        this.assertSingleSelectedSourceLabel("Green Tiles");
        assertSourceLabelsVisible(
            "White Tiles",
            "Black Tiles",
            "Green Tiles"
        );
    }

    /**
     * Tests minimap obeys a saved "mapstyle" preference on startup.
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourcePrefObeyed() throws Exception {
        Config.getPref().put("slippy_map_chooser.mapstyle", "Green Tiles");

        this.setUpMiniMap();

        this.assertSingleSelectedSourceLabel("Green Tiles");

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xff00ff00, paintedSlippyMap.getRGB(0, 0));

        this.clickSourceMenuItemByLabel("Magenta Tiles");
        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        assertEquals("Magenta Tiles", Config.getPref().get("slippy_map_chooser.mapstyle", "Fail"));
    }

    /**
     * Tests minimap handles an unrecognized "mapstyle" preference on startup
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourcePrefInvalid() throws Exception {
        Config.getPref().put("slippy_map_chooser.mapstyle", "Hooloovoo Tiles");

        this.setUpMiniMap();

        this.assertSingleSelectedSourceLabel("White Tiles");

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffffffff, paintedSlippyMap.getRGB(0, 0));
    }

    /**
     * test viewport marker rectangle matches the mapView's aspect ratio
     * @throws Exception if any error occurs
     */
    @Test
    public void testViewportAspectRatio() throws Exception {
        // Add a test layer to the layer manager to get the MapFrame & MapView
        MainApplication.getLayerManager().addLayer(new TestLayer());

        Config.getPref().put("slippy_map_chooser.mapstyle", "White Tiles");
        // ensure projection matches JMapViewer's
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));

        MapView mapView = MainApplication.getMap().mapView;
        GuiHelper.runInEDTAndWaitWithException(() -> {
            mapView.setVisible(true);
            mapView.addNotify();
            mapView.doLayout();
            // ensure we have a square mapView viewport
            mapView.setBounds(0, 0, 350, 350);
        });

        this.setUpMiniMap();

        // attempt to set viewport to cover a non-square area
        mapView.zoomTo(new Bounds(26.27, -18.23, 26.275, -18.229));

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        Map<Integer, String> paletteMap = new HashMap<Integer, String>() {{
            put(0xffffffff, "w");  // white
            put(0xff000000, "b");  // black
            put(0xfff0d1d1, "p");  // pink
        }};

        Matcher rowMatcher = ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^(w+)b(p+)b(w+)$",
            true
        );

        // (within a tolerance for numerical error) the number of pixels on the left of the viewport marker
        // should equal the number on the right
        assertTrue(
            "Viewport marker not horizontally centered",
            Math.abs(rowMatcher.group(1).length() - rowMatcher.group(3).length()) < 4
        );

        Matcher colMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^(w+)b(p+)b(w+)$",
            true
        );

        // (within a tolerance for numerical error) the number of pixels on the top of the viewport marker
        // should equal the number on the bottom
        assertTrue(
            "Viewport marker not vertically centered",
            Math.abs(colMatcher.group(1).length() - colMatcher.group(3).length()) < 4
        );

        // (within a tolerance for numerical error) the viewport marker should be square
        assertTrue(
            "Viewport marker not square",
            Math.abs(colMatcher.group(2).length() - rowMatcher.group(2).length()) < 4
        );

        // now change the mapView size
        GuiHelper.runInEDTAndWaitWithException(() -> {
            mapView.setBounds(0, 0, 150, 300);
            Arrays.stream(mapView.getComponentListeners()).forEach(
                cl -> cl.componentResized(new ComponentEvent(mapView, ComponentEvent.COMPONENT_RESIZED))
            );
        });
        // minimap doesn't (yet?) listen for component resize events to update its viewport marker, so
        // trigger a zoom change
        mapView.zoomTo(mapView.getCenter().add(1., 0.));
        this.paintSlippyMap();

        rowMatcher = ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^(w+)b(p+)b(w+)$",
            true
        );
        assertTrue(
            "Viewport marker not horizontally centered",
            Math.abs(rowMatcher.group(1).length() - rowMatcher.group(3).length()) < 4
        );

        colMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^(w+)b(p+)b(w+)$",
            true
        );
        assertTrue(
            "Viewport marker not vertically centered",
            Math.abs(colMatcher.group(1).length() - colMatcher.group(3).length()) < 4
        );

        try {
            javax.imageio.ImageIO.write(paintedSlippyMap, "png", new java.io.File("failed.png"));
        } catch (java.io.IOException ioe) {
            System.err.println("Failed writing image");
        }

        assertTrue(
            "Viewport marker not 2:1 aspect ratio",
            Math.abs(colMatcher.group(2).length() - (rowMatcher.group(2).length()*2.0)) < 5
        );
    }

    protected JCheckBoxMenuItem getShowDownloadedAreaMenuItem() {
        JPopupMenu menu = this.sourceButton.getPopupMenu();
        boolean afterSeparator = false;
        for (Component c: menu.getComponents()) {
            if (JPopupMenu.Separator.class.isInstance(c)) {
                assertFalse("More than one separator before target item", afterSeparator);
                afterSeparator = true;
            } else if (((JMenuItem) c).getText().equals(tr("Show downloaded area"))) {
                assertTrue("Separator not found before target item", afterSeparator);
                assertTrue("Target item doesn't appear to be a JCheckBoxMenuItem", JCheckBoxMenuItem.class.isInstance(c));
                return (JCheckBoxMenuItem) c;
            }
        }
        fail("'Show downloaded area' menu item not found");
        return null;
    }

    /**
     * test downloaded area is shown shaded
     * @throws Exception if any error occurs
     */
    @Test
    public void testShowDownloadedArea() throws Exception {
        Config.getPref().put("slippy_map_chooser.mapstyle", "Green Tiles");
        Config.getPref().putBoolean("slippy_map_chooser.show_downloaded_area", false);

        DataSet dataSet = new DataSet();
        dataSet.addDataSource(new DataSource(new Bounds(51.725, -0.0209, 51.746, 0.0162), "Somewhere"));

        OsmDataLayer dataLayer = new OsmDataLayer(
            dataSet,
            "Test Layer 123",
            null
        );
        MainApplication.getLayerManager().addLayer(dataLayer);
        MainApplication.getLayerManager().setActiveLayer(dataLayer);

        MapView mapView = MainApplication.getMap().mapView;
        GuiHelper.runInEDTAndWaitWithException(() -> {
            mapView.setVisible(true);
            mapView.addNotify();
            mapView.doLayout();
            mapView.setBounds(0, 0, 500, 500);
        });

        this.setUpMiniMap();

        // assert "show downloaded areas" checkbox is unchecked
        assertFalse(this.getShowDownloadedAreaMenuItem().isSelected());

        // we won't end up with exactly this viewport as it doesn't *precisely* match the aspect ratio
        mapView.zoomTo(new Bounds(51.732, -0.0269, 51.753, 0.0102));

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        Map<Integer, String> paletteMap = new HashMap<Integer, String>() {{
            put(0xff00ff00, "g");  // green
            put(0xff000000, "b");  // black
            put(0xff8ad16b, "v");  // viewport marker inner (pink+green mix)
            put(0xff00df00, "d");  // (shaded green)
            put(0xff8ac46b, "q");  // (shaded pink+green mix)
        }};

        // assert downloaded areas are not drawn
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );

        // enable "show downloaded areas"
        GuiHelper.runInEDTAndWaitWithException(() -> this.getShowDownloadedAreaMenuItem().doClick());
        assertTrue(this.getShowDownloadedAreaMenuItem().isSelected());

        // assert downloaded areas are drawn
        this.paintSlippyMap();

        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^d+bq+v+bg+d+$",
            true
        );
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^d+bq+v+bg+d+$",
            true
        );

        // also assert the leftmost column doesn't (yet) have any downloaded area marks (i.e. fully shaded)
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^d+$",
            true
        );

        // add another downloaded area, going off the left of the widget
        dataSet.addDataSource(new DataSource(new Bounds(51.745, -1., 51.765, 0.0162), "Somewhere else"));
        // and redraw
        this.paintSlippyMap();

        // the middle row should be as before
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^d+bq+v+bg+d+$",
            true
        );
        // the middle column should have its unshaded region extended beyond the viewport marker
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^d+g+bv+bg+d+$",
            true
        );
        // but the leftmost column should now have an unshaded mark
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^d+g+d+$",
            true
        );
        // and the rightmost column should be untouched
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()-1,
            paletteMap,
            "^d+$",
            true
        );

        // and now if we pan to the left (in EastNorth units)
        mapView.zoomTo(mapView.getCenter().add(-5000., 0.));
        // and redraw
        this.paintSlippyMap();

        // the middle row should have its unshaded region outside the viewport marker
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^d+bq+bd+g+d*$",
            true
        );
        // the middle column should have a shaded region inside the viewport marker
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^d+g+bv+q+bd+$",
            true
        );
    }

    /**
     * test display of downloaded area follows active layer switching
     * @throws Exception if any error occurs
     */
    @Test
    public void testShowDownloadedAreaLayerSwitching() throws Exception {
        Config.getPref().put("slippy_map_chooser.mapstyle", "Green Tiles");
        Config.getPref().putBoolean("slippy_map_chooser.show_downloaded_area", true);

        DataSet dataSetA = new DataSet();
        // dataSetA has a long thin horizontal downloaded area (extending off the left & right of the map)
        dataSetA.addDataSource(new DataSource(new Bounds(-18., -61.02, -15., -60.98), "Elsewhere"));

        OsmDataLayer dataLayerA = new OsmDataLayer(
            dataSetA,
            "Test Layer A",
            null
        );
        MainApplication.getLayerManager().addLayer(dataLayerA);

        DataSet dataSetB = new DataSet();
        // dataSetB has a long thin vertical downloaded area (extending off the top & bottom of the map)
        dataSetB.addDataSource(new DataSource(new Bounds(-16.38, -62., -16.34, -60.), "Nowhere"));

        OsmDataLayer dataLayerB = new OsmDataLayer(
            dataSetB,
            "Test Layer B",
            null
        );
        MainApplication.getLayerManager().addLayer(dataLayerB);

        MainApplication.getLayerManager().setActiveLayer(dataLayerB);

        MapView mapView = MainApplication.getMap().mapView;
        GuiHelper.runInEDTAndWaitWithException(() -> {
            mapView.setVisible(true);
            mapView.addNotify();
            mapView.doLayout();
            mapView.setBounds(0, 0, 400, 400);
        });

        this.setUpMiniMap();

        // assert "show downloaded areas" checkbox is checked
        assertTrue(this.getShowDownloadedAreaMenuItem().isSelected());

        // again, we won't end up with exactly this viewport as it doesn't *precisely* match the aspect ratio
        mapView.zoomTo(new Bounds(-16.423, -61.076, -16.299, -60.932));

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        Map<Integer, String> paletteMap = new HashMap<Integer, String>() {{
            put(0xff00ff00, "g");  // green
            put(0xff000000, "b");  // black
            put(0xff8ad16b, "v");  // viewport marker inner (pink+green mix)
            put(0xff00df00, "d");  // (shaded green)
            put(0xff8ac46b, "q");  // (shaded pink+green mix)
        }};

        // the middle row should be entirely unshaded
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );
        // the middle column should have an unshaded band within the viewport marker
        Matcher centerMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^(d+bq+)(v+)(q+bd+)$",
            true
        );
        // the leftmost and rightmost columns should have an unshaded band
        Matcher leftMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^(d+)(g+)(d+)$",
            true
        );
        Matcher rightMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()-1,
            paletteMap,
            "^(d+)(g+)(d+)$",
            true
        );
        // the three columns should have the unshaded band in the same place
        assertEquals(centerMatcher.group(1).length(), leftMatcher.group(1).length());
        assertEquals(centerMatcher.group(1).length(), rightMatcher.group(1).length());
        assertEquals(centerMatcher.group(2).length(), leftMatcher.group(2).length());
        assertEquals(centerMatcher.group(2).length(), rightMatcher.group(2).length());

        // switch active layer
        MainApplication.getLayerManager().setActiveLayer(dataLayerA);
        this.paintSlippyMap();

        // the middle column should be entirely unshaded
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );
        // the middle row should have an unshaded band within the viewport marker
        centerMatcher = ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^(d+bq+)(v+)(q+bd+)$",
            true
        );
        // the topmost and bottommost rows should have an unshaded band
        Matcher topMatcher = ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^(d+)(g+)(d+)$",
            true
        );
        Matcher BottomMatcher = ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()-1,
            paletteMap,
            "^(d+)(g+)(d+)$",
            true
        );
        // the three rows should have the unshaded band in the same place
        assertEquals(centerMatcher.group(1).length(), topMatcher.group(1).length());
        assertEquals(centerMatcher.group(1).length(), BottomMatcher.group(1).length());
        assertEquals(centerMatcher.group(2).length(), topMatcher.group(2).length());
        assertEquals(centerMatcher.group(2).length(), BottomMatcher.group(2).length());

        // deleting dataLayerA should hopefully switch our active layer back to dataLayerB
        MainApplication.getLayerManager().removeLayer(dataLayerA);
        this.paintSlippyMap();

        // now we're really just repeating the same assertions we made originally when dataLayerB was active
        // the middle row should be entirely unshaded
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );
        // the middle column should have an unshaded band within the viewport marker
        centerMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^(d+bq+)(v+)(q+bd+)$",
            true
        );
        // the leftmost and rightmost columns should have an unshaded band
        leftMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^(d+)(g+)(d+)$",
            true
        );
        rightMatcher = ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()-1,
            paletteMap,
            "^(d+)(g+)(d+)$",
            true
        );
        // the three columns should have the unshaded band in the same place
        assertEquals(centerMatcher.group(1).length(), leftMatcher.group(1).length());
        assertEquals(centerMatcher.group(1).length(), rightMatcher.group(1).length());
        assertEquals(centerMatcher.group(2).length(), leftMatcher.group(2).length());
        assertEquals(centerMatcher.group(2).length(), rightMatcher.group(2).length());

        // but now if we expand its downloaded area to cover most of the southern hemisphere...
        dataSetB.addDataSource(new DataSource(new Bounds(-75., -100., 0., 100.), "Everywhere"));
        this.paintSlippyMap();

        // we should see it all as unshaded.
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^g+$",
            true
        );
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );
        ImagePatternMatching.rowMatch(
            paintedSlippyMap,
            paintedSlippyMap.getHeight()-1,
            paletteMap,
            "^g+$",
            true
        );
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            0,
            paletteMap,
            "^g+$",
            true
        );
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()/2,
            paletteMap,
            "^g+bv+bg+$",
            true
        );
        ImagePatternMatching.columnMatch(
            paintedSlippyMap,
            paintedSlippyMap.getWidth()-1,
            paletteMap,
            "^g+$",
            true
        );
    }
}
