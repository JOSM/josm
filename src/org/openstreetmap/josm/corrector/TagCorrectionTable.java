// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Font;

public class TagCorrectionTable extends JTable {

	public static class BoldRenderer extends JLabel implements TableCellRenderer {

		public Component getTableCellRendererComponent(JTable table,
		        Object value, boolean isSelected, boolean hasFocus, int row,
		        int column) {

			Font f = getFont();
			setFont(new Font(f.getName(), f.getStyle() | Font.BOLD, f.getSize()));

			setText((String)value);

			return this;
		}
	}

	private static TableCellRenderer boldRenderer = null;

	public static TagCorrectionTable create(List<TagCorrection> tagCorrections) {
		TagCorrectionTableModel tagCorrectionTableModel = new TagCorrectionTableModel(tagCorrections);
		TagCorrectionTable table = new TagCorrectionTable(
		        tagCorrectionTableModel);
		int lines = tagCorrections.size() > 10 ? 10 : tagCorrections.size();  
		table.setPreferredScrollableViewportSize(new Dimension(400, lines * table.getRowHeight()));
		table.getColumnModel().getColumn(4).setPreferredWidth(40);
		table.setRowSelectionAllowed(false);

		return table;
	}

	public TableCellRenderer getCellRenderer(int row, int column) {
		TagCorrection tagCorrection = getTagCorrectionTableModel().tagCorrections
		        .get(row);
		if ((column == 2 && tagCorrection.isKeyChanged())
		        || (column == 3 && tagCorrection.isValueChanged())) {
			if (boldRenderer == null)
				boldRenderer = new BoldRenderer();
			return boldRenderer;
		}
		return super.getCellRenderer(row, column);
	}

	private TagCorrectionTable(TagCorrectionTableModel tagCorrectionTableModel) {
		super(tagCorrectionTableModel);
	}

	public TagCorrectionTableModel getTagCorrectionTableModel() {
		return (TagCorrectionTableModel) getModel();
	}

}
