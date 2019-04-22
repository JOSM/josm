// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Abstracts superclass of {@link ChangeNodesCommand} / {@link RemoveNodesCommand}.
 * @param <C> type of nodes collection used for this command
 * @since 15013
 */
public abstract class AbstractNodesCommand<C extends Collection<Node>> extends Command {

    protected final Way way;
    protected final C cmdNodes;

    /**
     * Constructs a new {@code AbstractNodesCommand}.
     * @param way The way to modify
     * @param cmdNodes The collection of nodes for this command
     */
    protected AbstractNodesCommand(Way way, C cmdNodes) {
        this(way.getDataSet(), way, cmdNodes);
    }

    /**
     * Constructs a new {@code AbstractNodesCommand}.
     * @param ds The target data set. Must not be {@code null}
     * @param way The way to modify
     * @param cmdNodes The collection of nodes for this command
     */
    protected AbstractNodesCommand(DataSet ds, Way way, C cmdNodes) {
        super(ds);
        this.way = Objects.requireNonNull(way, "way");
        this.cmdNodes = Objects.requireNonNull(cmdNodes, "cmdNodes");
        if (cmdNodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes collection is empty");
        }
    }

    protected abstract void modifyWay();

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        modifyWay();
        way.setModified(true);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(way);
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get(OsmPrimitiveType.WAY);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), way, cmdNodes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        AbstractNodesCommand<?> that = (AbstractNodesCommand<?>) obj;
        return Objects.equals(way, that.way) &&
               Objects.equals(cmdNodes, that.cmdNodes);
    }
}
