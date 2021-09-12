// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * A convenience list cell renderer to override.
 *
 * @param <E> The type of the ListCellRenderer
 * @since 18221
 */
public class JosmListCellRenderer<E> implements ListCellRenderer<E> {
    protected ListCellRenderer<? super E> renderer;
    protected Component component;

    /**
     * Constructor.
     * @param component the component
     * @param renderer The inner renderer
     */
    public JosmListCellRenderer(Component component, ListCellRenderer<? super E> renderer) {
        this.component = component;
        this.renderer = renderer;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        Component l = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        l.setComponentOrientation(component.getComponentOrientation());
        return l;
    }
}
