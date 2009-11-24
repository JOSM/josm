// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This is the common base class for {@see Command}s which manipulate {@see Conflict}s in
 * addition to {@see OsmPrimitive}s.
 *
 * A ConflictResolverCommand can remember a collection of conflicts it resolves. Upon undoing
 * it reconstitutes them.
 *
 */
public abstract class ConflictResolveCommand extends Command {
    private static final Logger logger = Logger.getLogger(ConflictResolveCommand.class.getName());

    /** the list of resolved conflicts */
    private ConflictCollection resolvedConflicts;

    public ConflictResolveCommand() {
        super();
        resolvedConflicts = new ConflictCollection();
    }

    public ConflictResolveCommand(OsmDataLayer layer) {
        super(layer);
        resolvedConflicts = new ConflictCollection();
    }

    /**
     * remembers a conflict in the internal list of remembered conflicts
     *
     * @param c the remembered conflict
     */
    protected void rememberConflict(Conflict<?> c) {
        if (! resolvedConflicts.hasConflictForMy(c.getMy())) {
            resolvedConflicts.add(c);
        }
    }

    /**
     * reconstitutes all remembered conflicts. Add the remembered conflicts to the
     * set of conflicts of the {@see OsmDataLayer} this command was applied to.
     *
     */
    protected void reconstituteConflicts() {
        OsmDataLayer editLayer = getLayer();
        for(Conflict<?> c : resolvedConflicts) {
            if (!editLayer.getConflicts().hasConflictForMy(c.getMy())) {
                editLayer.getConflicts().add(c);
            }
        }
    }

    @Override
    public void undoCommand() {
        super.undoCommand();

        if (! Main.map.mapView.hasLayer(getLayer())) {
            logger.warning(tr("Can''t undo command ''{0}'' because layer ''{1}'' is not present any more",
                    this.toString(),
                    getLayer().toString()
            ));
            return;
        }

        Main.map.mapView.setActiveLayer(getLayer());
        reconstituteConflicts();
    }
}
