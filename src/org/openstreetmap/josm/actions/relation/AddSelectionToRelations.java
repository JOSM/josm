// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * Add all objects selected in the current dataset after the last member of relation(s).
 * @since 5799
 */
public class AddSelectionToRelations extends AbstractRelationAction implements DataSelectionListener {
    /**
    * Constructs a new <code>AddSelectionToRelation</code>.
    */
    public AddSelectionToRelations() {
        new ImageProvider("dialogs/conflict", "copyendright").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset after the last member"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<Command> cmds = new LinkedList<>();
        for (Relation orig : Utils.filteredCollection(relations, Relation.class)) {
            Command c = GenericRelationEditor.addPrimitivesToRelation(orig, MainApplication.getLayerManager().getActiveDataSet().getSelected());
            if (c != null) {
                cmds.add(c);
            }
        }
        if (!cmds.isEmpty()) {
            UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Add selection to relation"), cmds));
            new Notification(
                    "<html>"+
                    tr("{0}Add selection to relation{1}: Verify every single relation to avoid damage!", "<strong>", "</strong>")+
                    "</html>")
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
        }
    }

    @Override
    public void updateEnabledState() {
        int size = relations.size();
        putValue(NAME, trn("Add selection to {0} relation", "Add selection to {0} relations", size, size));
        OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
        if (ds != null) {
            selectionChanged(ds.getSelected());
        } else {
            setEnabled(false);
        }
    }

    private void selectionChanged(final Collection<? extends IPrimitive> newSelection) {
        GuiHelper.runInEDT(() -> setEnabled(newSelection != null && !newSelection.isEmpty()
                && OsmUtils.isOsmCollectionEditable(relations)));
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        selectionChanged(event.getSelection());
    }
}
