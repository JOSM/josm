// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

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

    private synchronized static void fireExpertModeChanged(boolean isExpert) {
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
            listener.expertChanged(Main.main.menu.expert.isSelected());
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

    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }

    public boolean isSelected() {
        return selected;
    }
}
