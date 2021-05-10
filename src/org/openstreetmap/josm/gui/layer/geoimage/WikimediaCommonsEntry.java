// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Mediawiki;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A geocoded image from <a href="https://commons.wikimedia.org/">Wikimedia Commons</a>
 */
class WikimediaCommonsEntry extends ImageEntry {
    private final String title;

    WikimediaCommonsEntry(String title, LatLon latLon) {
        this.title = title.replaceFirst("^File:", "").replace(" ", "_");
        setPos(latLon);
    }

    @Override
    protected URL getImageUrl() throws MalformedURLException {
        return new URL(Mediawiki.getImageUrl("https://upload.wikimedia.org/wikipedia/commons", title));
    }

    @Override
    public String getDisplayName() {
        return "File:" + title;
    }

    @Override
    public String toString() {
        return "File:" + title;
    }
}
