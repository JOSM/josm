// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.xml.sax.SAXException;

public class OsmImporter extends FileImporter {

    public OsmImporter() {
        super(new ExtensionFileFilter("osm,xml", "osm", tr("OSM Server Files") + " (*.osm *.xml)"));
    }

    @Override public void importData(File file) throws IOException {
        try {
            OsmReader osm = OsmReader.parseDataSetOsm(new FileInputStream(file), null, Main.pleaseWaitDlg);
            DataSet dataSet = osm.getDs();
            OsmDataLayer layer = new OsmDataLayer(dataSet, file.getName(), file);
            Main.main.addLayer(layer);
            layer.fireDataChange();
            if (osm.getParseNotes().length() != 0) {
                /* display at most five lines */
                String notes = osm.getParseNotes();
                int j = 0;
                for (int i = 0; i < 5; i++)
                    j = notes.indexOf('\n', j + 1);
                j = j >= 0 ? j : notes.length();
                JOptionPane.showMessageDialog(Main.parent, notes.substring(0, j));
            }
        } catch (HeadlessException e) {
            e.printStackTrace();
            throw new IOException(tr("Could not read \"{0}\"", file.getName()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IOException(tr("Could not read \"{0}\"", file.getName()));
        } catch (SAXException e) {
            e.printStackTrace();
            throw new IOException(tr("Could not read \"{0}\"", file.getName()));
        }
    }
}
