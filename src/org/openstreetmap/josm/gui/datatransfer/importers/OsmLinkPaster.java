// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Handles the paste / drop of an OSM address.
 * <p>
 * e.g. http://www.openstreetmap.org/node/123 downloads node 123
 *
 * @author Michael Zangl
 * @since 10881
 */
public class OsmLinkPaster extends AbstractOsmDataPaster {

    static final class NoWarnOpenLocationAction extends OpenLocationAction {

        NoWarnOpenLocationAction() {
            super(null);
        }

        @Override
        protected void warnNoSuitableTasks(String url) {
            // ignore this.
        }
    }

    private static final String OSM_SERVER = "^https?\\://(\\w+\\.)?(osm|openstreetmap)\\.org/";

    /**
     * Create a new Osm address paster
     */
    public OsmLinkPaster() {
        super(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(TransferSupport support, OsmDataLayer layer, EastNorth pasteAt)
            throws UnsupportedFlavorException, IOException {
        if (!supports(support)) {
            throw new UnsupportedFlavorException(df);
        }

        String transferData = (String) support.getTransferable().getTransferData(df);
        if (!new NoWarnOpenLocationAction().openUrl(transferData).isEmpty()) {
            return true;
        }

        LatLon ll = parseLatLon(transferData);
        if (ll != null) {
            Component comp = support.getComponent();
            if (comp instanceof MapView) {
                ((MapView) comp).zoomTo(ll);
            }
        }

        return false;
    }

    static LatLon parseLatLon(String transferData) {
        Matcher matcher = Pattern
                .compile(OSM_SERVER + "#map=(?<zoom>\\d+)/(?<lat>-?\\d+\\.\\d+)/(?<lon>-?\\d+\\.\\d+)$")
                .matcher(transferData);

        if (!matcher.matches()) {
            return null;
        } else {
            return new LatLon(Double.parseDouble(matcher.group("lat")), Double.parseDouble(matcher.group("lon")));
        }
    }
}
