// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Unit tests of {@link GenericRelationEditor} class.
 */
public class GenericRelationEditorTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link GenericRelationEditor#addPrimitivesToRelation}.
     */
    @Test
    public void testAddPrimitivesToRelation() {
        assertNull(GenericRelationEditor.addPrimitivesToRelation(new Relation(1), Collections.<OsmPrimitive>emptyList()));
        assertNull(GenericRelationEditor.addPrimitivesToRelation(new Relation(1), Collections.singleton(new Relation(1))));

        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(new Relation(1), Collections.singleton(new Node(1))));
        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(new Relation(1), Collections.singleton(new Way(1))));
        assertNotNull(GenericRelationEditor.addPrimitivesToRelation(new Relation(1), Collections.singleton(new Relation(2))));
    }

}
