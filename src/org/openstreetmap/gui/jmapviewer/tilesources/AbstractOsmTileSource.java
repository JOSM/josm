/**
 * 
 */
package org.openstreetmap.gui.jmapviewer.tilesources;

import java.awt.Image;
import java.io.IOException;

import javax.swing.ImageIcon;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;

public abstract class AbstractOsmTileSource extends AbstractTMSTileSource {
    public AbstractOsmTileSource(String name, String base_url) {
        super(name, base_url);
    }

    public int getMaxZoom() {
        return 18;
    }

    @Override
    public boolean requiresAttribution() {
        return true;
    }

    @Override
    public String getAttributionText(int zoom, Coordinate topLeft, Coordinate botRight) {
        return "\u00a9 OpenStreetMap contributors, CC-BY-SA ";
    }

    @Override
    public String getAttributionLinkURL() {
        return "http://openstreetmap.org/";
    }

    @Override
    public Image getAttributionImage() {
        return null;
    }

    @Override
    public String getAttributionImageURL() {
        return null;
    }

    @Override
    public String getTermsOfUseText() {
        return null;
    }

    @Override
    public String getTermsOfUseURL() {
        return "http://www.openstreetmap.org/copyright";
    }
}
