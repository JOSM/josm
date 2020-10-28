// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

class RelationMemberMergerTestFT extends JFrame {

    private RelationMemberMerger merger;

    protected void populate() {
        Relation r1 = new Relation();
        r1.addMember(new RelationMember("role1", new Node(1)));
        r1.addMember(new RelationMember("role2", new Way(2)));
        r1.addMember(new RelationMember("role3", new Relation(3)));


        Relation r2 = new Relation();
        r2.addMember(new RelationMember("role1", new Node(1)));
        r2.addMember(new RelationMember("role2", new Way(2)));
        r2.addMember(new RelationMember("role3", new Relation(3)));

        merger.populate(new Conflict<OsmPrimitive>(r1, r2));

    }

    protected void build() {
        merger = new RelationMemberMerger();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(merger, BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code RelationMemberMergerTest}.
     */
    RelationMemberMergerTestFT() {
        build();
        populate();
    }

    public static void main(String[] args) {
        RelationMemberMergerTestFT test = new RelationMemberMergerTestFT();
        test.setSize(600, 600);
        test.setVisible(true);
    }
}
