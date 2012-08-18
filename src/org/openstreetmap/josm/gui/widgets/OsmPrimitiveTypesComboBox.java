// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * A Combo box containing OSM primitive types (Node, Way, Relation).
 * @author Matthias Julius
 * @see OsmPrimitiveType#dataValues
 * @since 2923
 */
public class OsmPrimitiveTypesComboBox extends JosmComboBox {

    /**
     * Constructs a new {@code OsmPrimitiveTypesComboBox}.
     */
    public OsmPrimitiveTypesComboBox() {
        super(OsmPrimitiveType.dataValues().toArray());
    }

    /**
     * Replies the currently selected {@code OsmPrimitiveType}.
     * @return the currently selected {@code OsmPrimitiveType}.
     */
    public OsmPrimitiveType getType() {
        Object selectedItem = this.getSelectedItem();
        return selectedItem instanceof OsmPrimitiveType ? (OsmPrimitiveType) selectedItem : null;
    }
}
