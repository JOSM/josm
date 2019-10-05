// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.io.importexport.GeoJSONImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * GeoJson server reader.
 * @author Omar Vega Ramos &lt;ovruni@riseup.net&gt;
 * @since 15424
 */
public class GeoJSONServerReader extends OsmServerReader {

    private final String url;

    /**
     * Constructs a new {@code GeoJSONServerReader}.
     * @param url geojson URL
     */
    public GeoJSONServerReader(String url) {
        this.url = Objects.requireNonNull(url);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        try {
            progressMonitor.beginTask(tr("Contacting Server…"), 10);
            return new GeoJSONImporter().parseDataSet(url);
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }
}
