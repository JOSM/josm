// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.tags.MergeDecisionType;
import org.openstreetmap.josm.gui.conflict.tags.TagMergeItem;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a the resolution of a tag conflict in an {@see OsmPrimitive}
 *
 */
public class TagConflictResolveCommand extends Command {

    /** my primitive (in the local dataset). merge decisions are applied to this
     *  primitive
     */
    private final OsmPrimitive my;
    /** their primitive (in the server dataset) */
    private final OsmPrimitive their;

    /** the list of merge decisions, represented as {@see TagMergeItem}s */
    private final List<TagMergeItem> mergeItems;

    /**
     * replies the number of decided conflicts
     * 
     * @return the number of decided conflicts
     */
    public int getNumDecidedConflicts() {
        int n = 0;
        for (TagMergeItem item: mergeItems) {
            if (!item.getMergeDecision().equals(MergeDecisionType.UNDECIDED)) {
                n++;
            }
        }
        return n;
    }

    /**
     * replies a (localized) display name for the type of an OSM primitive
     * 
     * @param primitive the primitive
     * @return a localized display name
     */
    protected String getPrimitiveTypeAsString(OsmPrimitive primitive) {
        if (primitive instanceof Node) return tr("node");
        if (primitive instanceof Way) return tr("way");
        if (primitive instanceof Relation) return tr("relation");
        return "";
    }

    /**
     * constructor
     * 
     * @param my  my primitive
     * @param their  their primitive
     * @param mergeItems the list of merge decisions, represented as {@see TagMergeItem}s
     */
    public TagConflictResolveCommand(OsmPrimitive my, OsmPrimitive their, List<TagMergeItem> mergeItems) {
        this.my = my;
        this.their = their;
        this.mergeItems = mergeItems;
    }


    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Resolve {0} tag conflicts in {1} {2}",getNumDecidedConflicts(), getPrimitiveTypeAsString(my), my.id),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of modified primitives, i.e. of
        // OSM primitive 'my'
        //
        super.executeCommand();

        // apply the merge decisions to OSM primitive 'my'
        //
        for (TagMergeItem item: mergeItems) {
            if (! item.getMergeDecision().equals(MergeDecisionType.UNDECIDED)) {
                item.applyToMyPrimitive(my);
            }
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(my);
    }

    @Override
    public void undoCommand() {
        // restore former state of modified primitives
        //
        super.undoCommand();

        // restore a conflict if necessary
        //
        if (!Main.map.conflictDialog.conflicts.containsKey(my)) {
            Main.map.conflictDialog.conflicts.put(my,their);
        }
    }


}
