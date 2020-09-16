// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Obtains the selected key and values from a table and restores those properties on the specified primitive.
 *
 * @since 16593
 */
public class RestorePropertyAction extends AbstractAction {

    private final IntFunction<String> keyFn;
    private final IntFunction<String> valueFn;
    private final Supplier<OsmPrimitive> objectSp;
    private final ListSelectionModel selectionModel;

    /**
     * Constructs a new {@code RestorePropertyAction}.
     *
     * @param keyFn          a function which returns the selected key for a given row index
     * @param valueFn        a function which returns the selected value for a given row index
     * @param objectSp       a supplier which returns the selected tagged object
     * @param selectionModel selection model
     */
    public RestorePropertyAction(IntFunction<String> keyFn, IntFunction<String> valueFn,
                                 Supplier<OsmPrimitive> objectSp, ListSelectionModel selectionModel) {
        super(tr("Restore selected tags"));
        this.keyFn = keyFn;
        this.valueFn = valueFn;
        this.objectSp = objectSp;
        this.selectionModel = selectionModel;
        new ImageProvider("undo").getResource().attachImageIcon(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OsmPrimitive primitive = objectSp.get();
        if (primitive == null) return;

        HashMap<String, String> changes = new HashMap<>();
        for (int index : TableHelper.getSelectedIndices(selectionModel)) {
            changes.put(keyFn.apply(index), valueFn.apply(index));
        }
        ChangePropertyCommand command = new ChangePropertyCommand(Collections.singleton(primitive), changes);
        UndoRedoHandler.getInstance().add(command);
    }
}
