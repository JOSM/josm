// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmImporter extends FileImporter {

    private OsmDataLayer layer;
    private Runnable postLayerTask;

    public OsmImporter() {
        super(new ExtensionFileFilter("osm,xml", "osm", tr("OSM Server Files") + " (*.osm *.xml)"));
    }

    public OsmImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        try {
            FileInputStream in = new FileInputStream(file);
            importData(in, file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IOException(tr("File ''{0}'' does not exist.", file.getName()));
        }
    }

    protected void importData(InputStream in, final File associatedFile) throws IllegalDataException {
        loadLayer(in, associatedFile, associatedFile.getName(), NullProgressMonitor.INSTANCE);
        // FIXME: remove UI stuff from IO subsystem
        Runnable uiStuff = new Runnable() {
            @Override
            public void run() {
                Main.main.addLayer(layer);
                postLayerTask.run();
                layer.onPostLoadFromFile();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            uiStuff.run();
        } else {
            SwingUtilities.invokeLater(uiStuff);
        }
    }

    /**
     * Load osm data layer from InputStream.
     * associatedFile can be null if the stream does not come from a file.
     */
    public void loadLayer(InputStream in, final File associatedFile, final String layerName, ProgressMonitor progressMonitor) throws IllegalDataException {
        final DataSet dataSet = OsmReader.parseDataSet(in, progressMonitor);
        String name = associatedFile == null ? OsmDataLayer.createNewName() : associatedFile.getName();
        layer = new OsmDataLayer(dataSet, layerName, associatedFile);
        postLayerTask = new Runnable() {
            @Override
            public void run() {
                if (dataSet.allPrimitives().isEmpty()) {
                    String msg;
                    if (associatedFile == null) {
                        msg = tr("No data found for layer ''{0}''.", layerName);
                    } else {
                        msg = tr("No data found in file ''{0}''.", associatedFile.getPath());
                    }
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            msg,
                            tr("Open OSM file"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
                layer.onPostLoadFromFile();
            }
        };
    }

    public OsmDataLayer getLayer() {
        return layer;
    }

    public Runnable getPostLayerTask() {
        return postLayerTask;
    }
}
