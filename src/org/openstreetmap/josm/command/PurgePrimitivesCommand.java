// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Physically removes an {@see OsmPrimitive} from the dataset of the edit
 * layer and disconnects any references from {@see Way}s or {@see Relation}s
 * to this primitive.
 *
 * This command is necessary if a local {@see OsmPrimitive} has been deleted on
 * the server by another user and if the local user decides to delete his version
 * too. If he only deleted it "logically" JOSM would try to delete it on the server
 * which would result in an non resolvable conflict.
 *
 */
public class PurgePrimitivesCommand extends ConflictResolveCommand{

    static private final Logger logger = Logger.getLogger(PurgePrimitivesCommand.class.getName());

    /** the primitives to purge */
    private Collection<OsmPrimitive> toPurge;

    /** the set of primitives to purge as consequence of purging
     * {@see #primitive}, including {@see #primitive}
     */
    private Set<OsmPrimitive> purgedPrimitives;

    private Set<OsmPrimitive> origVersionsOfTouchedPrimitives;

    protected void init(Collection<OsmPrimitive> toPurge) {
        this.toPurge = toPurge;
        this.purgedPrimitives = new HashSet<OsmPrimitive>();
        this.origVersionsOfTouchedPrimitives = new HashSet<OsmPrimitive>();
    }

    /**
     * constructor
     * @param primitive the primitive to purge
     *
     */
    public PurgePrimitivesCommand(OsmPrimitive primitive) {
        init(Collections.singleton(primitive));
    }

    /**
     * constructor
     * @param layer the OSM data layer
     * @param primitive the primitive to purge
     *
     */
    public PurgePrimitivesCommand(OsmDataLayer layer, OsmPrimitive primitive) {
        super(layer);
        init(Collections.singleton(primitive));
    }

    /**
     * constructor
     * @param layer the OSM data layer
     * @param primitives the primitives to purge
     *
     */
    public PurgePrimitivesCommand(OsmDataLayer layer, Collection<OsmPrimitive> primitives) {
        super(layer);
        init(primitives);
    }

    /**
     * Replies a collection with the purged primitives
     *
     * @return a collection with the purged primitives
     */
    public Collection<OsmPrimitive> getPurgedPrimitives() {
        return purgedPrimitives;
    }

    @Override public JLabel getDescription() {
        if (purgedPrimitives.size() == 1) {
            return new JLabel(
                tr("Purged object ''{0}''",
                        purgedPrimitives.iterator().next().getDisplayName(DefaultNameFormatter.getInstance())),
                ImageProvider.get("data", "object"),
                JLabel.HORIZONTAL
            );
        } else {
            return new JLabel(trn("Purged {0} object", "Purged {0} objects", purgedPrimitives.size(), purgedPrimitives.size()));
        }
    }

    @Override public Collection<PseudoCommand> getChildren() {
        if (purgedPrimitives.size() == 1)
            return null;
        List<PseudoCommand> children = new ArrayList<PseudoCommand>();
        for (final OsmPrimitive osm : purgedPrimitives) {
            children.add(new PseudoCommand() {
                @Override public JLabel getDescription() {
                    return new JLabel(
                        tr("Purged object ''{0}''",
                                osm.getDisplayName(DefaultNameFormatter.getInstance())),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                    );
                }
                @Override public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
                    return Collections.singleton(osm);
                }

            });
        }
        return children;
    }

    /**
     * Purges an {@see OsmPrimitive} <code>child</code> from a {@see DataSet}.
     *
     * @param child the primitive to purge
     * @param hive the hive of {@see OsmPrimitive}s we remember other {@see OsmPrimitive}
     * we have to purge because we purge <code>child</code>.
     *
     */
    protected void removeReferecesToPrimitive(OsmPrimitive child, Set<OsmPrimitive> hive) {
        hive.remove(child);
        for (OsmPrimitive parent: child.getReferrers()) {
            if (toPurge.contains(parent))
                // parent itself is to be purged. This method is going to be
                // invoked for parent later
                return;
            if (parent instanceof Way) {
                Way w = (Way)parent;
                if (!origVersionsOfTouchedPrimitives.contains(w)) {
                    origVersionsOfTouchedPrimitives.add(w);
                }
                w.removeNode((Node)child);
                // if a way ends up with less than two nodes we
                // remember it on the "hive"
                //
                if (w.getNodesCount() < 2) {
                    System.out.println(tr("Warning: Purging way {0} because number of nodes dropped below 2. Current is {1}",
                            w.getId(),w.getNodesCount()));
                    hive.add(w);
                }
            } else if (parent instanceof Relation) {
                Relation r = (Relation)parent;
                if (!origVersionsOfTouchedPrimitives.contains(r)) {
                    origVersionsOfTouchedPrimitives.add(r);
                }
                System.out.println(tr("Removing reference from relation {0}",r.getId()));
                r.removeMembersFor(child);
            } else {
                // should not happen. parent can't be a node
            }
        }
    }

    @Override
    public boolean executeCommand() {
        HashSet<OsmPrimitive> hive = new HashSet<OsmPrimitive>();

        // iteratively purge the primitive and all primitives
        // which violate invariants after they lose a reference to
        // the primitive (i.e. ways which end up with less than two
        // nodes)
        hive.addAll(toPurge);
        while(! hive.isEmpty()) {
            OsmPrimitive p = hive.iterator().next();
            removeReferecesToPrimitive(p, hive);
            getLayer().data.removePrimitive(p);
            purgedPrimitives.add(p);
            ConflictCollection conflicts = getLayer().getConflicts();
            if (conflicts.hasConflictForMy(p)) {
                rememberConflict(conflicts.getConflictForMy(p));
                conflicts.remove(p);
            }
        }
        return super.executeCommand();
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.addAll(origVersionsOfTouchedPrimitives);
    }

    @Override
    public void undoCommand() {
        if (! Main.map.mapView.hasLayer(getLayer())) {
            logger.warning(tr("Cannot undo command ''{0}'' because layer ''{1}'' is not present any more",
                    this.toString(),
                    getLayer().toString()
            ));
            return;
        }
        Main.map.mapView.setActiveLayer(getLayer());

        // restore purged primitives
        //
        for (OsmPrimitive purged : purgedPrimitives) {
            getLayer().data.addPrimitive(purged);
        }
        reconstituteConflicts();

        // will restore the primitives referring to one
        // of the purged primitives
        super.undoCommand();
    }
}
