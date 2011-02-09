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
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.tools.ImageProvider;

abstract public class StyleSource extends SourceEntry {

    private List<Throwable> errors = new ArrayList<Throwable>();
    public File zipIcons;

    public StyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription, true);
    }

    public StyleSource(SourceEntry entry) {
        super(entry.url, entry.name, entry.shortdescription, entry.active);
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

    protected void clearErrors() {
        errors.clear();
    }

    private static ImageIcon pencil;

    protected ImageIcon getSourceIcon() {
        if (pencil == null) {
            pencil = ImageProvider.get("dialogs/mappaint", "pencil");
        }
        return pencil;
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
