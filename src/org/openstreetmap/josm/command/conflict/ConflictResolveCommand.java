// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Objects;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.tools.Logging;

/**
 * This is the common base class for {@link Command}s which manipulate {@link Conflict}s in
 * addition to {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s.
 *
 * A ConflictResolverCommand can remember a collection of conflicts it resolves. Upon undoing
 * it reconstitutes them.
 *
 */
public abstract class ConflictResolveCommand extends Command {
    /** the list of resolved conflicts */
    private final ConflictCollection resolvedConflicts = new ConflictCollection();

    /**
     * Constructs a new {@code ConflictResolveCommand} in the context of a given data set.
     * @param ds the data set. Must not be null.
     */
    public ConflictResolveCommand(DataSet ds) {
        super(ds);
    }

    /**
     * remembers a conflict in the internal list of remembered conflicts
     *
     * @param c the remembered conflict
     */
    protected void rememberConflict(Conflict<?> c) {
        if (!resolvedConflicts.hasConflictForMy(c.getMy())) {
            resolvedConflicts.add(c);
        }
    }

    /**
     * reconstitutes all remembered conflicts. Add the remembered conflicts to the
     * set of conflicts of the {@link DataSet} this command was applied to.
     *
     */
    protected void reconstituteConflicts() {
        DataSet ds = getAffectedDataSet();
        for (Conflict<?> c : resolvedConflicts) {
            if (!ds.getConflicts().hasConflictForMy(c.getMy())) {
                ds.getConflicts().add(c);
            }
        }
    }

    @Override
    public void undoCommand() {
        super.undoCommand();

        DataSet ds = getAffectedDataSet();
        if (!OsmDataManager.getInstance().containsDataSet(ds)) {
            Logging.warn(tr("Cannot undo command ''{0}'' because layer ''{1}'' is not present any more",
                    this.toString(),
                    ds.getName()
            ));
            return;
        }

        OsmDataManager.getInstance().setActiveDataSet(ds);
        reconstituteConflicts();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resolvedConflicts);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ConflictResolveCommand that = (ConflictResolveCommand) obj;
        return Objects.equals(resolvedConflicts, that.resolvedConflicts);
    }
}
