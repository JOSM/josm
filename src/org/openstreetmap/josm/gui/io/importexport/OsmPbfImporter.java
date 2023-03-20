// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.Arrays;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmPbfReader;

/**
 * File importer that reads *.osm.pbf data files.
 * @since xxx
 */
public class OsmPbfImporter extends OsmImporter {
    /**
     * The OSM file filter (*.osm.pbf files).
     */
    public static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "osm.pbf", "osm.pbf", tr("OSM PBF Files") + " (*.osm.pbf, *.osm.pbf.gz, *.osm.pbf.bz2, *.osm.pbf.xz, *.osm.pbf.zip)",
            ExtensionFileFilter.AddArchiveExtension.NONE, Arrays.asList("gz", "bz", "bz2", "xz", "zip"));

    /**
     * Constructs a new {@code OsmPbfImporter}.
     */
    public OsmPbfImporter() {
        super(FILE_FILTER);
    }

    /**
     * Constructs a new {@code OsmPbfImporter} with the given extension file filter.
     * @param filter The extension file filter
     */
    public OsmPbfImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    @Override
    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException {
        return OsmPbfReader.parseDataSet(in, progressMonitor);
    }
}
