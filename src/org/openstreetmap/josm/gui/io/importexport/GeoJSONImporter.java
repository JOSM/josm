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

    /**
     * Parse GeoJSON dataset.
     * @param source geojson file
     * @return GeoJSON dataset
     * @throws IOException in case of I/O error
     * @throws IllegalDataException if an error was found while parsing the data from the source
     * @deprecated since 18807, use {@link #parseDataSet(InputStream, ProgressMonitor)} instead
     */
    @Deprecated
    public DataSet parseDataSet(final String source) throws IOException, IllegalDataException {
        try (CachedFile cf = new CachedFile(source)) {
            InputStream fileInputStream = Compression.getUncompressedFileInputStream(cf.getFile()); // NOPMD
            return this.parseDataSet(fileInputStream, NullProgressMonitor.INSTANCE);
        }
    }

    @Override
    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException {
        return GeoJSONReader.parseDataSet(in, progressMonitor);
    }
}
