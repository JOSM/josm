// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GeoJSONReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.Logging;

/**
 * GeoJSON file importer.
 * @author Ian Dees &lt;ian.dees@gmail.com&gt;
 * @author matthieun &lt;https://github.com/matthieun&gt;
 * @since 15424
 */
public class GeoJSONImporter extends FileImporter {

    private static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
        "geojson,json", "geojson", tr("GeoJSON file") + " (*.geojson, *.geojson.gz, *.geojson.bz2, *.geojson.xz, *.geojson.zip, *.json)",
        ExtensionFileFilter.AddArchiveExtension.NONE, Arrays.asList("gz", "bz", "bz2", "xz", "zip"));

    /**
     * Constructs a new GeoJSON File importer with an extension filter for .json and .geojson
     */
    public GeoJSONImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(final File file, final ProgressMonitor progressMonitor) {
        progressMonitor.beginTask(tr("Loading json file…"));
        progressMonitor.setTicksCount(2);
        Logging.info("Parsing GeoJSON: {0}", file.getAbsolutePath());
        try (InputStream fileInputStream = Compression.getUncompressedFileInputStream(file)) {
            DataSet data = GeoJSONReader.parseDataSet(fileInputStream, progressMonitor);
            progressMonitor.worked(1);
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(data, file.getName(), file));
        } catch (IOException | IllegalDataException e) {
            Logging.error("Error while reading json file!");
            Logging.error(e);
            GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(
                null, tr("Error loading geojson file {0}", file.getAbsolutePath()), tr("Error"), JOptionPane.WARNING_MESSAGE));
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Parse GeoJSON dataset.
     * @param source geojson file
     * @return GeoJSON dataset
     * @throws IOException in case of I/O error
     * @throws IllegalDataException if an error was found while parsing the data from the source
     */
    public DataSet parseDataSet(final String source) throws IOException, IllegalDataException {
        try (CachedFile cf = new CachedFile(source)) {
            InputStream fileInputStream = Compression.getUncompressedFileInputStream(cf.getFile()); // NOPMD
            return GeoJSONReader.parseDataSet(fileInputStream, NullProgressMonitor.INSTANCE);
        }
    }
}
