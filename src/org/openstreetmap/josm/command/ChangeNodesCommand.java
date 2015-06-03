// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that changes the nodes list of a way.
 * The same can be done with ChangeCommand, but this is more
 * efficient. (Needed for the duplicate node fixing
 * tool of the validator plugin, when processing large data sets.)
 *
 * @author Imi
 */
public class ChangeNodesCommand extends Command {

    private final Way way;
    private final List<Node> newNodes;

    /**
     * Constructs a new {@code ChangeNodesCommand}.
     * @param way The way to modify
     * @param newNodes The new list of nodes for the given way
     */
    public ChangeNodesCommand(Way way, List<Node> newNodes) {
        this.way = way;
        this.newNodes = newNodes;
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        way.setNodes(newNodes);
        way.setModified(true);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(way);
    }

    @Override
    public String getDescriptionText() {
        return tr("Changed nodes of {0}", way.getDisplayName(DefaultNameFormatter.getInstance()));
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get(OsmPrimitiveType.WAY);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((newNodes == null) ? 0 : newNodes.hashCode());
        result = prime * result + ((way == null) ? 0 : way.hashCode());
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
        ChangeNodesCommand other = (ChangeNodesCommand) obj;
        if (newNodes == null) {
            if (other.newNodes != null)
                return false;
        } else if (!newNodes.equals(other.newNodes))
            return false;
        if (way == null) {
            if (other.way != null)
                return false;
        } else if (!way.equals(other.way))
            return false;
        return true;
    }
}
