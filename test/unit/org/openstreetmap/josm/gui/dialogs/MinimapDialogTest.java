// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.bbox.SourceButton;
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

    private static void assertSingleSelectedSourceLabel(JPopupMenu menu, String label) {
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

    private static JMenuItem getSourceMenuItemByLabel(JPopupMenu menu, String label) {
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

    /**
     * Tests to switch imagery source.
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourceSwitching() throws Exception {
        MinimapDialog dlg = new MinimapDialog();
        dlg.setSize(300, 200);
        dlg.showDialog();
        SlippyMapBBoxChooser slippyMap = (SlippyMapBBoxChooser) TestUtils.getPrivateField(dlg, "slippyMap");
        SourceButton sourceButton = (SourceButton) TestUtils.getPrivateField(slippyMap, "iSourceButton");

        // get dlg in a paintable state
        dlg.addNotify();
        dlg.doLayout();

        BufferedImage image = new BufferedImage(
            slippyMap.getSize().width,
            slippyMap.getSize().height,
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = image.createGraphics();
        // an initial paint operation is required to trigger the tile fetches
        slippyMap.paintAll(g);
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        slippyMap.paintAll(g);

        assertEquals(0xffffffff, image.getRGB(0, 0));

        assertSingleSelectedSourceLabel(sourceButton.getPopupMenu(), "White Tiles");

        getSourceMenuItemByLabel(sourceButton.getPopupMenu(), "Magenta Tiles").doClick();
        assertSingleSelectedSourceLabel(sourceButton.getPopupMenu(), "Magenta Tiles");
        // call paint to trigger new tile fetch
        slippyMap.paintAll(g);

        // clear background to a recognizably "wrong" color & dispose our Graphics2D so we don't risk carrying over
        // any state
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        slippyMap.paintAll(g);

        assertEquals(0xffff00ff, image.getRGB(0, 0));

        getSourceMenuItemByLabel(sourceButton.getPopupMenu(), "Green Tiles").doClick();
        assertSingleSelectedSourceLabel(sourceButton.getPopupMenu(), "Green Tiles");
        // call paint to trigger new tile fetch
        slippyMap.paintAll(g);

        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        slippyMap.paintAll(g);

        assertEquals(0xff00ff00, image.getRGB(0, 0));
    }
}
