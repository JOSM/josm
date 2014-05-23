// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;

public class OsmChangeImporter extends FileImporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osc,osc.bz2,osc.bz,osc.gz", "osc", tr("OsmChange File") + " (*.osc *.osc.bz2 *.osc.bz *.osc.gz)");

    /**
     * Constructs a new {@code OsmChangeImporter}.
     */
    public OsmChangeImporter() {
        super(FILE_FILTER);
    }

    public OsmChangeImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    @Override public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        try {
            importData(Compression.getUncompressedFileInputStream(file), file, progressMonitor);
        } catch (FileNotFoundException e) {
            Main.error(e);
            throw new IOException(tr("File ''{0}'' does not exist.", file.getName()));
        }
    }

    protected void importData(InputStream in, final File associatedFile) throws IllegalDataException {
        importData(in, associatedFile, NullProgressMonitor.INSTANCE);
    }

    protected void importData(InputStream in, final File associatedFile, ProgressMonitor  progressMonitor) throws IllegalDataException {
        final DataSet dataSet = OsmChangeReader.parseDataSet(in, progressMonitor);
        final OsmDataLayer layer = new OsmDataLayer(dataSet, associatedFile.getName(), associatedFile);
        addDataLayer(dataSet, layer, associatedFile.getPath());
    }

    protected void addDataLayer(final DataSet dataSet, final OsmDataLayer layer, final String filePath) {
        // FIXME: remove UI stuff from IO subsystem
        //
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                if (dataSet.allPrimitives().isEmpty()) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("No data found in file {0}.", filePath),
                            tr("Open OsmChange file"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
                Main.main.addLayer(layer);
                layer.onPostLoadFromFile();
            }
        });
    }
}
