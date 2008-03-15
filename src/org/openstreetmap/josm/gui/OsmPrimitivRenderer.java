// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * Renderer that renders the objects from an OsmPrimitive as data.
 * 
 * Can be used in lists and tables.
 * 
 * @author imi
 * @author Frederik Ramm <frederik@remote.org>
 */
public class OsmPrimitivRenderer implements ListCellRenderer, TableCellRenderer {

	/**
	 * NameVisitor provides proper names and icons for OsmPrimitives
	 */
	private NameVisitor visitor = new NameVisitor();

	/**
	 * Default list cell renderer - delegate for ListCellRenderer operation
	 */
	private DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
	
	/**
	 * Default table cell renderer - delegate for TableCellRenderer operation
	 */
	private DefaultTableCellRenderer defaultTableCellRenderer = new DefaultTableCellRenderer();

	/**
	 * Adapter method supporting the ListCellRenderer interface.
	 */
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		Component def = defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		return renderer(def, (OsmPrimitive) value);
	}

	/**
	 * Adapter method supporting the TableCellRenderer interface. 
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component def = defaultTableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		return renderer(def, (OsmPrimitive) value);
	}
	
	/**
	 * Internal method that stuffs information into the rendering component
	 * provided that it's a kind of JLabel.
	 * @param def the rendering component
	 * @param value the OsmPrimtive to render
	 * @return the modified rendering component
	 */
	private Component renderer(Component def, OsmPrimitive value) {
		if (def != null && value != null && def instanceof JLabel) {
			(value).visit(visitor);
			((JLabel)def).setText(visitor.name);
			((JLabel)def).setIcon(visitor.icon);
		}
		return def;
	}
	
}
