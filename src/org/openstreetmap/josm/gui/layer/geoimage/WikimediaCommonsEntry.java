// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Mediawiki;

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

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        WikimediaCommonsEntry other = (WikimediaCommonsEntry) obj;
        return Objects.equals(title, other.title);
    }
}
