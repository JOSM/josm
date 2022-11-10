// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.function.Supplier;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.gui.layer.geoimage.RemoteEntry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Marker representing an image. Uses a special icon, and when clicked,
 * displays an image view dialog. Re-uses some code from GeoImageLayer.
 *
 * @author Frederik Ramm
 *
 */
public class ImageMarker extends ButtonMarker {

    public URL imageUrl;

    public ImageMarker(LatLon ll, URL imageUrl, MarkerLayer parentLayer, double time, double offset) {
        super(ll, /* ICON(markers/) */ "photo", parentLayer, time, offset);
        this.imageUrl = imageUrl;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        ImageViewerDialog.getInstance().displayImages(this.parentLayer, Collections.singletonList(getRemoteEntry()));
    }

    private RemoteEntry getRemoteEntry() {
        try {
            final RemoteEntry remoteEntry = new RemoteEntry(this.parentLayer, imageUrl.toURI(), getFirstImage(), getPreviousImage(),
                    getNextImage(), getLastImage());
            // First, extract EXIF data
            remoteEntry.extractExif();
            // Then, apply information from this point. This may overwrite details from
            // the exif, but that will (hopefully) be OK.
            if (Double.isFinite(this.time)) {
                remoteEntry.setGpsTime(Instant.ofEpochMilli((long) (this.time * 1000)));
            }
            if (this.isLatLonKnown()) {
                remoteEntry.setPos(this);
            }
            if (!Utils.isBlank(this.getText())) {
                remoteEntry.setDisplayName(this.getText());
            }
            return remoteEntry;
        } catch (URISyntaxException e) {
            Logging.trace(e);
            new Notification(tr("Malformed URI: ", this.imageUrl.toExternalForm())).setIcon(JOptionPane.WARNING_MESSAGE).show();
        }
        return null;
    }

    private Supplier<RemoteEntry> getFirstImage() {
        for (Marker marker : this.parentLayer.data) {
            if (marker instanceof ImageMarker) {
                if (marker == this) {
                    break;
                }
                ImageMarker imageMarker = (ImageMarker) marker;
                return imageMarker::getRemoteEntry;
            }
        }
        return () -> null;
    }

    private Supplier<RemoteEntry> getPreviousImage() {
        int index = this.parentLayer.data.indexOf(this);
        for (int i = index - 1; i >= 0; i--) {
            Marker marker = this.parentLayer.data.get(i);
            if (marker instanceof ImageMarker) {
                ImageMarker imageMarker = (ImageMarker) marker;
                return imageMarker::getRemoteEntry;
            }
        }
        return () -> null;
    }

    private Supplier<RemoteEntry> getNextImage() {
        int index = this.parentLayer.data.indexOf(this);
        for (int i = index + 1; i < this.parentLayer.data.size(); i++) {
            Marker marker = this.parentLayer.data.get(i);
            if (marker instanceof ImageMarker) {
                ImageMarker imageMarker = (ImageMarker) marker;
                return imageMarker::getRemoteEntry;
            }
        }
        return () -> null;
    }

    private Supplier<RemoteEntry> getLastImage() {
        int index = Math.max(0, this.parentLayer.data.indexOf(this));
        for (int i = this.parentLayer.data.size() - 1; i >= index; i--) {
            Marker marker = this.parentLayer.data.get(i);
            if (marker instanceof ImageMarker) {
                if (marker == this) {
                    break;
                }
                ImageMarker imageMarker = (ImageMarker) marker;
                return imageMarker::getRemoteEntry;
            }
        }
        return () -> null;
    }

    @Override
    public WayPoint convertToWayPoint() {
        WayPoint wpt = super.convertToWayPoint();
        GpxLink link = new GpxLink(imageUrl.toString());
        link.type = "image";
        wpt.put(GpxConstants.META_LINKS, Collections.singleton(link));
        return wpt;
    }
}
