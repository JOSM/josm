// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.tools.ListenerList;

/**
 * This action toggles the Expert mode.
 * @since 4840
 */
public class ExpertToggleAction extends ToggleAction {

    /**
     * This listener is notified whenever the expert mode setting changed.
     */
    @FunctionalInterface
    public interface ExpertModeChangeListener {
        /**
         * The expert mode changed.
         * @param isExpert <code>true</code> if expert mode was enabled, false otherwise.
         */
        void expertChanged(boolean isExpert);
    }

    // TODO: Switch to checked list. We can do this as soon as we do not see any more warnings.
    private static final ListenerList<ExpertModeChangeListener> listeners = ListenerList.createUnchecked();
    private static final ListenerList<Component> visibilityToggleListeners = ListenerList.createUnchecked();

    private static final BooleanProperty PREF_EXPERT = new BooleanProperty("expert", false);

    private static final ExpertToggleAction INSTANCE = new ExpertToggleAction();

    private static synchronized void fireExpertModeChanged(boolean isExpert) {
        listeners.fireEvent(listener -> listener.expertChanged(isExpert));
        visibilityToggleListeners.fireEvent(c -> c.setVisible(isExpert));
    }

    /**
     * Register a expert mode change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static void addExpertModeChangeListener(ExpertModeChangeListener listener) {
        addExpertModeChangeListener(listener, false);
    }

    public static synchronized void addExpertModeChangeListener(ExpertModeChangeListener listener, boolean fireWhenAdding) {
        if (listener == null) return;
        listeners.addWeakListener(listener);
        if (fireWhenAdding) {
            listener.expertChanged(isExpert());
        }
    }

    /**
     * Removes a expert mode change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static synchronized void removeExpertModeChangeListener(ExpertModeChangeListener listener) {
        if (listener == null) return;
        listeners.removeListener(listener);
    }

    /**
     * Marks a component to be only visible when expert mode is enabled. The visibility of the component is changed automatically.
     * @param c The component.
     */
    public static synchronized void addVisibilitySwitcher(Component c) {
        if (c == null) return;
        visibilityToggleListeners.addWeakListener(c);
        c.setVisible(isExpert());
    }

    /**
     * Stops tracking visibility changes for the given component.
     * @param c The component.
     * @see #addVisibilitySwitcher(Component)
     */
    public static synchronized void removeVisibilitySwitcher(Component c) {
        if (c == null) return;
        visibilityToggleListeners.removeListener(c);
    }

    /**
     * Constructs a new {@code ExpertToggleAction}.
     */
    public ExpertToggleAction() {
        super(tr("Expert Mode"),
              "expert",
              tr("Enable/disable expert mode"),
              null,
              false /* register toolbar */
        );
        putValue("toolbar", "expertmode");
        if (Main.toolbar != null) {
            Main.toolbar.register(this);
        }
        setSelected(PREF_EXPERT.get());
        notifySelectedState();
    }

    @Override
    protected final void notifySelectedState() {
        super.notifySelectedState();
        PREF_EXPERT.put(isSelected());
        fireExpertModeChanged(isSelected());
    }

    /**
     * Forces the expert mode state to the given state.
     * @param isExpert if expert mode should be used.
     * @since 11224
     */
    public void setExpert(boolean isExpert) {
        if (isSelected() != isExpert) {
            setSelected(isExpert);
            notifySelectedState();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        notifySelectedState();
    }

    /**
     * Replies the unique instance of this action.
     * @return The unique instance of this action
     */
    public static ExpertToggleAction getInstance() {
        return INSTANCE;
    }

    /**
     * Determines if expert mode is enabled.
     * @return {@code true} if expert mode is enabled, {@code false} otherwise.
     */
    public static boolean isExpert() {
        return INSTANCE.isSelected();
    }
}
