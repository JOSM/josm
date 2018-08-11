// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Converts a {@link MarkerLayer} to a {@link OsmDataLayer}.
 * @since 14129 (extracted from {@link ConvertToDataLayerAction})
 */
public class ConvertFromMarkerLayerAction extends ConvertToDataLayerAction<MarkerLayer> {

    /**
     * Converts a {@link MarkerLayer} to a {@link OsmDataLayer}.
     * @param layer marker layer
     */
    public ConvertFromMarkerLayerAction(MarkerLayer layer) {
        super(layer);
    }

    @Override
    public DataSet convert() {
        final DataSet ds = new DataSet();
        for (Marker marker : layer.data) {
            final Node node = new Node(marker.getCoor());
            final Collection<String> mapping = Config.getPref().getList("gpx.to-osm-mapping", Arrays.asList(
                    GpxConstants.GPX_NAME, "name",
                    GpxConstants.GPX_DESC, "description",
                    GpxConstants.GPX_CMT, "note",
                    GpxConstants.GPX_SRC, "source",
                    GpxConstants.PT_SYM, "gpxicon"));
            if (mapping.size() % 2 == 0) {
                final Iterator<String> it = mapping.iterator();
                while (it.hasNext()) {
                    final String gpxKey = it.next();
                    final String osmKey = it.next();
                    Optional.ofNullable(marker.getTemplateValue(gpxKey, false))
                            .map(String::valueOf)
                            .ifPresent(s -> node.put(osmKey, s));
                }
            } else {
                Logging.warn("Invalid gpx.to-osm-mapping Einstein setting: expecting even number of entries");
            }
            ds.addPrimitive(node);
        }
        return ds;
    }
}
