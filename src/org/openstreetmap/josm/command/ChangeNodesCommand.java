// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.List;
import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
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

    public ChangeNodesCommand(Way way, List<Node> newNodes) {
        super();
        this.way = way;
        this.newNodes = newNodes;
    }

    @Override public boolean executeCommand() {
        super.executeCommand();
        way.setNodes(newNodes);
        way.setModified(true);
        return true;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
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
}
