// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.JComboBox;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * @author Matthias Julius
 */
public class OsmPrimitiveTypesComboBox extends JComboBox {

    public OsmPrimitiveTypesComboBox() {
        for (OsmPrimitiveType type: OsmPrimitiveType.values()){
            addItem(type);
        }
    }

    public OsmPrimitiveType getType() {
        return (OsmPrimitiveType)this.getSelectedItem();
    }
}
