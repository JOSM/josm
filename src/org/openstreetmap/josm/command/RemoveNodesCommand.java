// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that removes a set of nodes from a way.
 * The same can be done with ChangeNodesCommand, but this is more
 * efficient. (Needed for the tool to disconnect nodes from ways.)
 *
 * @author Giuseppe Bilotta
 */
public class RemoveNodesCommand extends Command {

    private final Way way;
    private final Set<Node> rmNodes;

    /**
     * Constructs a new {@code RemoveNodesCommand}.
     * @param way The way to modify
     * @param rmNodes The list of nodes to remove
     */
    public RemoveNodesCommand(Way way, List<Node> rmNodes) {
        this.way = way;
        this.rmNodes = new HashSet<>(rmNodes);
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        way.removeNodes(rmNodes);
        way.setModified(true);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(way);
    }

    @Override
    public String getDescriptionText() {
        return tr("Removed nodes from {0}", way.getDisplayName(DefaultNameFormatter.getInstance()));
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get(OsmPrimitiveType.WAY);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((rmNodes == null) ? 0 : rmNodes.hashCode());
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
        RemoveNodesCommand other = (RemoveNodesCommand) obj;
        if (rmNodes == null) {
            if (other.rmNodes != null)
                return false;
        } else if (!rmNodes.equals(other.rmNodes))
            return false;
        if (way == null) {
            if (other.way != null)
                return false;
        } else if (!way.equals(other.way))
            return false;
        return true;
    }
}
