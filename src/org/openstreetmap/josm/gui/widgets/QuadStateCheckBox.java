// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;

import org.openstreetmap.josm.tools.Utils;

/**
 * A four-state checkbox. The states are enumerated in {@link State}.
 * @since 591
 */
public class QuadStateCheckBox extends JCheckBox {

    /**
     * The 4 possible states of this checkbox.
     */
    public enum State {
        /** Not selected: the property is explicitly switched off */
        NOT_SELECTED,
        /** Selected: the property is explicitly switched on */
        SELECTED,
        /** Unset: do not set this property on the selected objects */
        UNSET,
        /** Partial: different selected objects have different values, do not change */
        PARTIAL
    }

    private final transient QuadStateDecorator model;
    private State[] allowed;

    /**
     * Constructs a new {@code QuadStateCheckBox}.
     * @param text the text of the check box
     * @param icon the Icon image to display
     * @param initial The initial state
     * @param allowed The allowed states
     */
    public QuadStateCheckBox(String text, Icon icon, State initial, State[] allowed) {
        super(text, icon);
        this.allowed = Utils.copyArray(allowed);
        // Add a listener for when the mouse is pressed
        super.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                grabFocus();
                model.nextState();
            }
        });
        // Reset the keyboard action map
        ActionMap map = new ActionMapUIResource();
        map.put("pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                grabFocus();
                model.nextState();
            }
        });
        map.put("released", null);
        SwingUtilities.replaceUIActionMap(this, map);
        // set the model to the adapted model
        model = new QuadStateDecorator(getModel());
        setModel(model);
        setState(initial);
    }

    /**
     * Constructs a new {@code QuadStateCheckBox}.
     * @param text the text of the check box
     * @param initial The initial state
     * @param allowed The allowed states
     */
    public QuadStateCheckBox(String text, State initial, State[] allowed) {
        this(text, null, initial, allowed);
    }

    /** Do not let anyone add mouse listeners */
    @Override
    public void addMouseListener(MouseListener l) { }

    /**
     * Set the new state.
     * @param state The new state
     */
    public final void setState(State state) {
        model.setState(state);
    }

    /**
     * Return the current state, which is determined by the selection status of the model.
     * @return The current state
     */
    public State getState() {
        return model.getState();
    }

    @Override
    public void setSelected(boolean b) {
        if (b) {
            setState(State.SELECTED);
        } else {
            setState(State.NOT_SELECTED);
        }
    }

    private final class QuadStateDecorator implements ButtonModel {
        private final ButtonModel other;

        private QuadStateDecorator(ButtonModel other) {
            this.other = other;
        }

        private void setState(State state) {
            if (state == State.NOT_SELECTED) {
                other.setArmed(false);
                other.setPressed(false);
                other.setSelected(false);
                setToolTipText(tr("false: the property is explicitly switched off"));
            } else if (state == State.SELECTED) {
                other.setArmed(false);
                other.setPressed(false);
                other.setSelected(true);
                setToolTipText(tr("true: the property is explicitly switched on"));
            } else if (state == State.PARTIAL) {
                other.setArmed(true);
                other.setPressed(true);
                other.setSelected(true);
                setToolTipText(tr("partial: different selected objects have different values, do not change"));
            } else {
                other.setArmed(true);
                other.setPressed(true);
                other.setSelected(false);
                setToolTipText(tr("unset: do not set this property on the selected objects"));
            }
        }

        /**
         * The current state is embedded in the selection / armed
         * state of the model.
         *
         * We return the SELECTED state when the checkbox is selected
         * but not armed, PARTIAL state when the checkbox is
         * selected and armed (grey) and NOT_SELECTED when the
         * checkbox is deselected.
         */
        private State getState() {
            if (isSelected() && !isArmed()) {
                // normal black tick
                return State.SELECTED;
            } else if (isSelected() && isArmed()) {
                // don't care grey tick
                return State.PARTIAL;
            } else if (!isSelected() && !isArmed()) {
                return State.NOT_SELECTED;
            } else {
                return State.UNSET;
            }
        }
        /** Rotate to the next allowed state.*/
        private void nextState() {
            State current = getState();
            for (int i = 0; i < allowed.length; i++) {
                if (allowed[i] == current) {
                    setState((i == allowed.length-1) ? allowed[0] : allowed[i+1]);
                    break;
                }
            }
        }
        /** Filter: No one may change the armed/selected/pressed status except us. */
        @Override public void setArmed(boolean b) { }
        @Override public void setSelected(boolean b) { }
        @Override public void setPressed(boolean b) { }
        /** We disable focusing on the component when it is not enabled. */
        @Override public void setEnabled(boolean b) {
            setFocusable(b);
            other.setEnabled(b);
        }
        /** All these methods simply delegate to the "other" model
         * that is being decorated. */
        @Override public boolean isArmed() { return other.isArmed(); }
        @Override public boolean isSelected() { return other.isSelected(); }
        @Override public boolean isEnabled() { return other.isEnabled(); }
        @Override public boolean isPressed() { return other.isPressed(); }
        @Override public boolean isRollover() { return other.isRollover(); }
        @Override public void setRollover(boolean b) { other.setRollover(b); }
        @Override public void setMnemonic(int key) { other.setMnemonic(key); }
        @Override public int getMnemonic() { return other.getMnemonic(); }
        @Override public void setActionCommand(String s) {
            other.setActionCommand(s);
        }
        @Override public String getActionCommand() {
            return other.getActionCommand();
        }
        @Override public void setGroup(ButtonGroup group) {
            other.setGroup(group);
        }
        @Override public void addActionListener(ActionListener l) {
            other.addActionListener(l);
        }
        @Override public void removeActionListener(ActionListener l) {
            other.removeActionListener(l);
        }
        @Override public void addItemListener(ItemListener l) {
            other.addItemListener(l);
        }
        @Override public void removeItemListener(ItemListener l) {
            other.removeItemListener(l);
        }
        @Override public void addChangeListener(ChangeListener l) {
            other.addChangeListener(l);
        }
        @Override public void removeChangeListener(ChangeListener l) {
            other.removeChangeListener(l);
        }
        @Override public Object[] getSelectedObjects() {
            return other.getSelectedObjects();
        }
    }
}
