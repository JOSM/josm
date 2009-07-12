//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.xml.sax.SAXException;

public class OsmServerObjectReader extends OsmServerReader {

    long id;
    OsmPrimitiveType type;
    boolean full;

    public OsmServerObjectReader(long id, OsmPrimitiveType type, boolean full) {
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
    @Override
    public DataSet parseOsm() throws OsmTransferException {
        try {
            Main.pleaseWaitDlg.progress.setValue(0);
            Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
            StringBuffer sb = new StringBuffer();
            sb.append(type.getAPIName());
            sb.append("/");
            sb.append(id);
            if (full && ! type.equals(OsmPrimitiveType.NODE)) {
                sb.append("/full");
            }

            final InputStream in = getInputStream(sb.toString(), Main.pleaseWaitDlg);
            if (in == null)
                return null;
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
            final OsmReader osm = OsmReader.parseDataSetOsm(in,Main.pleaseWaitDlg);
            final DataSet data = osm.getDs();

            if (osm.getParseNotes().length() != 0) {
                JOptionPane.showMessageDialog(Main.parent, osm.getParseNotes());
            }
            in.close();
            activeConnection = null;
            return data;
        } catch (IOException e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } catch (SAXException e) {
            throw new OsmTransferException(e);
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        }
    }

}
