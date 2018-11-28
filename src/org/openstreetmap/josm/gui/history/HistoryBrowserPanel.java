// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * Superclass of history browsing panels, backed by an {@link HistoryBrowserModel}.
 * @since 14463
 */
public abstract class HistoryBrowserPanel extends JPanel implements Destroyable {

    /** the model */
    protected transient HistoryBrowserModel model;
    /** the common info panel for the history object in role REFERENCE_POINT_IN_TIME */
    protected VersionInfoPanel referenceInfoPanel;
    /** the common info panel for the history object in role CURRENT_POINT_IN_TIME */
    protected VersionInfoPanel currentInfoPanel;

    private final List<JosmAction> josmActions = new ArrayList<>();

    protected HistoryBrowserPanel() {
        super(new GridBagLayout());
    }

    protected void registerAsChangeListener(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.addChangeListener(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.addChangeListener(referenceInfoPanel);
        }
    }

    protected void unregisterAsChangeListener(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.removeChangeListener(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.removeChangeListener(referenceInfoPanel);
        }
    }

    /**
     * Sets the history browsing model for this viewer.
     *
     * @param model the history browsing model
     */
    protected final void setModel(HistoryBrowserModel model) {
        if (this.model != null) {
            unregisterAsChangeListener(this.model);
        }
        this.model = model;
        if (this.model != null) {
            registerAsChangeListener(model);
        }
    }

    protected final <T extends AbstractAction> T trackJosmAction(T action) {
        if (action instanceof JosmAction) {
            josmActions.add((JosmAction) action);
        }
        return action;
    }

    @Override
    public void destroy() {
        setModel(null);
        if (referenceInfoPanel != null)
            referenceInfoPanel.destroy();
        if (currentInfoPanel != null)
            currentInfoPanel.destroy();
        josmActions.forEach(JosmAction::destroy);
        josmActions.clear();
    }
}
