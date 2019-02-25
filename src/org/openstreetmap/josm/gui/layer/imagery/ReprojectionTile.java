// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.CoordinateConversion;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageWarp;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

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

    /**
     * Get the scale that was used for reprojecting the tile.
     *
     * This is not necessarily the mapview scale, but may be
     * adjusted to avoid excessively large cache image.
     * @return the scale that was used for reprojecting the tile
     */
    public double getNativeScale() {
        return nativeScale;
    }

    /**
     * Check if it is necessary to refresh the cache to match the current mapview
     * scale and get optimized image quality.
     *
     * When the maximum zoom is exceeded, this method will generally return false.
     * @param currentScale the current mapview scale
     * @return true if the tile should be reprojected again from the source image.
     */
    public synchronized boolean needsUpdate(double currentScale) {
        if (Utils.equalsEpsilon(nativeScale, currentScale))
            return false;
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

    private EastNorth tileToEastNorth(int x, int y, int z) {
        return CoordinateConversion.projToEn(source.tileXYtoProjected(x, y, z));
    }

    /**
     * Transforms the given image.
     * @param imageIn tile image to reproject
     */
    protected void transform(BufferedImage imageIn) {
        if (!MainApplication.isDisplayingMapView()) {
            reset();
            return;
        }
        double scaleMapView = MainApplication.getMap().mapView.getScale();
        ImageWarp.Interpolation interpolation;
        switch (Config.getPref().get("imagery.warp.pixel-interpolation", "bilinear")) {
            case "nearest_neighbor":
                interpolation = ImageWarp.Interpolation.NEAREST_NEIGHBOR;
                break;
            default:
                interpolation = ImageWarp.Interpolation.BILINEAR;
        }

        Projection projCurrent = ProjectionRegistry.getProjection();
        Projection projServer = Projections.getProjectionByCode(source.getServerCRS());
        EastNorth en00Server = tileToEastNorth(xtile, ytile, zoom);
        EastNorth en11Server = tileToEastNorth(xtile + 1, ytile + 1, zoom);
        ProjectionBounds pbServer = new ProjectionBounds(en00Server);
        pbServer.extend(en11Server);
        // find east-north rectangle in current projection, that will fully contain the tile
        ProjectionBounds pbTarget = projCurrent.getEastNorthBoundsBox(pbServer, projServer);

        double margin = 2;
        Dimension dim = getDimension(pbMarginAndAlign(pbTarget, scaleMapView, margin), scaleMapView);
        Integer scaleFix = limitScale(source.getTileSize(), Math.sqrt(dim.getWidth() * dim.getHeight()));
        double scale = scaleFix == null ? scaleMapView : (scaleMapView * scaleFix);
        ProjectionBounds pbTargetAligned = pbMarginAndAlign(pbTarget, scale, margin);

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

        ImageWarp.PointTransform transform;
        int stride = Config.getPref().getInt("imagery.warp.projection-interpolation.stride", 7);
        if (stride > 0) {
            transform = new ImageWarp.GridTransform(pointTransform, stride);
        } else {
            transform = pointTransform;
        }
        Dimension targetDim = getDimension(pbTargetAligned, scale);
        try {
            BufferedImage imageOut = ImageWarp.warp(imageIn, targetDim, transform, interpolation);
            synchronized (this) {
                this.image = imageOut;
                this.anchor = new TileAnchor(p00Img, p11Img);
                this.nativeScale = scale;
                this.maxZoomReached = scaleFix != null;
            }
        } catch (NegativeArraySizeException e) {
            // See #17387 - https://bugs.openjdk.java.net/browse/JDK-4690476
            throw BugReport.intercept(e).put("targetDim", targetDim);
        }
    }

    // add margin and align to pixel grid
    private static ProjectionBounds pbMarginAndAlign(ProjectionBounds box, double scale, double margin) {
        double minEast = Math.floor(box.minEast / scale - margin) * scale;
        double minNorth = -Math.floor(-(box.minNorth / scale - margin)) * scale;
        double maxEast = Math.ceil(box.maxEast / scale + margin) * scale;
        double maxNorth = -Math.ceil(-(box.maxNorth / scale + margin)) * scale;
        return new ProjectionBounds(minEast, minNorth, maxEast, maxNorth);
    }

    // dimension in pixel
    private static Dimension getDimension(ProjectionBounds bounds, double scale) {
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
