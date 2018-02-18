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
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Add all objects selected in the current dataset after the last member of relation(s).
 * @since 5799
 */
public class AddSelectionToRelations extends AbstractRelationAction implements SelectionChangedListener {
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
        for (Relation orig : relations) {
            Command c = GenericRelationEditor.addPrimitivesToRelation(orig, MainApplication.getLayerManager().getActiveDataSet().getSelected());
            if (c != null) {
                cmds.add(c);
            }
        }
        if (!cmds.isEmpty()) {
            MainApplication.undoRedo.add(new SequenceCommand(tr("Add selection to relation"), cmds));
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
        DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
        if (ds != null) {
            selectionChanged(ds.getSelected());
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void selectionChanged(final Collection<? extends OsmPrimitive> newSelection) {
        GuiHelper.runInEDT(() -> setEnabled(newSelection != null && !newSelection.isEmpty() && !relations.isEmpty()
                && relations.stream().map(Relation::getDataSet).noneMatch(DataSet::isReadOnly)));
    }
}
