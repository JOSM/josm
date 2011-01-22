// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.FilteredCollection;
import org.openstreetmap.josm.tools.Predicate;

public class ElemStyles {
    private List<StyleSource> styleSources;

    public ElemStyles()
    {
        styleSources = new ArrayList<StyleSource>();
    }

    public void add(StyleSource style) {
        styleSources.add(style);
    }

    public Collection<StyleSource> getStyleSources() {
        return new FilteredCollection<StyleSource>(styleSources, new Predicate<StyleSource>() {

            String name = Main.pref.get("mappaint.style", "standard");

            @Override
            public boolean evaluate(StyleSource s) {
                return equal(s.getPrefName(), name);
            }

        });
    }

    public ElemStyle get(OsmPrimitive osm) {
        return get(osm, false);
    }

    public ElemStyle get(OsmPrimitive osm, boolean forceArea) {
        if (!osm.hasKeys())
            return null;

        if (osm instanceof Node) {
            IconElemStyle icon = null;
            for (StyleSource s : getStyleSources()) {
                icon = s.getNode(osm, icon);
            }
            return icon;
        } else {
            boolean noclosed;
            if (forceArea) {
                noclosed = false;
            } else {
                noclosed = osm instanceof Way && !((Way) osm).isClosed();
            }
            AreaElemStyle area = null;
            LineElemStyle line = null;
            ElemStyle result = null;
            for (StyleSource s : getStyleSources()) {
                result = s.get(osm, noclosed, area, line);
                if (result instanceof LineElemStyle) {
                    area = null;
                    line = (LineElemStyle) result;
                } else if (result instanceof AreaElemStyle) {
                    area = (AreaElemStyle) result;
                    if (area.getLineStyle() != null) {
                        line = area.getLineStyle();
                    }
                } else if (result != null)
                    throw new AssertionError();
            }
            return result;
        }
    }

    public boolean hasAreas() {
        for (StyleSource s : getStyleSources()) {
            if (s.hasAreas())
                return true;
        }
        return false;
    }

    public boolean isArea(OsmPrimitive osm) {
        for (StyleSource s : getStyleSources()) {
            if (s.isArea(osm))
                return true;
        }
        return false;
    }

    public ElemStyle getArea(Way osm) {
        if (osm.hasKeys()) {
            /* force area mode also for unclosed ways */
            ElemStyle style = get(osm, true);
            if (style != null && style instanceof AreaElemStyle) {
                return style;
            }
        }
        return null;
    }

    public IconElemStyle getIcon(OsmPrimitive osm) {
        return osm.hasKeys() ? (IconElemStyle) get(osm) : null;
    }

    public Collection<String> getStyleNames() {
        Set<String> names = new HashSet<String>();
        names.add("standard");
        for (StyleSource s : styleSources) {
            if (s.name != null) {
                names.add(s.name);
            }
        }
        return names;
    }
}
