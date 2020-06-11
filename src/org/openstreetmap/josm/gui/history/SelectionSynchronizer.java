// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.gui.util.TableHelper;

/**
 * Helper class to ensure that two (or more) {@link javax.swing.JTable}s always
 * have the same entries selected.
 * 
 * The tables are usually displayed side-by-side.
 */
public class SelectionSynchronizer implements ListSelectionListener {

    private final Set<ListSelectionModel> participants;
    private boolean preventRecursion;
    private BiFunction<Integer, ListSelectionModel, IntStream> selectionIndexMapper = (i, model) -> IntStream.of(i);

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

    void setSelectionIndexMapper(BiFunction<Integer, ListSelectionModel, IntStream> selectionIndexMapper) {
        this.selectionIndexMapper = Objects.requireNonNull(selectionIndexMapper);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (preventRecursion) {
            return;
        }
        preventRecursion = true;
        ListSelectionModel referenceModel = (ListSelectionModel) e.getSource();
        int[] selectedIndices = TableHelper.getSelectedIndices(referenceModel);
        for (ListSelectionModel model : participants) {
            if (model == referenceModel) {
                continue;
            }
            TableHelper.setSelectedIndices(model,
                    Arrays.stream(selectedIndices).flatMap(i -> selectionIndexMapper.apply(i, referenceModel)));
        }
        preventRecursion = false;
    }
}
