// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that basically replaces one OSM primitive by another of the same type.
 *
 * @since 93
 */
public class ChangeCommand extends Command {

    private final OsmPrimitive osm;
    private final OsmPrimitive newOsm;

    /**
     * Constructs a new {@code ChangeCommand} in the context of the current edit layer, if any.
     * @param osm The existing primitive to modify
     * @param newOsm The new primitive
     */
    public ChangeCommand(OsmPrimitive osm, OsmPrimitive newOsm) {
        this.osm = osm;
        this.newOsm = newOsm;
        sanityChecks();
    }

    /**
     * Constructs a new {@code ChangeCommand} in the context of a given data layer.
     * @param layer The data layer
     * @param osm The existing primitive to modify
     * @param newOsm The new primitive
     * @deprecated to be removed end of 2017. Use {@link #ChangeCommand(DataSet, OsmPrimitive, OsmPrimitive)} instead
     */
    @Deprecated
    public ChangeCommand(OsmDataLayer layer, OsmPrimitive osm, OsmPrimitive newOsm) {
        super(layer);
        this.osm = osm;
        this.newOsm = newOsm;
        sanityChecks();
    }

    /**
     * Constructs a new {@code ChangeCommand} in the context of a given data set.
     * @param data The data set
     * @param osm The existing primitive to modify
     * @param newOsm The new primitive
     * @since 11240
     */
    public ChangeCommand(DataSet data, OsmPrimitive osm, OsmPrimitive newOsm) {
        super(data);
        this.osm = osm;
        this.newOsm = newOsm;
        sanityChecks();
    }

    private void sanityChecks() {
        CheckParameterUtil.ensureParameterNotNull(osm, "osm");
        CheckParameterUtil.ensureParameterNotNull(newOsm, "newOsm");
        if (newOsm instanceof Way && ((Way) newOsm).getNodesCount() == 0) {
            // Do not allow to create empty ways (see #7465)
            throw new IllegalArgumentException(tr("New way {0} has 0 nodes", newOsm));
        }
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        osm.cloneFrom(newOsm);
        osm.setModified(true);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.add(osm);
    }

    @Override
    public String getDescriptionText() {
        String msg;
        switch(OsmPrimitiveType.from(osm)) {
        case NODE: msg = marktr("Change node {0}"); break;
        case WAY: msg = marktr("Change way {0}"); break;
        case RELATION: msg = marktr("Change relation {0}"); break;
        default: throw new AssertionError();
        }
        return tr(msg, osm.getDisplayName(DefaultNameFormatter.getInstance()));
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get(osm.getDisplayType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), osm, newOsm);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ChangeCommand that = (ChangeCommand) obj;
        return Objects.equals(osm, that.osm) &&
                Objects.equals(newOsm, that.newOsm);
    }
}
