// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Graphics;

import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * A {@link JTabbedPane} extension that completely hides the tab area and border if it contains less than 2 tabs.
 * @since 17314
 */
public class HideableTabbedPane extends JTabbedPane {

    /**
     * Creates an empty <code>HideableTabbedPane</code> with a default tab placement of <code>JTabbedPane.TOP</code>.
     * @see #addTab
     */
    public HideableTabbedPane() {
        initUI();
    }

    /**
     * Creates an empty <code>HideableTabbedPane</code> with the specified tab placement of either:
     * <code>JTabbedPane.TOP</code>, <code>JTabbedPane.BOTTOM</code>, <code>JTabbedPane.LEFT</code>, or <code>JTabbedPane.RIGHT</code>.
     *
     * @param tabPlacement the placement for the tabs relative to the content
     * @see #addTab
     */
    public HideableTabbedPane(int tabPlacement) {
        super(tabPlacement);
        initUI();
    }

    /**
     * Creates an empty <code>TabbedPane</code> with the specified tab placement and tab layout policy. Tab placement may be either:
     * <code>JTabbedPane.TOP</code>, <code>JTabbedPane.BOTTOM</code>, <code>JTabbedPane.LEFT</code>, or <code>JTabbedPane.RIGHT</code>.
     * Tab layout policy may be either: <code>JTabbedPane.WRAP_TAB_LAYOUT</code> or <code>JTabbedPane.SCROLL_TAB_LAYOUT</code>.
     *
     * @param tabPlacement the placement for the tabs relative to the content
     * @param tabLayoutPolicy the policy for laying out tabs when all tabs will not fit on one run
     * @exception IllegalArgumentException if tab placement or tab layout policy are not one of the above supported values
     * @see #addTab
     */
    public HideableTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);
        initUI();
    }

    private void initUI() {
        // See https://stackoverflow.com/a/8897685/2257172
        setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int runCount, int maxTabHeight) {
                return getTabCount() > 1 ? super.calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight) : 0;
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (getTabCount() > 1) {
                    super.paintTabBorder(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
                }
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                if (getTabCount() > 1) {
                    super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);
                }
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                if (getTabCount() > 1) {
                    super.paintContentBorder(g, tabPlacement, selectedIndex);
                }
            }
        });
    }
}
