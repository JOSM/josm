// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Command used to add a new conflict.
 * @since 1857
 */
public class ConflictAddCommand extends Command {
    private final Conflict<? extends OsmPrimitive> conflict;

    /**
     * Constructs a new {@code ConflictAddCommand}.
     * @param ds the data set. Must not be null.
     * @param conflict the conflict to add
     * @since 12672
     */
    public ConflictAddCommand(DataSet ds, Conflict<? extends OsmPrimitive> conflict) {
        super(ds);
        this.conflict = conflict;
    }

    protected void warnBecauseOfDoubleConflict() {
        JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                tr("<html>Layer ''{0}'' already has a conflict for object<br>"
                        + "''{1}''.<br>"
                        + "This conflict cannot be added.</html>",
                        Utils.escapeReservedCharactersHTML(getAffectedDataSet().getName()),
                        Utils.escapeReservedCharactersHTML(conflict.getMy().getDisplayName(DefaultNameFormatter.getInstance()))
                ),
                tr("Double conflict"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public boolean executeCommand() {
        try {
            getAffectedDataSet().getConflicts().add(conflict);
        } catch (IllegalStateException e) {
            Logging.error(e);
            warnBecauseOfDoubleConflict();
        }
        return true;
    }

    @Override
    public void undoCommand() {
        DataSet ds = getAffectedDataSet();
        if (!OsmDataManager.getInstance().containsDataSet(ds)) {
            Logging.warn(tr("Layer ''{0}'' does not exist any more. Cannot remove conflict for object ''{1}''.",
                    ds.getName(),
                    conflict.getMy().getDisplayName(DefaultNameFormatter.getInstance())
            ));
            return;
        }
        ds.getConflicts().remove(conflict);
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
        return Objects.hash(super.hashCode(), conflict);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ConflictAddCommand that = (ConflictAddCommand) obj;
        return Objects.equals(conflict, that.conflict);
    }
}
