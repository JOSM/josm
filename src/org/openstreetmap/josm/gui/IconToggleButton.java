// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JToggleButton;

/**
 * Just a toggle button, with smaller border and icon only to display in
 * MapFrame toolbars.
 *
 * @author imi
 */
public class IconToggleButton extends JToggleButton implements PropertyChangeListener {

    public boolean groupbutton;

    /**
     * Construct the toggle button with the given action.
     */
    public IconToggleButton(Action action) {
        super(action);
        setText(null);

        Object o = action.getValue(Action.SHORT_DESCRIPTION);
        if (o != null) {
            setToolTipText(o.toString());
        }

        action.addPropertyChangeListener(this);

        addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e) {
                groupbutton = e.getX() > getWidth()/2 && e.getY() > getHeight()/2;
            }
        });
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("active")) {
            setSelected((Boolean)evt.getNewValue());
            requestFocusInWindow();
        } else if (evt.getPropertyName().equals("selected")) {
            setSelected((Boolean)evt.getNewValue());
        }
    }
}
