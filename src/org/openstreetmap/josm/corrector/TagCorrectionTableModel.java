// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class TagCorrectionTableModel extends AbstractTableModel {

	List<TagCorrection> tagCorrections;

	private boolean[] apply;

	public TagCorrectionTableModel(List<TagCorrection> tagCorrections) {
		this.tagCorrections = tagCorrections;
		apply = new boolean[this.tagCorrections.size()];
		Arrays.fill(apply, true);
	}

	public int getColumnCount() {
		return 5;
	}

	@Override public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 4)
			return Boolean.class;
		return String.class;
	}

	@Override public String getColumnName(int colIndex) {
		switch (colIndex) {
		case 0:
			return tr("Old key");
		case 1:
			return tr("Old value");
		case 2:
			return tr("New key");
		case 3:
			return tr("New value");
		case 4:
			return tr("Apply?");
		}
		return null;
	}

	public int getRowCount() {
		return tagCorrections.size();
	}

	public Object getValueAt(int rowIndex, int colIndex) {

		TagCorrection tagCorrection = tagCorrections.get(rowIndex);

		switch (colIndex) {
		case 0:
			return tagCorrection.oldKey;
		case 1:
			return tagCorrection.oldValue;
		case 2:
			return tagCorrection.newKey;
		case 3:
			return tagCorrection.newValue;
		case 4:
			return apply[rowIndex];
		}
		return null;
	}

	@Override public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 4;
	}

	@Override public void setValueAt(Object aValue, int rowIndex,
	        int columnIndex) {
		if (columnIndex == 4 && aValue instanceof Boolean)
			apply[rowIndex] = (Boolean)aValue;
	}

	public boolean getApply(int i) {
		return apply[i];
	}
}
