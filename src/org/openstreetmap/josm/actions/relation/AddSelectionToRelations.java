// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;


public class AddSelectionToRelations extends AbstractRelationAction implements SelectionChangedListener {
    /**
    * Constructs a new <code>AddSelectionToRelation</code>.
    */
    public AddSelectionToRelations() {
        putValue(SMALL_ICON, ImageProvider.get("dialogs/conflict", "copyendright"));
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset after the last member"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<Command> cmds = new LinkedList<Command>();
        for (Relation orig : relations) {
            Command c = GenericRelationEditor.addPrimitivesToRelation(orig, Main.main.getCurrentDataSet().getSelected());
            if (c != null) {
                cmds.add(c);
            }
        }
        if (!cmds.isEmpty()) {
            Main.main.undoRedo.add(new SequenceCommand(tr("Add selection to relation"), cmds));
        }
    }

    @Override
    public void updateEnabledState() {
        putValue(NAME, trn("Add selection to {0} relation", "Add selection to {0} relations",
                relations.size(), relations.size()));
    }

    @Override
    public void selectionChanged(final Collection<? extends OsmPrimitive> newSelection) {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                setEnabled(newSelection != null && !newSelection.isEmpty());
            }
        });
    }
}
