// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * @author Matthias Julius
 */
public class OsmPrimitiveTypesComboBox extends JosmComboBox {

    public OsmPrimitiveTypesComboBox() {
        super(OsmPrimitiveType.dataValues());
    }

    public OsmPrimitiveType getType() {
        try {
            return (OsmPrimitiveType)this.getSelectedItem();
        } catch (Exception e) {
            return null;
        }
    }
}
