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
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

abstract public class StyleSource extends SourceEntry {

    private List<Throwable> errors = new ArrayList<Throwable>();
    public File zipIcons;

    private ImageIcon imageIcon;
    private long lastMTime = 0L;

    /******
     * The following fields is additional information found in the header
     * of the source file.
     */

    public String icon;

    public StyleSource(String url, String name, String title) {
        super(url, name, title, true);
    }

    public StyleSource(SourceEntry entry) {
        super(entry);
    }

    abstract public void apply(MultiCascade mc, OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed);

    abstract public void loadStyleSource();

    /**
     * Returns a new {@code InputStream} to the style source. When finished, {@link #closeSourceInputStream(InputStream)} must be called.
     * @return A new {@code InputStream} to the style source that must be closed by the caller
     * @throws IOException if any I/O error occurs.
     * @see #closeSourceInputStream(InputStream)
     */
    abstract public InputStream getSourceInputStream() throws IOException;

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

    final public ImageIcon getIcon() {
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

    public long getLastMTime() {
        return lastMTime;
    }

    public void setLastMTime(long lastMTime) {
        this.lastMTime = lastMTime;
    }


}
