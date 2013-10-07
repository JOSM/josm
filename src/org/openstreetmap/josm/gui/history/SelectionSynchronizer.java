// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class SelectionSynchronizer implements ListSelectionListener {

    private List<ListSelectionModel> participants;

    /**
     * Constructs a new {@code SelectionSynchronizer}.
     */
    public SelectionSynchronizer() {
        participants = new ArrayList<ListSelectionModel>();
    }

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
        DefaultListSelectionModel referenceModel = (DefaultListSelectionModel)e.getSource();
        int i = referenceModel.getMinSelectionIndex();
        for (ListSelectionModel model : participants) {
            if (model == e.getSource()) {
                continue;
            }
            model.setSelectionInterval(i,i);
        }
    }
}
