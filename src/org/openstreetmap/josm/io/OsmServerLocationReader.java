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
    public DataSet parseOsm() throws SAXException, IOException {
        try {
            Main.pleaseWaitDlg.progress.setValue(0);
            Main.pleaseWaitDlg.currentAction.setText(tr("Contacting Server..."));

            final InputStream in = getInputStreamRaw(url, Main.pleaseWaitDlg);
            if (in == null)
                return null;
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
            final DataSet data = OsmReader.parseDataSet(in, null, Main.pleaseWaitDlg);
//          String origin = Main.pref.get("osm-server.url")+"/"+Main.pref.get("osm-server.version", "0.5");
//          Bounds bounds = new Bounds(new LatLon(lat1, lon1), new LatLon(lat2, lon2));
//          DataSource src = new DataSource(bounds, origin);
//          data.dataSources.add(src);
            in.close();
            activeConnection = null;
            return data;
        } catch (IOException e) {
            if (cancel)
                return null;
            throw e;
        } catch (SAXException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

}
