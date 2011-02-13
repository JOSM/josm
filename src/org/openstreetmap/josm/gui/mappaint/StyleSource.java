// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.trn;

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

abstract public class StyleSource extends SourceEntry {

    private List<Throwable> errors = new ArrayList<Throwable>();
    public File zipIcons;

    private ImageIcon imageIcon;

    /******
     * The following fields is additional information found in the header
     * of the source file.
     */
    
    public String icon;

    public StyleSource(String url, String name, String title) {
        super(url, name, title, true);
    }

    public StyleSource(SourceEntry entry) {
        super(entry.url, entry.name, entry.title, entry.active);
    }

    abstract public void apply(MultiCascade mc, OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed);

    abstract public void loadStyleSource();

    abstract public InputStream getSourceInputStream() throws IOException;

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
                imageIcon = MapPaintStyles.getIcon(new IconReference(icon, this), false);
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
                    "dialogs/mappaint/error_small",
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
}
