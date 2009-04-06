//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.xml.sax.SAXException;

import javax.swing.JOptionPane;

public class OsmServerObjectReader extends OsmServerReader {

    public final static  String TYPE_WAY = "way";
    public final static  String TYPE_REL = "relation";
    public final static  String TYPE_NODE = "node";

    long id;
    String type;
    boolean full;

    public OsmServerObjectReader(long id, String type, boolean full) {
        this.id = id;
        this.type = type;
        this.full = full;
    }
    /**
     * Method to download single objects from OSM server. ways, relations, nodes
     * @return the data requested
     * @throws SAXException
     * @throws IOException
     */
    public DataSet parseOsm() throws SAXException, IOException {
        try {
            Main.pleaseWaitDlg.progress.setValue(0);
            Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
            StringBuffer sb = new StringBuffer();
            sb.append(type);
            sb.append("/");
            sb.append(id);
            if (full)
                sb.append("/full");

            final InputStream in = getInputStream(sb.toString(), Main.pleaseWaitDlg);
            if (in == null)
                return null;
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
            final OsmReader osm = OsmReader.parseDataSetOsm(in, null, Main.pleaseWaitDlg);
            final DataSet data = osm.getDs();

//          Bounds bounds = new Bounds(new LatLon(lat1, lon1), new LatLon(lat2, lon2));
//          DataSource src = new DataSource(bounds, origin);
//          data.dataSources.add(src);
            if (osm.getParseNotes().length() != 0) {
                JOptionPane.showMessageDialog(Main.parent, osm.getParseNotes());
            }
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
