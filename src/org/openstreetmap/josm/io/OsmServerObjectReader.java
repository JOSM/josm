// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.xml.sax.SAXException;

public class OsmServerObjectReader extends OsmServerReader {
    
     public final static  String TYPE_WAY = "way";
     public final static  String TYPE_REL = "relation";
     public final static  String TYPE_NODE = "node";
     
     /**
      * Method to download single objects from OSM server. ways, relations, nodes
      * @param id Object ID
      * @param type way node relation
      * @param full download with or without child objects
      * @return the data requested
      * @throws SAXException
      * @throws IOException
      */
     public DataSet parseOsm(long id, String type, boolean full) throws SAXException, IOException {
            try {
                
                Main.pleaseWaitDlg.progress.setValue(0);
                Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
                StringBuffer sb = new StringBuffer();
                sb.append(type);
                sb.append("/");
                sb.append(id);
                if (full)
                {
                    sb.append("/full");
                }
                
                final InputStream in = getInputStream(sb.toString(), Main.pleaseWaitDlg);
                if (in == null)
                    return null;
                Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
                final DataSet data = OsmReader.parseDataSet(in, null, Main.pleaseWaitDlg);
//                String origin = Main.pref.get("osm-server.url")+"/"+Main.pref.get("osm-server.version", "0.5");
//                Bounds bounds = new Bounds(new LatLon(lat1, lon1), new LatLon(lat2, lon2));
//                DataSource src = new DataSource(bounds, origin);
//                data.dataSources.add(src);
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
