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
import org.openstreetmap.josm.gui.mappaint.xml.AreaPrototype;
import org.openstreetmap.josm.gui.mappaint.xml.IconPrototype;
import org.openstreetmap.josm.gui.mappaint.xml.LinePrototype;
import org.openstreetmap.josm.gui.mappaint.xml.LinemodPrototype;
import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSource;
import org.openstreetmap.josm.tools.FilteredCollection;
import org.openstreetmap.josm.tools.Predicate;

public class ElemStyles {
    private List<XmlStyleSource> styleSources;

    public ElemStyles()
    {
        styleSources = new ArrayList<XmlStyleSource>();
    }

    public void add(XmlStyleSource style) {
        styleSources.add(style);
    }

    public Collection<XmlStyleSource> getStyleSources() {
        return new FilteredCollection<XmlStyleSource>(styleSources, new Predicate<XmlStyleSource>() {

            String name = Main.pref.get("mappaint.style", "standard");

            @Override
            public boolean evaluate(XmlStyleSource s) {
                return equal(s.getPrefName(), name);
            }

        });
    }

    public static class WayPrototypesRecord {
        public LinePrototype line;
        public List<LinemodPrototype> linemods;
        public AreaPrototype area;

        public List<ElemStyle> createStyles() {
            List<ElemStyle> ret = new ArrayList<ElemStyle>();
            if (area != null) {
                ret.add(area.createStyle());
            }
            if (line != null) {
                ret.add(line.createStyle());
            } else {
                if (area != null) {
                    ret.add(LineElemStyle.createSimpleLineStyle(area.color));
                } else {
                    ret.add(LineElemStyle.UNTAGGED_WAY);
                }
            }

            if (linemods != null) {
                for (LinemodPrototype p : linemods) {
                    LineElemStyle s = p.createStyle(line.getWidth());
                    if (p.over) {
                        ret.add(s);
                    } else {
                        ret.add(0, s);
                    }
                }
            }
            return ret;
        }
    }

    public StyleCache get(OsmPrimitive osm) {
        if (osm instanceof Node) {
            IconPrototype icon = getNode(osm);
            if (icon == null)
                return StyleCache.EMPTY_STYLECACHE;
            return StyleCache.create(icon.createStyle());
        } else {
            WayPrototypesRecord p = get(osm, false);
            return StyleCache.create(p.createStyles());
        }
    }

    public IconPrototype getNode(OsmPrimitive osm) {
        IconPrototype icon = null;
        for (XmlStyleSource s : getStyleSources()) {
            icon = s.getNode(osm, icon);
        }
        return icon;
    }

    private WayPrototypesRecord get(OsmPrimitive osm, boolean forceArea) {
        WayPrototypesRecord p = new WayPrototypesRecord();
        for (XmlStyleSource s : getStyleSources()) {
            s.get(osm, forceArea || !(osm instanceof Way) || ((Way) osm).isClosed(), p);
        }
        return p;
    }

    public boolean hasAreas() {
        for (XmlStyleSource s : getStyleSources()) {
            if (s.hasAreas())
                return true;
        }
        return false;
    }

    public boolean isArea(OsmPrimitive osm) {
        for (XmlStyleSource s : getStyleSources()) {
            if (s.isArea(osm))
                return true;
        }
        return false;
    }

    public StyleCache getArea(Way osm) {
        if (osm.hasKeys()) {
            /* force area mode also for unclosed ways */
            WayPrototypesRecord p = get(osm, true);
            if (p.area != null)
                return StyleCache.create(p.createStyles());
        }
        return StyleCache.EMPTY_STYLECACHE;
    }

    public AreaPrototype getAreaProto(Way osm) {
        if (osm.hasKeys()) {
            /* force area mode also for unclosed ways */
            WayPrototypesRecord p = get(osm, true);
            if (p.area != null)
                return p.area;
        }
        return null;
    }

    public IconElemStyle getIcon(OsmPrimitive osm) {
        if (!osm.hasKeys())
            return null;
        NodeElemStyle icon = getNode(osm).createStyle();
        if (icon instanceof IconElemStyle) {
            return (IconElemStyle) icon;
        }
        return null;
    }

    public Collection<String> getStyleNames() {
        Set<String> names = new HashSet<String>();
        names.add("standard");
        for (XmlStyleSource s : styleSources) {
            if (s.name != null) {
                names.add(s.name);
            }
        }
        return names;
    }
}
