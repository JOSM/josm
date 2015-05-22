// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.ImageOverlay;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * A mappaint style (abstract class).
 *
 * Handles everything from parsing the style definition to application
 * of the style to an osm primitive.
 */
public abstract class StyleSource extends SourceEntry {

    private List<Throwable> errors = new ArrayList<>();
    public File zipIcons;

    /** image provider returning the icon for this style */
    private ImageProvider imageIconProvider;

    /** image provider returning the default icon */
    private static ImageProvider defaultIconProvider;

    /******
     * The following fields is additional information found in the header
     * of the source file.
     */
    public String icon;

    /**
     * List of settings for user customization.
     */
    public final List<StyleSetting> settings = new ArrayList<>();
    /**
     * Values of the settings for efficient lookup.
     */
    public Map<String, Object> settingValues = new HashMap<>();

    /**
     * Constructs a new, active {@link StyleSource}.
     * @param url URL that {@link org.openstreetmap.josm.io.CachedFile} understands
     * @param name The name for this StyleSource
     * @param title The title that can be used as menu entry
     */
    public StyleSource(String url, String name, String title) {
        super(url, name, title, true);
    }

    /**
     * Constructs a new {@link StyleSource}
     * @param entry The entry to copy the data (url, name, ...) from.
     */
    public StyleSource(SourceEntry entry) {
        super(entry);
    }

    /**
     * Apply style to osm primitive.
     *
     * Adds properties to a MultiCascade. All active {@link StyleSource}s add
     * their properties on after the other. At a later stage, concrete painting
     * primitives (lines, icons, text, ...) are derived from the MultiCascade.
     * @param mc the current MultiCascade, empty for the first StyleSource
     * @param osm the primitive
     * @param scale the map scale
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     */
    public abstract void apply(MultiCascade mc, OsmPrimitive osm, double scale, boolean pretendWayIsClosed);

    /**
     * Loads the style source.
     */
    public abstract void loadStyleSource();

    /**
     * Returns a new {@code InputStream} to the style source. When finished, {@link #closeSourceInputStream(InputStream)} must be called.
     * @return A new {@code InputStream} to the style source that must be closed by the caller
     * @throws IOException if any I/O error occurs.
     * @see #closeSourceInputStream(InputStream)
     */
    public abstract InputStream getSourceInputStream() throws IOException;

    /**
     * Returns a new {@code CachedFile} to the local file containing style source (can be a text file or an archive).
     * @return A new {@code CachedFile} to the local file containing style source
     * @throws IOException if any I/O error occurs.
     * @since 7081
     */
    public abstract CachedFile getCachedFile() throws IOException;

    /**
     * Closes the source input stream previously returned by {@link #getSourceInputStream()} and other linked resources, if applicable.
     * @param is The source input stream that must be closed
     * @see #getSourceInputStream()
     * @since 6289
     */
    public void closeSourceInputStream(InputStream is) {
        Utils.close(is);
    }

    public void logError(Throwable e) {
        errors.add(e);
    }

    public Collection<Throwable> getErrors() {
        return Collections.unmodifiableCollection(errors);
    }

    /**
     * Initialize the class.
     */
    protected void init() {
        errors.clear();
        imageIconProvider = null;
        icon = null;
    }

    /**
     * Image provider for default icon.
     *
     * @return image provider for default styles icon
     * @see #getIconProvider()
     * @since 8097
     */
    private static synchronized ImageProvider getDefaultIconProvider() {
        if (defaultIconProvider == null) {
            defaultIconProvider = new ImageProvider("dialogs/mappaint", "pencil");
        }
        return defaultIconProvider;
    }

    /**
     * Image provider for source icon. Uses default icon, when not else available.
     *
     * @return image provider for styles icon
     * @see #getIconProvider()
     * @since 8097
     */
    protected ImageProvider getSourceIconProvider() {
        if (imageIconProvider == null) {
            if (icon != null) {
                imageIconProvider = MapPaintStyles.getIconProvider(new IconReference(icon, this), true);
            }
            if (imageIconProvider == null) {
                imageIconProvider = getDefaultIconProvider();
            }
        }
        return imageIconProvider;
    }

    /**
     * Image provider for source icon.
     *
     * @return image provider for styles icon
     * @since 8097
     */
    public final ImageProvider getIconProvider() {
        ImageProvider i = getSourceIconProvider();
        if (!getErrors().isEmpty()) {
            i = new ImageProvider(i).addOverlay(new ImageOverlay(new ImageProvider("dialogs/mappaint/error_small")));
        }
        return i;
    }

    /**
     * Image for source icon.
     *
     * @return styles icon for display
     */
    public final ImageIcon getIcon() {
        return getIconProvider().setMaxSize(ImageProvider.ImageSizes.MENU).get();
    }

    /**
     * Return text to display as ToolTip.
     *
     * @return tooltip text containing error status
     */
    public String getToolTipText() {
        if (errors.isEmpty())
            return null;
        else
            return trn("There was an error when loading this style. Select ''Info'' from the right click menu for details.",
                    "There were {0} errors when loading this style. Select ''Info'' from the right click menu for details.",
                    errors.size(), errors.size());
    }

    public Color getBackgroundColorOverride() {
        return null;
    }
}
