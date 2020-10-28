// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import javax.swing.JFrame;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

class ConflictResolutionDialogTestFT extends JFrame {

    private ConflictResolutionDialog dialog;

    protected void build() {
        setSize(100, 100);
        dialog = new ConflictResolutionDialog(this);
        dialog.setSize(600, 600);
    }

    protected void populate() {
        Way w1 = new Way(1);
        w1.addNode(new Node(10));
        w1.addNode(new Node(11));

        Way w2 = new Way(1);
        w2.addNode(new Node(10));
        w2.addNode(new Node(11));

        dialog.getConflictResolver().populate(new Conflict<OsmPrimitive>(w1, w2));
    }

    public void showDialog() {
        dialog.showDialog();
    }

    /**
     * Constructs a new {@code ConflictResolutionDialogTest}.
     */
    ConflictResolutionDialogTestFT() {
        build();
    }

    public static void main(String[] args) {
        ConflictResolutionDialogTestFT test = new ConflictResolutionDialogTestFT();
        test.setVisible(true);
        test.populate();
        test.showDialog();
    }
}
