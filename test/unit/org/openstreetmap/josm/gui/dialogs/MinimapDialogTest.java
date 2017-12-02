// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.bbox.SourceButton;
import org.openstreetmap.josm.gui.layer.LayerManagerTest.TestLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.testutils.ImagePatternMatching;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.google.common.collect.ImmutableMap;

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
    public JOSMTestRules josmTestRules = new JOSMTestRules().main().platform().projection().fakeImagery();

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
                boolean equalText = ((JMenuItem) c).getText() == label;
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
                    } else if (((JMenuItem) c).getText() == label) {
                        ((JMenuItem) c).doClick();
                        return;
                    }
                    // else continue...
                }
                fail();
            });
        } catch (Throwable e) {
            // need to turn this *back* into an AssertionFailedError
            fail(String.format("Failed to find menu item with label %s: %s", label, e));
        }
    }

    protected MinimapDialog minimap;
    protected SlippyMapBBoxChooser slippyMap;
    protected SourceButton sourceButton;
    protected Callable<Boolean> slippyMapTasksFinished;

    protected static BufferedImage paintedSlippyMap;

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

        assertEquals("Green Tiles", Main.pref.get("slippy_map_chooser.mapstyle", "Fail"));
    }

    /**
     * Tests minimap obeys a saved "mapstyle" preference on startup.
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourcePrefObeyed() throws Exception {
        Main.pref.put("slippy_map_chooser.mapstyle", "Green Tiles");

        this.setUpMiniMap();

        this.assertSingleSelectedSourceLabel("Green Tiles");

        // an initial paint operation is required to trigger the tile fetches
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xff00ff00, paintedSlippyMap.getRGB(0, 0));

        this.clickSourceMenuItemByLabel("Magenta Tiles");
        this.assertSingleSelectedSourceLabel("Magenta Tiles");

        assertEquals("Magenta Tiles", Main.pref.get("slippy_map_chooser.mapstyle", "Fail"));
    }

    /**
     * Tests minimap handles an unrecognized "mapstyle" preference on startup
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourcePrefInvalid() throws Exception {
        Main.pref.put("slippy_map_chooser.mapstyle", "Hooloovoo Tiles");

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

        Main.pref.put("slippy_map_chooser.mapstyle", "White Tiles");
        // ensure projection matches JMapViewer's
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));

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

        Map<Integer, String> paletteMap = ImmutableMap.<Integer, String>builder()
            .put(0xffffffff, "w")
            .put(0xff000000, "b")
            .put(0xfff0d1d1, "p")
            .build();

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
}
