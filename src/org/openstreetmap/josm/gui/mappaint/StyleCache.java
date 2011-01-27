// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class StyleCache {

    private List<ElemStyle> styles;

    private final static Storage<StyleCache> internPool = new Storage<StyleCache>(); // TODO: clean up the intern pool from time to time (after purge or layer removal)

    public final static StyleCache EMPTY_STYLECACHE = create();
    public final static StyleCache SIMPLE_NODE_STYLECACHE = create(SimpleNodeElemStyle.INSTANCE);
    public final static StyleCache UNTAGGED_WAY_STYLECACHE = create(LineElemStyle.UNTAGGED_WAY);

    
    private StyleCache() {
        styles = new ArrayList<ElemStyle>();
    }

    public static StyleCache create() {
        StyleCache sc = new StyleCache();
        sc.styles = new ArrayList<ElemStyle>();
        return sc.intern();
    }

    public static StyleCache create(ElemStyle... styles) {
        StyleCache sc = new StyleCache();
        sc.styles = Arrays.asList(styles);
        return sc.intern();
    }

    public static StyleCache create(Collection<ElemStyle> styles) {
        StyleCache sc = new StyleCache();
        sc.styles = new ArrayList<ElemStyle>(styles);
        return sc.intern();
    }

    public Collection<ElemStyle> getStyles() {
        return Collections.unmodifiableList(styles);
    }

    /**
     * like String.intern() (reduce memory consumption)
     */
    public StyleCache intern() {
        return internPool.putUnique(this);
    }

    public void paint(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        for (ElemStyle s : styles) {
            s.paintPrimitive(primitive, paintSettings, painter, selected, member);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        return styles.equals(((StyleCache) obj).styles);
    }

    @Override
    public int hashCode() {
        return styles.hashCode();
    }

    @Override
    public String toString() {
        return "SC{" + styles + '}';
    }
}
