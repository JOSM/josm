// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;

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
    private Conflict<? extends OsmPrimitive> conflict;

    /**
     * constructor
     * @param my  my primitive (i.e. the primitive from the local dataset)
     * @param their their primitive (i.e. the primitive from the server)
     */
    public VersionConflictResolveCommand(Conflict<? extends OsmPrimitive> conflict) {
        this.conflict = conflict;
    }

    @Override public JLabel getDescription() {
        String msg = "";
        switch(OsmPrimitiveType.from(conflict.getMy())) {
        case NODE: msg = marktr("Resolve version conflict for node {0}"); break;
        case WAY: msg = marktr("Resolve version conflict for way {0}"); break;
        case RELATION: msg = marktr("Resolve version conflict for relation {0}"); break;
        }
        return new JLabel(
                        tr(msg,conflict.getMy().getId()),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
        );
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        if (!conflict.getMy().isNew()) {
            long myVersion = conflict.getMy().getVersion();
            long theirVersion = conflict.getTheir().getVersion();
            conflict.getMy().setOsmId(
                    conflict.getMy().getId(),
                    (int)Math.max(myVersion, theirVersion)
            );
            // update visiblity state
            if (theirVersion >= myVersion) {
                conflict.getMy().setVisible(conflict.getTheir().isVisible());
            }
        }
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
