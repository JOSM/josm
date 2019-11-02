// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxExtension;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Marker specifying the default behaviour.
 * @since 11892 (extracted from Marker)
 */
public final class DefaultMarkerProducers implements MarkerProducers {

    @Override
    public Collection<Marker> createMarkers(WayPoint wpt, File relativePath, MarkerLayer parentLayer, double time,
            double offset) {
        String uri = null;
        // cheapest way to check whether "link" object exists and is a non-empty collection of GpxLink objects...
        Collection<GpxLink> links = wpt.<GpxLink>getCollection(GpxConstants.META_LINKS);
        if (links != null) {
            for (GpxLink oneLink : links) {
                uri = oneLink.uri;
                break;
            }
        }

        URL url = uriToUrl(uri, relativePath);
        String urlStr = url == null ? "" : url.toString();
        String symbolName = Optional.ofNullable(wpt.getString("symbol"))
                .orElseGet(() -> wpt.getString(GpxConstants.PT_SYM));
        // text marker is returned in every case, see #10208
        final Marker marker = new Marker(wpt.getCoor(), wpt, symbolName, parentLayer, time, offset);
        if (url == null) {
            return Collections.singleton(marker);
        } else if (Utils.hasExtension(urlStr, "wav", "mp3", "aac", "aif", "aiff")) {
            final AudioMarker audioMarker = new AudioMarker(wpt.getCoor(), wpt, url, parentLayer, time, offset);
            GpxExtension offsetExt = wpt.getExtensions().get("josm", "sync-offset");
            if (offsetExt != null && offsetExt.getValue() != null) {
                try {
                    audioMarker.syncOffset = Double.parseDouble(offsetExt.getValue());
                } catch (NumberFormatException nfe) {
                    Logging.warn(nfe);
                }
            }
            return Arrays.asList(marker, audioMarker);
        } else if (Utils.hasExtension(urlStr, "png", "jpg", "jpeg", "gif")) {
            return Arrays.asList(marker, new ImageMarker(wpt.getCoor(), url, parentLayer, time, offset));
        } else {
            return Arrays.asList(marker, new WebMarker(wpt.getCoor(), url, parentLayer, time, offset));
        }
    }

    private static URL uriToUrl(String uri, File relativePath) {
        URL url = null;
        if (uri != null) {
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {
                // Try a relative file:// url, if the link is not in an URL-compatible form
                if (relativePath != null) {
                    url = Utils.fileToURL(new File(relativePath.getParentFile(), uri));
                }
            }
        }
        return url;
    }
}
