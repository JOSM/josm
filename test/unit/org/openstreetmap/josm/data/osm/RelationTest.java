// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.junit.Test;

import static org.junit.Assert.*;

public class RelationTest {
    @Test(expected=NullPointerException.class)
    public void createNewRelation() {
        new Relation(null);
    }

    @Test
    public void equalSemenaticsToNull() {
        Relation relation = new Relation();
        assertFalse(relation.hasEqualTechnicalAttributes(null));
    }

}
