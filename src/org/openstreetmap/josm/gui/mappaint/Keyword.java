// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Locale;
import java.util.Objects;

public class Keyword {
    public final String val;

    public Keyword(String val) {
        this.val = val.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return "Keyword{" + val + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        return Objects.equals(val, ((Keyword) obj).val);
    }

    @Override
    public int hashCode() {
        return val.hashCode();
    }

    public static final Keyword AUTO = new Keyword("auto");
    public static final Keyword BOTTOM = new Keyword("bottom");
    public static final Keyword CENTER = new Keyword("center");
    public static final Keyword DEFAULT = new Keyword("default");
    public static final Keyword RIGHT = new Keyword("right");
    public static final Keyword THINNEST = new Keyword("thinnest");
}
