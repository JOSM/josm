// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.function.Function;

/**
 * This is an extension of the flow layout that prefers wrapping the text instead of increasing the component width
 * when there is not enough space.
 * <p>
 * This allows for a better preferred size computation.
 * It should be used in all places where a flow layout fills the full width of the parent container.
 * <p>
 * This does not support baseline alignment.
 * @author Michael Zangl
 * @since 10622
 */
public class MultiLineFlowLayout extends FlowLayout {
    /**
     * Same as {@link FlowLayout#FlowLayout()}
     */
    public MultiLineFlowLayout() {
        super();
    }

    /**
     * Same as {@link FlowLayout#FlowLayout(int, int, int)}
     * @param align Alignment
     * @param hgap horizontal gap
     * @param vgap vertical gap
     */
    public MultiLineFlowLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * Same as {@link FlowLayout#FlowLayout(int)}
     * @param align Alignment
     */
    public MultiLineFlowLayout(int align) {
        super(align);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return getLayoutSize(target, Component::getPreferredSize);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return getLayoutSize(target, Component::getMinimumSize);
    }

    private Dimension getLayoutSize(Container target, Function<Component, Dimension> baseSize) {
        synchronized (target.getTreeLock()) {
            int outerWidth = getWidthOf(target);

            Insets insets = target.getInsets();
            int containerWidth = outerWidth - insets.left - insets.right - getHgap() * 2;

            int x = 0;
            int totalHeight = insets.top + insets.bottom + getVgap() * 2;
            int rowHeight = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component child = target.getComponent(i);
                if (!child.isVisible()) {
                    continue;
                }
                Dimension size = baseSize.apply(child);
                if (x != 0) {
                    x += getHgap();
                }
                x += size.width;
                if (x > containerWidth) {
                    totalHeight += rowHeight + getVgap();
                    rowHeight = 0;
                    x = 0;
                }

                rowHeight = Math.max(rowHeight, size.height);
            }
            totalHeight += rowHeight;

            return new Dimension(outerWidth, totalHeight);
        }
    }

    private static int getWidthOf(Container target) {
        Container current = target;
        while (current.getWidth() == 0 && current.getParent() != null) {
            current = current.getParent();
        }
        int width = current.getWidth();
        if (width == 0) {
            return Integer.MAX_VALUE;
        } else {
            return width;
        }
    }

    @Override
    public String toString() {
        return "MultiLineFlowLayout [align=" + getAlignment() + ']';
    }
}
