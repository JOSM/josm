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

    private ImageIcon imageIcon;

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

    public StyleSource(String url, String name, String title) {
        super(url, name, title, true);
    }

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
     * @param multipolyOuterWay support for a very old multipolygon tagging style
     * where you add the tags both to the outer and the inner way.
     * However, independent inner way style is also possible.
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
     * @since 6289
     * @see #getSourceInputStream()
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

    protected void init() {
        errors.clear();
        imageIcon = null;
        icon = null;
    }

    private static ImageIcon defaultIcon;

    private static ImageIcon getDefaultIcon() {
        if (defaultIcon == null) {
            defaultIcon = ImageProvider.get("dialogs/mappaint", "pencil");
        }
        return defaultIcon;
    }

    protected ImageIcon getSourceIcon() {
        if (imageIcon == null) {
            if (icon != null) {
                imageIcon = MapPaintStyles.getIcon(new IconReference(icon, this), -1, -1);
            }
            if (imageIcon == null) {
                imageIcon = getDefaultIcon();
            }
        }
        return imageIcon;
    }

    public final ImageIcon getIcon() {
        if (getErrors().isEmpty())
            return getSourceIcon();
        else
            return ImageProvider.overlay(getSourceIcon(),
                    ImageProvider.get("dialogs/mappaint/error_small"),
                    ImageProvider.OverlayPosition.SOUTHEAST);
    }

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
