// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * Just a toggle button, with smaller border and icon only to display in
 * MapFrame toolbars.
 * Also provides methods for storing hidden state in preferences
 * @author imi, akks
 */
public class IconToggleButton extends JToggleButton implements HideableButton, PropertyChangeListener, Destroyable {

    public boolean groupbutton;
    private ShowHideButtonListener listener;

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

    public void destroy() {
        Action action = getAction();
        if (action instanceof Destroyable) {
            ((Destroyable) action).destroy();
        }
        if (action != null) {
            action.removePropertyChangeListener(this);
        }
    }
    
    @Override
    public void applyButtonHiddenPreferences() {
        String actionName = (String) getSafeActionValue(AbstractAction.NAME);
        boolean hiddenFlag = Main.pref.getBoolean(actionName + ".itbutton_hidden", false);
        setVisible(!hiddenFlag);   
    }

    @Override
    public void setButtonHidden(boolean b) {
        String actionName = (String) getSafeActionValue(AbstractAction.NAME);
        setVisible(!b);
        if (listener!=null) { // if someone wants to know about changes of visibility
            if (!b) listener.buttonShown(); else listener.buttonHidden();
        }
        Main.pref.put(actionName + ".itbutton_hidden", b);
    }
    
    @Override
    public void showButton() {
        setButtonHidden(false);
    }
    
    @Override
    public void hideButton() {
        setButtonHidden(true);
    }

    @Override
    public String getActionName() {
        return (String) getSafeActionValue(Action.NAME);
    }

    @Override
    public Icon getIcon() {
        return (Icon) getSafeActionValue(Action.SMALL_ICON);
    }

    @Override
    public boolean isButtonVisible() {
        return isVisible();
    }

    @Override
    public void setShowHideButtonListener(ShowHideButtonListener l) {
        listener = l;
    }

    protected final Object getSafeActionValue(String key) {
        // Mac OS X Aqua L&F can call accessors from constructor, so getAction() can be null in those cases
        return getAction() != null ? getAction().getValue(key) : null;
    }
}
