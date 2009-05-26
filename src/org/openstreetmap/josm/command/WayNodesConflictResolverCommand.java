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
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represent a command for resolving conflicts in the node list of two
 * {@see Way}s.
 *
 */
public class WayNodesConflictResolverCommand extends Command {

    /** my way */
    private Way my;
    /** their way */ 
    private Way their;
    /** the list of merged nodes. This becomes the list of news of my way after the
     *  command is executed
     */
    private List<Node> mergedNodeList; 
    
    /**
     * 
     * @param my my may
     * @param their their way
     * @param mergedNodeList  the list of merged nodes 
     */
    public WayNodesConflictResolverCommand(Way my, Way their, List<Node> mergedNodeList) {
        this.my = my;
        this.their = their;
        this.mergedNodeList = mergedNodeList;
    }
    
    
    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                   tr("Resolve conflicts in node list of of way {0}", my.id), 
                   ImageProvider.get("data", "object"), 
                   JLabel.HORIZONTAL
                )
         );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of 'my' way
        //
        super.executeCommand();
        
        // replace the list of nodes of 'my' way by the list of merged
        // nodes 
        //
        my.nodes.clear();
        for (int i=0; i<mergedNodeList.size();i++) {
            Node n = mergedNodeList.get(i);
            my.nodes.add(n);
            if (! Main.ds.nodes.contains(n)) {
                System.out.println("Main.ds doesn't include node " + n.toString());
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
        // restore the former state
        //
        super.undoCommand();
        
        // restore a conflict if necessary
        //
        if (!Main.map.conflictDialog.conflicts.containsKey(my)) {
            Main.map.conflictDialog.conflicts.put(my,their);
        }
    }
}
