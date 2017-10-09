// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
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
    private String outputFile;

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
        Projection proj = Main.getProjection();
        projBounds = new ProjectionBounds();
        projBounds.extend(proj.latlon2eastNorth(bounds.getMin()));
        projBounds.extend(proj.latlon2eastNorth(bounds.getMax()));
    }

    /**
     * Set the output file for rendering.
     *
     * Default is {@code out.png}.
     * @param outputFile the output file for rendering
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
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
     * @throws IOException in case of an IOException
     * @throws IllegalDataException when illegal data is encountered (style has errors, etc.)
     */
    public void render() throws IOException, IllegalDataException {
        // load the styles
        MapCSSStyleSource.STYLE_SOURCE_LOCK.writeLock().lock();
        try {
            MapPaintStyles.getStyles().clear();
            for (StyleData sd : styles) {
                SourceEntry se = new SourceEntry(SourceType.MAP_PAINT_STYLE, sd.styleUrl,
                            "cliRenderingStyle", "cli rendering style '" + sd.styleUrl + "'", true /* active */);
                StyleSource source = MapPaintStyles.addStyle(se);
                if (!source.getErrors().isEmpty()) {
                    throw new IllegalDataException("Failed to load style file. Errors: " + source.getErrors());
                }
                for (String key : sd.settings.keySet()) {
                    StyleSetting.BooleanStyleSetting match = source.settings.stream()
                            .filter(s -> s instanceof StyleSetting.BooleanStyleSetting)
                            .map(s -> (StyleSetting.BooleanStyleSetting) s)
                            .filter(bs -> bs.prefKey.endsWith(":" + key))
                            .findFirst().orElse(null);
                    if (match == null) {
                        Logging.warn(tr("Style setting not found: ''{0}''", key));
                    } else {
                        boolean value = Boolean.parseBoolean(sd.settings.get(key));
                        Logging.trace("setting applied: ''{0}:{1}''", key, value);
                        match.setValue(value);
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
        g.setColor(PaintColors.getBackgroundColor());
        g.fillRect(0, 0, imgDimPx.width, imgDimPx.height);
        new StyledMapRenderer(g, nc, false).render(ds, false, bounds);

        // write to file
        String output = Optional.ofNullable(outputFile).orElse("out.png");
        ImageIO.write(image, "png", new File(output));
    }

}
