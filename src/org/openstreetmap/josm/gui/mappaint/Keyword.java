// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.tools.Utils;

public class Keyword {
    public final String val;

    public Keyword(String val) {
        this.val = val.toLowerCase();
    }

    @Override
    public String toString() {
        return "Keyword{" + val + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        return Utils.equal(val, ((Keyword) obj).val);
    }

    @Override
    public int hashCode() {
        return val.hashCode();
    }

    public final static Keyword AUTO = new Keyword("auto");
    public final static Keyword BOTTOM = new Keyword("bottom");
    public final static Keyword CENTER = new Keyword("center");
    public final static Keyword DEFAULT = new Keyword("default");
    public final static Keyword RIGHT = new Keyword("right");
    public final static Keyword THINNEST = new Keyword("thinnest");
}
