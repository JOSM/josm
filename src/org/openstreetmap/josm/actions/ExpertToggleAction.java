// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonModel;

import org.openstreetmap.josm.Main;

public class ExpertToggleAction extends JosmAction {

    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    private boolean selected;

    public interface ExpertModeChangeListener {
        void expertChanged(boolean isExpert);
    }

    private static final List<WeakReference<ExpertModeChangeListener>> listeners = new ArrayList<WeakReference<ExpertModeChangeListener>>();
    private static final List<WeakReference<Component>> visibilityToggleListeners = new ArrayList<WeakReference<Component>>();

    private static ExpertToggleAction INSTANCE = new ExpertToggleAction();

    private synchronized static void fireExpertModeChanged(boolean isExpert) {
        {
            Iterator<WeakReference<ExpertModeChangeListener>> it = listeners.iterator();
            while (it.hasNext()) {
                WeakReference<ExpertModeChangeListener> wr = it.next();
                ExpertModeChangeListener listener = wr.get();
                if (listener == null) {
                    it.remove();
                    continue;
                }
                listener.expertChanged(isExpert);
            }
        }
        {
            Iterator<WeakReference<Component>> it = visibilityToggleListeners.iterator();
            while (it.hasNext()) {
                WeakReference<Component> wr = it.next();
                Component c = wr.get();
                if (c == null) {
                    it.remove();
                    continue;
                }
                c.setVisible(isExpert);
            }
        }
    }

    /**
     * Register a expert mode change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static void addExpertModeChangeListener(ExpertModeChangeListener listener) {
        addExpertModeChangeListener(listener, false);
    }

    public synchronized static void addExpertModeChangeListener(ExpertModeChangeListener listener, boolean fireWhenAdding) {
        if (listener == null) return;
        for (WeakReference<ExpertModeChangeListener> wr : listeners) {
            // already registered ? => abort
            if (wr.get() == listener) return;
        }
        listeners.add(new WeakReference<ExpertModeChangeListener>(listener));
        if (fireWhenAdding) {
            listener.expertChanged(isExpert());
        }
    }

    /**
     * Removes a expert mode change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public synchronized static void removeExpertModeChangeListener(ExpertModeChangeListener listener) {
        if (listener == null) return;
        Iterator<WeakReference<ExpertModeChangeListener>> it = listeners.iterator();
        while (it.hasNext()) {
            WeakReference<ExpertModeChangeListener> wr = it.next();
            // remove the listener - and any other listener which god garbage
            // collected in the meantime
            if (wr.get() == null || wr.get() == listener) {
                it.remove();
            }
        }
    }

    public synchronized static void addVisibilitySwitcher(Component c) {
        if (c == null) return;
        for (WeakReference<Component> wr : visibilityToggleListeners) {
            // already registered ? => abort
            if (wr.get() == c) return;
        }
        visibilityToggleListeners.add(new WeakReference<Component>(c));
        c.setVisible(isExpert());
    }

    public synchronized static void removeVisibilitySwitcher(Component c) {
        if (c == null) return;
        Iterator<WeakReference<Component>> it = visibilityToggleListeners.iterator();
        while (it.hasNext()) {
            WeakReference<Component> wr = it.next();
            // remove the listener - and any other listener which god garbage
            // collected in the meantime
            if (wr.get() == null || wr.get() == c) {
                it.remove();
            }
        }
    }

    public ExpertToggleAction() {
        super(
                tr("Expert Mode"),
                "expert",
                tr("Enable/disable expert mode"),
                null,
                false /* register toolbar */
        );
        putValue("toolbar", "expertmode");
        Main.toolbar.register(this);
        selected = Main.pref.getBoolean("expert", false);
        notifySelectedState();
    }

    public void addButtonModel(ButtonModel model) {
        if (model != null && !buttonModels.contains(model)) {
            buttonModels.add(model);
            model.setSelected(selected);
        }
    }

    public void removeButtonModel(ButtonModel model) {
        if (model != null && buttonModels.contains(model)) {
            buttonModels.remove(model);
        }
    }

    protected void notifySelectedState() {
        for (ButtonModel model: buttonModels) {
            if (model.isSelected() != selected) {
                model.setSelected(selected);
            }
        }
        fireExpertModeChanged(selected);
    }

    protected void toggleSelectedState() {
        selected = !selected;
        Main.pref.put("expert", selected);
        notifySelectedState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }

    public boolean isSelected() {
        return selected;
    }

    public static ExpertToggleAction getInstance() {
        return INSTANCE;
    }

    public static boolean isExpert() {
        return INSTANCE.isSelected();
    }
}
