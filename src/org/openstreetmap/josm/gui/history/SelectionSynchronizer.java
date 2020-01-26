// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Helper class to ensure that two (or more) {@link javax.swing.JTable}s always
 * have the same entries selected.
 * 
 * The tables are usually displayed side-by-side.
 */
public class SelectionSynchronizer implements ListSelectionListener {

    private final Set<ListSelectionModel> participants;
    private boolean preventRecursion;

    /**
     * Constructs a new {@code SelectionSynchronizer}.
     */
    public SelectionSynchronizer() {
        participants = new HashSet<>();
    }

    /**
     * Add {@link ListSelectionModel} of the table to participate in selection
     * synchronization.
     * 
     * Call this method for all tables that should have their selection synchronized.
     * @param model the selection model of the table
     */
    public void participateInSynchronizedSelection(ListSelectionModel model) {
        if (model == null)
            return;
        if (participants.contains(model))
            return;
        participants.add(model);
        model.addListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (preventRecursion) {
            return;
        }
        preventRecursion = true;
        DefaultListSelectionModel referenceModel = (DefaultListSelectionModel) e.getSource();
        int i = referenceModel.getMinSelectionIndex();
        int j = referenceModel.getMaxSelectionIndex();
        for (ListSelectionModel model : participants) {
            if (model == e.getSource()) {
                continue;
            }
            model.setSelectionInterval(i, j);
        }
        preventRecursion = false;
    }
}
