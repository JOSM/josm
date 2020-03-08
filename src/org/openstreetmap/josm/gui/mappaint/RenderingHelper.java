// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class to render osm data to a file.
 * @since 12963
 */
public class RenderingHelper {

    private final DataSet ds;
    private final Bounds bounds;
    private final ProjectionBounds projBounds;
    private final double scale;
    private final Collection<StyleData> styles;
    private Color backgroundColor;
    private boolean fillBackground = true;
    private PrintStream debugStream;

    /**
     * Data class to save style settings along with the corresponding style URL.
     */
    public static class StyleData {
        public String styleUrl;
        public Map<String, String> settings = new HashMap<>();
    }

    /**
     * Construct a new {@code RenderingHelper}.
     * @param ds the dataset to render
     * @param bounds the bounds of the are to render
     * @param scale the scale to render at (east/north units per pixel)
     * @param styles the styles to use for rendering
     */
    public RenderingHelper(DataSet ds, Bounds bounds, double scale, Collection<StyleData> styles) {
        CheckParameterUtil.ensureParameterNotNull(ds, "ds");
        CheckParameterUtil.ensureParameterNotNull(bounds, "bounds");
        CheckParameterUtil.ensureParameterNotNull(styles, "styles");
        this.ds = ds;
        this.bounds = bounds;
        this.scale = scale;
        this.styles = styles;
        Projection proj = ProjectionRegistry.getProjection();
        projBounds = new ProjectionBounds();
        projBounds.extend(proj.latlon2eastNorth(bounds.getMin()));
        projBounds.extend(proj.latlon2eastNorth(bounds.getMax()));
    }

    /**
     * Set the background color to use for rendering.
     *
     * @param backgroundColor the background color to use, {@code} means
     * to determine the background color automatically from the style
     * @see #setFillBackground(boolean)
     * @since 12966
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Decide if background should be filled or left transparent.
     * @param fillBackground true, if background should be filled
     * @see #setBackgroundColor(java.awt.Color)
     * @since 12966
     */
    public void setFillBackground(boolean fillBackground) {
        this.fillBackground = fillBackground;
    }

    Dimension getImageSize() {
        double widthEn = projBounds.maxEast - projBounds.minEast;
        double heightEn = projBounds.maxNorth - projBounds.minNorth;
        int widthPx = (int) Math.round(widthEn / scale);
        int heightPx = (int) Math.round(heightEn / scale);
        return new Dimension(widthPx, heightPx);
    }

    /**
     * Invoke the renderer.
     *
     * @return the rendered image
     * @throws IOException in case of an IOException
     * @throws IllegalDataException when illegal data is encountered (style has errors, etc.)
     */
    public BufferedImage render() throws IOException, IllegalDataException {
        // load the styles
        ElemStyles elemStyles = new ElemStyles();
        MapCSSStyleSource.STYLE_SOURCE_LOCK.writeLock().lock();
        try {
            for (StyleData sd : styles) {
                MapCSSStyleSource source = new MapCSSStyleSource(sd.styleUrl, "cliRenderingStyle", "cli rendering style '" + sd.styleUrl + "'");
                source.loadStyleSource();
                elemStyles.add(source);
                if (!source.getErrors().isEmpty()) {
                    throw new IllegalDataException("Failed to load style file. Errors: " + source.getErrors());
                }
                for (String key : sd.settings.keySet()) {
                    StyleSetting.PropertyStyleSetting<?> match = source.settings.stream()
                            .filter(s -> s instanceof StyleSetting.PropertyStyleSetting)
                            .map(s -> (StyleSetting.PropertyStyleSetting<?>) s)
                            .filter(bs -> bs.getKey().endsWith(":" + key))
                            .findFirst().orElse(null);
                    if (match == null) {
                        Logging.warn(tr("Style setting not found: ''{0}''", key));
                    } else {
                        String value = sd.settings.get(key);
                        Logging.trace("setting applied: ''{0}:{1}''", key, value);
                        match.setStringValue(value);
                    }
                }
                if (!sd.settings.isEmpty()) {
                    source.loadStyleSource(); // reload to apply settings
                }
            }
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.writeLock().unlock();
        }

        Dimension imgDimPx = getImageSize();
        NavigatableComponent nc = new NavigatableComponent() {
            {
                setBounds(0, 0, imgDimPx.width, imgDimPx.height);
                updateLocationState();
            }

            @Override
            protected boolean isVisibleOnScreen() {
                return true;
            }

            @Override
            public Point getLocationOnScreen() {
                return new Point(0, 0);
            }
        };
        nc.zoomTo(projBounds.getCenter(), scale);

        // render the data
        BufferedImage image = new BufferedImage(imgDimPx.width, imgDimPx.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Force all render hints to be defaults - do not use platform values
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (fillBackground) {
            g.setColor(Optional.ofNullable(backgroundColor).orElse(elemStyles.getBackgroundColor()));
            g.fillRect(0, 0, imgDimPx.width, imgDimPx.height);
        }
        StyledMapRenderer smr = new StyledMapRenderer(g, nc, false);
        smr.setStyles(elemStyles);
        smr.render(ds, false, bounds);

        // For debugging, write computed StyleElement to debugStream for primitives marked with debug=yes
        if (debugStream != null) {
            for (OsmPrimitive primitive : ds.allPrimitives()) {
                if (!primitive.isKeyTrue("debug")) {
                    continue;
                }
                debugStream.println(primitive);
                for (StyleElement styleElement : elemStyles.get(primitive, scale, nc)) {
                    debugStream.append(" * ").println(styleElement);
                }
            }
        }

        return image;
    }

    void setDebugStream(PrintStream debugStream) {
        this.debugStream = debugStream;
    }
}
