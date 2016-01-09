// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;

/**
 * List of {@link StyleElement}s, immutable.
 */
public class StyleElementList implements Iterable<StyleElement> {
    private final List<StyleElement> lst;

    /**
     * Constructs a new {@code StyleList}.
     */
    public StyleElementList() {
        lst = new ArrayList<>();
    }

    public StyleElementList(StyleElement... init) {
        lst = new ArrayList<>(Arrays.asList(init));
    }

    public StyleElementList(Collection<StyleElement> sl) {
        lst = new ArrayList<>(sl);
    }

    public StyleElementList(StyleElementList sl, StyleElement s) {
        lst = new ArrayList<>(sl.lst);
        lst.add(s);
    }

    @Override
    public Iterator<StyleElement> iterator() {
        return lst.iterator();
    }

    public boolean isEmpty() {
        return lst.isEmpty();
    }

    public int size() {
        return lst.size();
    }

    @Override
    public String toString() {
        return lst.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StyleElementList that = (StyleElementList) obj;
        return Objects.equals(lst, that.lst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lst);
    }
}
