// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a command for resolving a version conflict between two {@see OsmPrimitive}
 *
 *
 */
public class VersionConflictResolveCommand extends ConflictResolveCommand {

    /** the conflict to resolve */
    private Conflict<OsmPrimitive> conflict;

    /**
     * constructor
     * @param my  my primitive (i.e. the primitive from the local dataset)
     * @param their their primitive (i.e. the primitive from the server)
     */
    public VersionConflictResolveCommand(OsmPrimitive my, OsmPrimitive their) {
        conflict = new Conflict<OsmPrimitive>(my, their);
    }

    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve version conflicts for {0} {1}",OsmPrimitiveType.from(conflict.getMy()).getLocalizedDisplayNameSingular(),conflict.getMy().id),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        conflict.getMy().version = Math.max(conflict.getMy().version, conflict.getTheir().version);
        getLayer().getConflicts().remove(conflict);
        rememberConflict(conflict);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }
}