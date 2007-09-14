// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * Renderer that renders the objects from an OsmPrimitive as data.
 * @author imi
 */
public class OsmPrimitivRenderer extends DefaultListCellRenderer {

	private NameVisitor visitor = new NameVisitor();

	@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value != null) {
			((OsmPrimitive)value).visit(visitor);
			setText(visitor.name);
			setIcon(visitor.icon);
		}
		return this;
	}
}
