// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command used to add a new conflict.
 * @since 1857
 */
public class ConflictAddCommand extends Command {
    private Conflict<? extends OsmPrimitive> conflict;

    /**
     * Constructs a new {@code ConflictAddCommand}.
     * @param layer the data layer. Must not be null.
     * @param conflict the conflict to add
     */
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

    @Override
    public boolean executeCommand() {
        try {
            getLayer().getConflicts().add(conflict);
        } catch (IllegalStateException e) {
            Main.error(e);
            warnBecauseOfDoubleConflict();
        }
        return true;
    }

    @Override
    public void undoCommand() {
        if (!Main.map.mapView.hasLayer(getLayer())) {
            Main.warn(tr("Layer ''{0}'' does not exist any more. Cannot remove conflict for object ''{1}''.",
                    getLayer().getName(),
                    conflict.getMy().getDisplayName(DefaultNameFormatter.getInstance())
            ));
            return;
        }
        getLayer().getConflicts().remove(conflict);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((conflict == null) ? 0 : conflict.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConflictAddCommand other = (ConflictAddCommand) obj;
        if (conflict == null) {
            if (other.conflict != null)
                return false;
        } else if (!conflict.equals(other.conflict))
            return false;
        return true;
    }
}
