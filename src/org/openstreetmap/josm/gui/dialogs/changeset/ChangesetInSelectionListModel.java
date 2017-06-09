// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.osm.DataSelectionListener;

/**
 * A table of changesets that displays the ones that are used by the primitives in the current selection.
 */
public class ChangesetInSelectionListModel extends ChangesetListModel implements DataSelectionListener {

    /**
     * Create a new {@link ChangesetInSelectionListModel}
     * @param selectionModel The model
     */
    public ChangesetInSelectionListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface DataSelectionListener                                              */
    /* ---------------------------------------------------------------------------- */

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        initFromPrimitives(event.getSelection());
    }
}
