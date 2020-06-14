// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;
/// Feel free to move me somewhere else. Maybe a bit specific for josm.tools?

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.gui.history.TwoColumnDiff.Item.DiffItemType;
import org.openstreetmap.josm.tools.Diff;
import org.openstreetmap.josm.tools.Utils;

/**
 * Produces a "two column diff" of two lists. (same as diff -y)
 *
 * Each list is annotated with the changes relative to the other, and "empty" cells are inserted so the lists are comparable item by item.
 *
 * diff on [1 2 3 4] [1 a 4 5] yields:
 *
 * item(SAME, 1)    item(SAME, 1)
 * item(CHANGED, 2) item(CHANGED, 2)
 * item(DELETED, 3) item(EMPTY)
 * item(SAME, 4)    item(SAME, 4)
 * item(EMPTY)      item(INSERTED, 5)
 *
 * @author olejorgenb
 */
class TwoColumnDiff {
    public static class Item {

        public enum DiffItemType {
            INSERTED(new Color(0xDD, 0xFF, 0xDD)),
            DELETED(new Color(255, 197, 197)),
            CHANGED(new Color(255, 234, 213)),
            REVERSED(new Color(255, 255, 204)),
            SAME(new Color(234, 234, 234)),
            EMPTY(new Color(234, 234, 234));

            @SuppressWarnings("ImmutableEnumChecker") // see https://github.com/google/error-prone/pull/1682
            private final Color color;
            DiffItemType(Color color) {
                this.color = color;
            }

            public Color getColor() {
                return color;
            }

            public Color getColor(boolean isSelected, boolean hasFocus) {
                if (isSelected && hasFocus) {
                    return TagTableCellRenderer.BGCOLOR_SELECTED_FOCUS;
                } else if (isSelected) {
                    return TagTableCellRenderer.BGCOLOR_SELECTED;
                } else {
                    return getColor();
                }
            }
        }

        Item(DiffItemType state, Object value) {
            this.state = state;
            this.value = state == DiffItemType.EMPTY ? null : value;
        }

        public final Object value;
        public final DiffItemType state;
    }

    public List<Item> referenceDiff;
    public List<Item> currentDiff;
    private final Object[] reference;
    private final Object[] current;
    boolean referenceReversed;

    TwoColumnDiff(Object[] reference, Object... current) {
        this.reference = Utils.copyArray(reference);
        this.current = Utils.copyArray(current);
        referenceDiff = new ArrayList<>();
        currentDiff = new ArrayList<>();
        diff();
    }

    private void diff() {
        Diff.Change script = new Diff(reference, current).diff2(false);
        // attempt diff with reference reversed and test whether less deletions+inserts are required
        Object[] referenceReversed = Utils.copyArray(reference);
        Collections.reverse(Arrays.asList(referenceReversed));
        Diff.Change scriptReversed = new Diff(referenceReversed, current).diff2(false);
        if (scriptReversed == null /* reference and current are identical */
                || (script != null && scriptReversed.getTotalNumberOfChanges() < script.getTotalNumberOfChanges())) {
            this.referenceReversed = true;
            twoColumnDiffFromScript(scriptReversed, referenceReversed, current, true);
        } else {
            this.referenceReversed = false;
            twoColumnDiffFromScript(script, reference, current, false);
        }
    }

    /**
     * The result from the diff algorithm is a "script" (a compressed description of the changes)
     * This method expands this script into a full two column description.
     * @param script diff script
     * @param a reference version
     * @param b current version
     * @param reversed if {@code true} use {@link DiffItemType#REVERSED} instead of {@link DiffItemType#SAME}
     */
    private void twoColumnDiffFromScript(Diff.Change script, Object[] a, Object[] b, final boolean reversed) {
        int ia = 0;
        int ib = 0;

        while (script != null) {
            int deleted = script.deleted;
            int inserted = script.inserted;
            while (ia < script.line0 && ib < script.line1) {
                referenceDiff.add(new Item(reversed ? DiffItemType.REVERSED : DiffItemType.SAME, a[ia++]));
                currentDiff.add(new Item(DiffItemType.SAME, b[ib++]));
            }

            while (inserted > 0 || deleted > 0) {
                if (inserted > 0 && deleted > 0) {
                    referenceDiff.add(new Item(DiffItemType.CHANGED, a[ia++]));
                    currentDiff.add(new Item(DiffItemType.CHANGED, b[ib++]));
                } else if (inserted > 0) {
                    referenceDiff.add(new Item(DiffItemType.EMPTY, null));
                    currentDiff.add(new Item(DiffItemType.INSERTED, b[ib++]));
                } else {
                    referenceDiff.add(new Item(DiffItemType.DELETED, a[ia++]));
                    currentDiff.add(new Item(DiffItemType.EMPTY, null));
                }
                inserted--;
                deleted--;
            }
            script = script.link;
        }
        while (ia < a.length && ib < b.length) {
            referenceDiff.add(new Item(reversed ? DiffItemType.REVERSED : DiffItemType.SAME, a[ia++]));
            currentDiff.add(new Item(DiffItemType.SAME, b[ib++]));
        }
    }
}
