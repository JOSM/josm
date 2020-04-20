// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;

/**
 * The asynchronous task for updating a collection of objects using multi fetch.
 * @since 2599
 */
public class UpdatePrimitivesTask extends AbstractPrimitiveTask {

    private final Collection<? extends OsmPrimitive> toUpdate;

    /**
     * Constructs a new {@code UpdatePrimitivesTask}.
     *
     * @param layer the layer in which primitives are updated. Must not be null.
     * @param toUpdate a collection of primitives to update from the server. Set to
     * the empty collection if null.
     * @throws IllegalArgumentException if layer is null.
     */
    public UpdatePrimitivesTask(OsmDataLayer layer, Collection<? extends OsmPrimitive> toUpdate) {
        super(tr("Update objects"), layer);
        this.toUpdate = toUpdate != null ? toUpdate : Collections.<OsmPrimitive>emptyList();
    }

    @Override
    protected void initMultiFetchReader(MultiFetchServerObjectReader reader) {
        // don't update new primitives
        toUpdate.stream().filter(p -> !p.isNew()).forEach(reader::append);
    }
}
