// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GeoJSONReader;
import org.openstreetmap.josm.io.IllegalDataException;

/**
 * GeoJSON file importer.
 * @author Ian Dees &lt;ian.dees@gmail.com&gt;
 * @author <a href="https://github.com/matthieun">matthieun</a>
 * @since 15424, extends {@link OsmImporter} since 18807
 */
public class GeoJSONImporter extends OsmImporter {

    private static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
        "geojson", "geojson", tr("GeoJSON file") + " (*.geojson, *.geojson.gz, *.geojson.bz2, *.geojson.xz, *.geojson.zip)",
        ExtensionFileFilter.AddArchiveExtension.NONE, Arrays.asList("gz", "bz", "bz2", "xz", "zip"));

    /**
     * Constructs a new GeoJSON File importer with an extension filter for .json and .geojson
     */
    public GeoJSONImporter() {
        super(FILE_FILTER);
    }

    @Override
    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException {
        return GeoJSONReader.parseDataSet(in, progressMonitor);
    }
}
