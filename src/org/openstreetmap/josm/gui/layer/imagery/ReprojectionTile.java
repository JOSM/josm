// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.tools.ImageWarp;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tile class that stores a reprojected version of the original tile.
 * @since 11858
 */
public class ReprojectionTile extends Tile {

    protected TileAnchor anchor;
    private double nativeScale;
    protected boolean maxZoomReached;

    /**
     * Constructs a new {@code ReprojectionTile}.
     * @param source sourec tile
     * @param xtile X coordinate
     * @param ytile Y coordinate
     * @param zoom zoom level
     */
    public ReprojectionTile(TileSource source, int xtile, int ytile, int zoom) {
        super(source, xtile, ytile, zoom);
    }

    /**
     * Get the position of the tile inside the image.
     * @return the position of the tile inside the image
     * @see #getImage()
     */
    public TileAnchor getAnchor() {
        return anchor;
    }

    public double getNativeScale() {
        return nativeScale;
    }

    public boolean needsUpdate(double currentScale) {
        if (Utils.equalsEpsilon(nativeScale, currentScale))
            return false;
        // zoomed in even more - max zoom already reached, so no update
        return !maxZoomReached || currentScale >= nativeScale;
    }

    @Override
    public void setImage(BufferedImage image) {
        if (image == null) {
            reset();
        } else {
            transform(image);
        }
    }

    /**
     * Invalidate tile - mark it as not loaded.
     */
    public synchronized void invalidate() {
        this.loaded = false;
        this.loading = false;
        this.error = false;
        this.error_message = null;
    }

    private synchronized void reset() {
        this.image = null;
        this.anchor = null;
        this.maxZoomReached = false;
    }

    public void transform(BufferedImage imageIn) {
        if (!Main.isDisplayingMapView()) {
            reset();
            return;
        }
        double scaleMapView = Main.map.mapView.getScale();
        ImageWarp.Interpolation interpolation;
        switch (Main.pref.get("imagery.warp.interpolation", "bilinear")) {
            case "nearest_neighbor":
                interpolation = ImageWarp.Interpolation.NEAREST_NEIGHBOR;
                break;
            default:
                interpolation = ImageWarp.Interpolation.BILINEAR;
        }
        double margin = interpolation.getMargin();

        Projection projCurrent = Main.getProjection();
        Projection projServer = Projections.getProjectionByCode(source.getServerCRS());
        EastNorth en00Server = new EastNorth(source.tileXYtoProjected(xtile, ytile, zoom));
        EastNorth en11Server = new EastNorth(source.tileXYtoProjected(xtile + 1, ytile + 1, zoom));
        ProjectionBounds pbServer = new ProjectionBounds(en00Server);
        pbServer.extend(en11Server);
        // find east-north rectangle in current projection, that will fully contain the tile
        ProjectionBounds pbTarget = projCurrent.getEastNorthBoundsBox(pbServer, projServer);

        // add margin and align to pixel grid
        double minEast = Math.floor(pbTarget.minEast / scaleMapView - margin) * scaleMapView;
        double minNorth = -Math.floor(-(pbTarget.minNorth / scaleMapView - margin)) * scaleMapView;
        double maxEast = Math.ceil(pbTarget.maxEast / scaleMapView + margin) * scaleMapView;
        double maxNorth = -Math.ceil(-(pbTarget.maxNorth / scaleMapView + margin)) * scaleMapView;
        ProjectionBounds pbTargetAligned = new ProjectionBounds(minEast, minNorth, maxEast, maxNorth);

        Dimension dim = getDimension(pbTargetAligned, scaleMapView);
        Integer scaleFix = limitScale(source.getTileSize(), Math.sqrt(dim.getWidth() * dim.getHeight()));
        double scale = scaleFix == null ? scaleMapView : (scaleMapView * scaleFix);

        ImageWarp.PointTransform pointTransform = pt -> {
            EastNorth target = new EastNorth(pbTargetAligned.minEast + pt.getX() * scale,
                    pbTargetAligned.maxNorth - pt.getY() * scale);
            EastNorth sourceEN = projServer.latlon2eastNorth(projCurrent.eastNorth2latlon(target));
            double x = source.getTileSize() *
                    (sourceEN.east() - pbServer.minEast) / (pbServer.maxEast - pbServer.minEast);
            double y = source.getTileSize() *
                    (pbServer.maxNorth - sourceEN.north()) / (pbServer.maxNorth - pbServer.minNorth);
            return new Point2D.Double(x, y);
        };

        // pixel coordinates of tile origin and opposite tile corner inside the target image
        // (tile may be deformed / rotated by reprojection)
        EastNorth en00Current = projCurrent.latlon2eastNorth(projServer.eastNorth2latlon(en00Server));
        EastNorth en11Current = projCurrent.latlon2eastNorth(projServer.eastNorth2latlon(en11Server));
        Point2D p00Img = new Point2D.Double(
                (en00Current.east() - pbTargetAligned.minEast) / scale,
                (pbTargetAligned.maxNorth - en00Current.north()) / scale);
        Point2D p11Img = new Point2D.Double(
                (en11Current.east() - pbTargetAligned.minEast) / scale,
                (pbTargetAligned.maxNorth - en11Current.north()) / scale);

        BufferedImage imageOut = ImageWarp.warp(
                imageIn, getDimension(pbTargetAligned, scale), pointTransform,
                interpolation);
        synchronized (this) {
            this.image = imageOut;
            this.anchor = new TileAnchor(p00Img, p11Img);
            this.nativeScale = scale;
            this.maxZoomReached = scaleFix != null;
        }
    }

    private Dimension getDimension(ProjectionBounds bounds, double scale) {
        return new Dimension(
                (int) Math.round((bounds.maxEast - bounds.minEast) / scale),
                (int) Math.round((bounds.maxNorth - bounds.minNorth) / scale));
    }

    /**
     * Make sure, the image is not scaled up too much.
     *
     * This would not give any significant improvement in image quality and may
     * exceed the user's memory. The correction factor is a power of 2.
     * @param lenOrig tile size of original image
     * @param lenNow (averaged) tile size of warped image
     * @return factor to shrink if limit is exceeded; 1 if it is already at the
     * limit, but no change needed; null if it is well below the limit and can
     * still be scaled up by at least a factor of 2.
     */
    protected Integer limitScale(double lenOrig, double lenNow) {
        final double limit = 3;
        if (lenNow > limit * lenOrig) {
            int n = (int) Math.ceil((Math.log(lenNow) - Math.log(limit * lenOrig)) / Math.log(2));
            int f = 1 << n;
            double lenNowFixed = lenNow / f;
            if (lenNowFixed > limit * lenOrig) throw new AssertionError();
            if (lenNowFixed <= limit * lenOrig / 2) throw new AssertionError();
            return f;
        }
        if (lenNow > limit * lenOrig / 2)
            return 1;
        return null;
    }
}
