// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmChangeReader;
import org.openstreetmap.josm.tools.Logging;

/**
 * File importer that reads OSM change files (*.osc).
 * @see <a href="http://wiki.openstreetmap.org/wiki/OsmChange">OsmChange</a>
 */
public class OsmChangeImporter extends FileImporter {

    public static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "osc", "osc", tr("OsmChange File"), true);

    /**
     * Constructs a new {@code OsmChangeImporter}.
     */
    public OsmChangeImporter() {
        super(FILE_FILTER);
    }

    public OsmChangeImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        try {
            importData(Compression.getUncompressedFileInputStream(file), file, progressMonitor);
        } catch (FileNotFoundException e) {
            Logging.error(e);
            throw new IOException(tr("File ''{0}'' does not exist.", file.getName()), e);
        }
    }

    protected void importData(InputStream in, final File associatedFile, ProgressMonitor progressMonitor) throws IllegalDataException {
        final DataSet dataSet = OsmChangeReader.parseDataSet(in, progressMonitor);
        final OsmDataLayer layer = new OsmDataLayer(dataSet, associatedFile.getName(), associatedFile);
        addDataLayer(dataSet, layer, associatedFile.getPath());
    }

    protected void addDataLayer(final DataSet dataSet, final OsmDataLayer layer, final String filePath) {
        // FIXME: remove UI stuff from IO subsystem
        //
        GuiHelper.runInEDT(() -> {
            if (dataSet.allPrimitives().isEmpty()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("No data found in file {0}.", filePath),
                        tr("Open OsmChange file"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            MainApplication.getLayerManager().addLayer(layer);
            layer.onPostLoadFromFile();
        });
    }
}
