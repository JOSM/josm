// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
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

    public RemoveNodesCommand(Way way, List<Node> rmNodes) {
        super();
        this.way = way;
        this.rmNodes = new HashSet<Node>(rmNodes);
    }

    @Override public boolean executeCommand() {
        super.executeCommand();
        way.removeNodes(rmNodes);
        way.setModified(true);
        return true;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
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
}
