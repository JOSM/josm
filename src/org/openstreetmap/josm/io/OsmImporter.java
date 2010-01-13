// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmImporter extends FileImporter {

    public OsmImporter() {
        super(new ExtensionFileFilter("osm,xml", "osm", tr("OSM Server Files") + " (*.osm *.xml)"));
    }

    public OsmImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    @Override public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        try {
            FileInputStream in = new FileInputStream(file);
            importData(in, file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IOException(tr("File ''{0}'' does not exist.", file.getName()));
        }
    }

    protected void importData(InputStream in, File associatedFile) throws IllegalDataException {
        DataSet dataSet = OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
        final OsmDataLayer layer = new OsmDataLayer(dataSet, associatedFile.getName(), associatedFile);
        // FIXME: remove UI stuff from IO subsystem
        //
        Runnable uiStuff = new Runnable() {
            public void run() {
                Main.main.addLayer(layer);
                layer.fireDataChange();
                layer.onPostLoadFromFile();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            uiStuff.run();
        } else {
            SwingUtilities.invokeLater(uiStuff);
        }
    }
}
