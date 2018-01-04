// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * Marker class with Web URL activation.
 *
 * @author Frederik Ramm
 * @since 200
 */
public class WebMarker extends ButtonMarker {

    private final URL webUrl;

    public WebMarker(LatLon ll, URL webUrl, MarkerLayer parentLayer, double time, double offset) {
        super(ll, /* ICON(markers/) */ "web", parentLayer, time, offset);
        CheckParameterUtil.ensureParameterNotNull(webUrl, "webUrl");
        this.webUrl = webUrl;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        String error = OpenBrowser.displayUrl(webUrl.toString());
        if (error != null) {
            setErroneous(true);
            new Notification(
                    "<b>" + tr("There was an error while trying to display the URL for this marker") + "</b><br>" +
                                  tr("(URL was: ") + webUrl + ')' + "<br>" + error)
                    .setIcon(JOptionPane.ERROR_MESSAGE)
                    .setDuration(Notification.TIME_LONG)
                    .show();
        } else {
            updateErroneous();
        }
    }

    @Override
    public WayPoint convertToWayPoint() {
        WayPoint wpt = super.convertToWayPoint();
        GpxLink link = new GpxLink(webUrl.toString());
        link.type = "web";
        wpt.put(GpxConstants.META_LINKS, Collections.singleton(link));
        return wpt;
    }

    private void updateErroneous() {
        if ("file".equals(webUrl.getProtocol())) {
            String path = webUrl.getPath();
            try {
                setErroneous(path.isEmpty() || !new File(path).exists());
            } catch (SecurityException e) {
                Logging.warn(e);
                setErroneous(true);
            }
        } else {
            setErroneous(false);
        }
    }
}
