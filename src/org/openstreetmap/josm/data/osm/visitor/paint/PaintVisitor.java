// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Graphics2D;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.NavigatableComponent;

public interface PaintVisitor {
    void setGraphics(Graphics2D g);
    void setNavigatableComponent(NavigatableComponent nc);
    void setInactive(boolean inactive);
    void visitAll(DataSet data, boolean virtual, Bounds box);
}
