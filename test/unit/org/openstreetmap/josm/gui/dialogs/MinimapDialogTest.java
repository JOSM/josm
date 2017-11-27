// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.bbox.SourceButton;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.awaitility.Awaitility;

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

    protected void assertSingleSelectedSourceLabel(String label) {
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

    protected JMenuItem getSourceMenuItemByLabel(String label) {
        JPopupMenu menu = this.sourceButton.getPopupMenu();
        for (Component c: menu.getComponents()) {
            if (JPopupMenu.Separator.class.isInstance(c)) {
                break;
            } else if (((JMenuItem) c).getText() == label) {
                return (JMenuItem) c;
            }
            // else continue...
        }
        fail("Failed to find menu item with label " + label);
        return null;
    }

    protected MinimapDialog minimap;
    protected SlippyMapBBoxChooser slippyMap;
    protected SourceButton sourceButton;
    protected Callable<Boolean> slippyMapTasksFinished;

    protected static BufferedImage paintedSlippyMap;

    protected void setUpMiniMap() throws Exception {
        this.minimap = new MinimapDialog();
        this.minimap.setSize(300, 200);
        this.minimap.showDialog();
        this.slippyMap = (SlippyMapBBoxChooser) TestUtils.getPrivateField(this.minimap, "slippyMap");
        this.sourceButton = (SourceButton) TestUtils.getPrivateField(this.slippyMap, "iSourceButton");

        this.slippyMapTasksFinished = () -> !this.slippyMap.getTileController().getTileLoader().hasOutstandingTasks();

        // get minimap in a paintable state
        this.minimap.addNotify();
        this.minimap.doLayout();
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

        this.getSourceMenuItemByLabel("Magenta Tiles").doClick();
        this.assertSingleSelectedSourceLabel("Magenta Tiles");
        // call paint to trigger new tile fetch
        this.paintSlippyMap();

        Awaitility.await().atMost(1000, MILLISECONDS).until(this.slippyMapTasksFinished);

        this.paintSlippyMap();

        assertEquals(0xffff00ff, paintedSlippyMap.getRGB(0, 0));

        this.getSourceMenuItemByLabel("Green Tiles").doClick();
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

        this.getSourceMenuItemByLabel("Magenta Tiles").doClick();
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
}
