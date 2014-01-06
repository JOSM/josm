// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

public class ConflictAddCommand extends Command {
    private Conflict<? extends OsmPrimitive> conflict;

    public ConflictAddCommand(OsmDataLayer layer, Conflict<? extends OsmPrimitive> conflict) {
        super(layer);
        this.conflict  = conflict;
    }

    protected void warnBecauseOfDoubleConflict() {
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("<html>Layer ''{0}'' already has a conflict for object<br>"
                        + "''{1}''.<br>"
                        + "This conflict cannot be added.</html>",
                        getLayer().getName(),
                        conflict.getMy().getDisplayName(DefaultNameFormatter.getInstance())
                ),
                tr("Double conflict"),
                JOptionPane.ERROR_MESSAGE
        );
    }
    @Override public boolean executeCommand() {
        try {
            getLayer().getConflicts().add(conflict);
        } catch(IllegalStateException e) {
            Main.error(e);
            warnBecauseOfDoubleConflict();
        }
        return true;
    }

    @Override public void undoCommand() {
        if (! Main.map.mapView.hasLayer(getLayer())) {
            Main.warn(tr("Layer ''{0}'' does not exist any more. Cannot remove conflict for object ''{1}''.",
                    getLayer().getName(),
                    conflict.getMy().getDisplayName(DefaultNameFormatter.getInstance())
            ));
            return;
        }
        getLayer().getConflicts().remove(conflict);
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        // nothing to fill
    }

    @Override
    public String getDescriptionText() {
        return tr("Add conflict for ''{0}''",
                conflict.getMy().getDisplayName(DefaultNameFormatter.getInstance()));
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get(conflict.getMy().getDisplayType());
    }
}
