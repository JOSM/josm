package org.openstreetmap.gui.jmapviewer.tilesources;

//License: GPL.

import java.awt.Image;

import org.openstreetmap.gui.jmapviewer.Coordinate;

public class TMSTileSource extends AbstractTMSTileSource {

    protected int maxZoom;
    protected int minZoom = 0;

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

}
