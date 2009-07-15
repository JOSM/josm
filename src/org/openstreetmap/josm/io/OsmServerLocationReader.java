//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.xml.sax.SAXException;

public class OsmServerLocationReader extends OsmServerReader {

    String url;

    public OsmServerLocationReader(String url) {
        this.url = url;
    }

    /**
     * Method to download OSM files from somewhere
     */
    @Override
    public DataSet parseOsm() throws OsmTransferException {
        InputStream in = null;
        try {
            Main.pleaseWaitDlg.progress.setValue(0);
            Main.pleaseWaitDlg.currentAction.setText(tr("Contacting Server..."));

            in = getInputStreamRaw(url, Main.pleaseWaitDlg);
            if (in == null)
                return null;
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
            return OsmReader.parseDataSet(in, Main.pleaseWaitDlg);
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                activeConnection = null;
            } catch(Exception e) {/* ignore it */}
        }
    }

}
