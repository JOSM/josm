// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.Logging;

/**
 * File importer that reads *.osm data files. (main storage format for OSM data in JOSM)
 */
public class OsmImporter extends FileImporter {

    /**
     * The OSM file filter (*.osm and *.xml files).
     */
    public static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "osm,xml", "osm", tr("OSM Server Files") + " (*.osm, *.osm.gz, *.osm.bz2, *.osm.xz, *.osm.zip, *.xml)",
            ExtensionFileFilter.AddArchiveExtension.NONE, Arrays.asList("gz", "bz", "bz2", "xz", "zip"));

    /**
     * Utility class containing imported OSM layer, and a task to run after it is added to MapView.
     */
    public static class OsmImporterData {

        private final OsmDataLayer layer;
        private final Runnable postLayerTask;

        public OsmImporterData(OsmDataLayer layer, Runnable postLayerTask) {
            this.layer = layer;
            this.postLayerTask = postLayerTask;
        }

        public OsmDataLayer getLayer() {
            return layer;
        }

        public Runnable getPostLayerTask() {
            return postLayerTask;
        }
    }

    /**
     * Constructs a new {@code OsmImporter}.
     */
    public OsmImporter() {
        super(FILE_FILTER);
    }

    /**
     * Constructs a new {@code OsmImporter} with the given extension file filter.
     * @param filter The extension file filter
     */
    public OsmImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    /**
     * Imports OSM data from file
     * @param file file to read data from
     * @param progressMonitor handler for progress monitoring and canceling
     */
    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        try (InputStream in = Compression.getUncompressedFileInputStream(file)) {
            importData(in, file, progressMonitor);
        } catch (FileNotFoundException e) {
            Logging.error(e);
            throw new IOException(tr("File ''{0}'' does not exist.", file.getName()), e);
        }
    }

    /**
     * Imports OSM data from stream
     * @param in input stream
     * @param associatedFile filename of data (layer name will be generated from name of file)
     * @param pm handler for progress monitoring and canceling
     * @throws IllegalDataException if an error was found while parsing the OSM data
     */
    protected void importData(InputStream in, final File associatedFile, ProgressMonitor pm) throws IllegalDataException {
        final OsmImporterData data = loadLayer(in, associatedFile,
                associatedFile == null ? OsmDataLayer.createNewName() : associatedFile.getName(), pm);

        // FIXME: remove UI stuff from IO subsystem
        GuiHelper.runInEDT(() -> {
            OsmDataLayer layer = data.getLayer();
            MainApplication.getLayerManager().addLayer(layer);
            data.getPostLayerTask().run();
            data.getLayer().onPostLoadFromFile();
        });
    }

    /**
     * Load osm data layer from InputStream.
     * @param in input stream
     * @param associatedFile filename of data (can be <code>null</code> if the stream does not come from a file)
     * @param layerName name of generated layer
     * @param progressMonitor handler for progress monitoring and canceling
     * @return Utility class containing imported OSM layer, and a task to run after it is added to MapView
     * @throws IllegalDataException if an error was found while parsing the OSM data
     */
    public OsmImporterData loadLayer(InputStream in, final File associatedFile, final String layerName, ProgressMonitor progressMonitor)
            throws IllegalDataException {
        final DataSet dataSet = parseDataSet(in, progressMonitor);
        if (dataSet == null) {
            throw new IllegalDataException(tr("Invalid dataset"));
        }
        OsmDataLayer layer = createLayer(dataSet, associatedFile, layerName);
        Runnable postLayerTask = createPostLayerTask(dataSet, associatedFile, layerName, layer);
        return new OsmImporterData(layer, postLayerTask);
    }

    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException {
        return OsmReader.parseDataSet(in, progressMonitor);
    }

    protected OsmDataLayer createLayer(final DataSet dataSet, final File associatedFile, final String layerName) {
        return new OsmDataLayer(dataSet, layerName, associatedFile);
    }

    protected Runnable createPostLayerTask(final DataSet dataSet, final File associatedFile, final String layerName, final OsmDataLayer layer) {
        return () -> {
            if (dataSet.allPrimitives().isEmpty()) {
                String msg;
                if (associatedFile == null) {
                    msg = tr("No data found for layer ''{0}''.", layerName);
                } else {
                    msg = tr("No data found in file ''{0}''.", associatedFile.getPath());
                }
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        msg,
                        tr("Open OSM file"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            layer.onPostLoadFromFile();
        };
    }
}
