// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSplitPane;

/**
 * Auto adjusting split pane when parent is resized.
 * @since 11772 (extracted from {@code CombinePrimitiveResolverDialog})
 */
public class AutoAdjustingSplitPane extends JSplitPane implements PropertyChangeListener, HierarchyBoundsListener {
    private double dividerLocation;

    /**
     * Constructs a new {@code AutoAdjustingSplitPane}.
     * @param newOrientation {@code JSplitPane.HORIZONTAL_SPLIT} or {@code JSplitPane.VERTICAL_SPLIT}
     */
    public AutoAdjustingSplitPane(int newOrientation) {
        super(newOrientation);
        addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, this);
        addHierarchyBoundsListener(this);
    }

    @Override
    public void ancestorResized(HierarchyEvent e) {
        setDividerLocation((int) (dividerLocation * getHeight()));
    }

    @Override
    public void ancestorMoved(HierarchyEvent e) {
        // do nothing
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName())) {
            int newVal = (Integer) evt.getNewValue();
            if (getHeight() != 0) {
                dividerLocation = (double) newVal / (double) getHeight();
            }
        }
    }
}
