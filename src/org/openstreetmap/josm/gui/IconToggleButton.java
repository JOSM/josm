// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * Just a toggle button, with smaller border and icon only to display in
 * MapFrame toolbars.
 * Also provides methods for storing hidden state in preferences
 * @author imi, akks
 */
public class IconToggleButton extends JToggleButton implements HideableButton, PropertyChangeListener, Destroyable, ExpertModeChangeListener {

    public boolean groupbutton;
    private ShowHideButtonListener listener;
    private boolean hideIfDisabled = false;
    private boolean isExpert;

    /**
     * Construct the toggle button with the given action.
     */
    public IconToggleButton(Action action) {
        this(action, false);
    }

    /**
     * Construct the toggle button with the given action.
     */
    public IconToggleButton(Action action, boolean isExpert) {
        super(action);
        this.isExpert = isExpert;
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

        ExpertToggleAction.addExpertModeChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("active")) {
            setSelected((Boolean)evt.getNewValue());
            requestFocusInWindow();
        } else if (evt.getPropertyName().equals("selected")) {
            setSelected((Boolean)evt.getNewValue());
        }
    }

    @Override
    public void destroy() {
        Action action = getAction();
        if (action instanceof Destroyable) {
            ((Destroyable) action).destroy();
        }
        if (action != null) {
            action.removePropertyChangeListener(this);
        }
    }

    String getPreferenceKey() {
        String s = (String) getSafeActionValue("toolbar");
        if (s == null) {
            if (getAction()!=null) {
                s = getAction().getClass().getName();
            }
        }
        return "sidetoolbar.hidden."+s;

    }

    @Override
    public void expertChanged(boolean isExpert) {
        applyButtonHiddenPreferences();
    }

    @Override
    public void applyButtonHiddenPreferences() {
        boolean alwaysHideDisabled = Main.pref.getBoolean("sidetoolbar.hideDisabledButtons", false);
        if (!isEnabled() && (hideIfDisabled || alwaysHideDisabled)) {
            setVisible(false);  // hide because of disabled button
        } else {
            boolean hiddenFlag = false;
            String hiddenFlagStr = Main.pref.get(getPreferenceKey(), null);
            if (hiddenFlagStr == null) {
                if (isExpert && !ExpertToggleAction.isExpert()) {
                    hiddenFlag = true;
                }
            } else {
                hiddenFlag = Boolean.parseBoolean(hiddenFlagStr);
            }
            setVisible( !hiddenFlag ); // show or hide, do what preferences say
        }
    }

    @Override
    public void setButtonHidden(boolean b) {
        setVisible(!b);
        if (listener!=null) { // if someone wants to know about changes of visibility
            if (!b) listener.buttonShown(); else listener.buttonHidden();
        }
        if ((b && isExpert && !ExpertToggleAction.isExpert()) ||
            (!b && isExpert && ExpertToggleAction.isExpert())) {
            Main.pref.put(getPreferenceKey(), null);
        } else {
            Main.pref.put(getPreferenceKey(), b);
        }
    }

    /*
     * This fuction should be called for plugins that want to enable auto-hiding
     * custom buttons when they are disabled (because of incorrect layer, for example)
     */
    public void setAutoHideDisabledButton(boolean b) {
        hideIfDisabled = b;
        if (b && !isEnabled()) {
            setVisible(false);
        }
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
