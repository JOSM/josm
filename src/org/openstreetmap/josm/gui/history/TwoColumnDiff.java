// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;
/// Feel free to move me somewhere else. Maybe a bit specific for josm.tools?

import java.util.ArrayList;

import org.openstreetmap.josm.tools.Diff;

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
        public static final int INSERTED = 1;
        public static final int DELETED = 2;
        public static final int CHANGED = 3;
        public static final int SAME = 4;
        public static final int EMPTY = 5; // value should be null
        public Item(int state, Object value) {
            this.state = state;
            this.value = state == EMPTY ? null : value;
        }

        public final Object value;
        public final int state;
    }

    public ArrayList<Item> referenceDiff;
    public ArrayList<Item> currentDiff;
    Object[] reference;
    Object[] current;

    /**
     * The arguments will _not_ be modified
     */
    public TwoColumnDiff(Object[] reference, Object[] current) {
        this.reference = reference;
        this.current = current;
        referenceDiff = new ArrayList<Item>();
        currentDiff = new ArrayList<Item>();
        diff();
    }
    private void diff() {
        Diff diff = new Diff(reference, current);
        Diff.change script = diff.diff_2(false);
        twoColumnDiffFromScript(script, reference, current);
    }

    /**
     * The result from the diff algorithm is a "script" (a compressed description of the changes)
     * This method expands this script into a full two column description.
     */
    private void twoColumnDiffFromScript(Diff.change script, Object[] a, Object[] b) {
        int ia = 0;
        int ib = 0;

        while(script != null) {
            int deleted = script.deleted;
            int inserted = script.inserted;
            while(ia < script.line0 && ib < script.line1){
                // System.out.println(" "+a[ia] + "\t "+b[ib]);
                Item cell = new Item(Item.SAME, a[ia]);
                referenceDiff.add(cell);
                currentDiff.add(cell);
                ia++;
                ib++;
            }

            while(inserted > 0 || deleted > 0) {
                if(inserted > 0 && deleted > 0) {
                    // System.out.println("="+a[ia] + "\t="+b[ib]);
                    referenceDiff.add(new Item(Item.CHANGED, a[ia++]));
                    currentDiff.add(new Item(Item.CHANGED, b[ib++]));
                } else if(inserted > 0) {
                    // System.out.println("\t+" + b[ib]);
                    referenceDiff.add(new Item(Item.EMPTY, null));
                    currentDiff.add(new Item(Item.INSERTED, b[ib++]));
                } else if(deleted > 0) {
                    // System.out.println("-"+a[ia]);
                    referenceDiff.add(new Item(Item.DELETED, a[ia++]));
                    currentDiff.add(new Item(Item.EMPTY, null));
                }
                inserted--;
                deleted--;
            }
            script = script.link;
        }
        while(ia < a.length && ib < b.length) {
            // System.out.println((ia < a.length ? " "+a[ia]+"\t" : "\t") + (ib < b.length ? " "+b[ib] : ""));
            referenceDiff.add(new Item(Item.SAME, a[ia++]));
            currentDiff.add(new Item(Item.SAME, b[ib++]));
        }
    }
}
