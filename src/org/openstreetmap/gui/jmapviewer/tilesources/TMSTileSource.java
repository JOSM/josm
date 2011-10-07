package org.openstreetmap.gui.jmapviewer.tilesources;

//License: GPL.

import java.awt.Image;

import org.openstreetmap.gui.jmapviewer.Coordinate;

public class TMSTileSource extends AbstractTSMTileSource {
    protected int maxZoom;
    protected int minZoom = 0;
    protected String attributionText;
    protected String attributionLinkURL;
    protected Image attributionImage;
    protected String attributionImageURL;
    protected String termsOfUseText;
    protected String termsOfUseURL;

    public TMSTileSource(String name, String url, int maxZoom) {
        super(name, url);
        this.maxZoom = maxZoom;
    }

    public TMSTileSource(String name, String url, int minZoom, int maxZoom) {
        super(name, url);
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    @Override
    public int getMinZoom() {
        return (minZoom == 0) ? super.getMinZoom() : minZoom;
    }

    @Override
    public int getMaxZoom() {
        return (maxZoom == 0) ? super.getMaxZoom() : maxZoom;
    }

    public TileUpdate getTileUpdate() {
        return TileUpdate.IfNoneMatch;
    }

    @Override
    public boolean requiresAttribution() {
        return attributionText != null;
    }

    @Override
    public String getAttributionText(int zoom, Coordinate topLeft, Coordinate botRight) {
        return attributionText;
    }

    @Override
    public String getAttributionLinkURL() {
        return attributionLinkURL;
    }

    @Override
    public Image getAttributionImage() {
        return attributionImage;
    }

    @Override
    public String getAttributionImageURL() {
        return attributionImageURL;
    }

    @Override
    public String getTermsOfUseText() {
        return termsOfUseText;
    }

    @Override
    public String getTermsOfUseURL() {
        return termsOfUseURL;
    }

    public void setAttributionText(String text) {
        attributionText = text;
    }

    public void setAttributionLinkURL(String text) {
        attributionLinkURL = text;
    }

    public void setAttributionImage(Image img) {
        attributionImage = img;
    }

    public void setAttributionImageURL(String text) {
        attributionImageURL = text;
    }

    public void setTermsOfUseText(String text) {
        termsOfUseText = text;
    }

    public void setTermsOfUseURL(String text) {
        termsOfUseURL = text;
    }
}
