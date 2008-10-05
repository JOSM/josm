// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;

import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ShortCut;
import org.openstreetmap.josm.gui.preferences.prefJPanel;

public class ShortcutPreference implements PreferenceSetting {

	private DefaultTableModel model;

	public void addGui(PreferenceDialog gui) {
		// icon source: http://www.iconfinder.net/index.php?q=key&page=icondetails&iconid=8553&size=128&q=key&s12=on&s16=on&s22=on&s32=on&s48=on&s64=on&s128=on
		// icon licence: GPL
		// icon designer: Paolino, http://www.paolinoland.it/
		// icon original filename: keyboard.png
		// icon original size: 128x128
		// modifications: icon was cropped, then resized
		JPanel p = gui.createPreferenceTab("shortcuts", tr("Shortcut Preferences"), tr("Changing keyboard shortcuts manually."));

		prefJPanel prefpanel = new prefJPanel(new scListModel());
		p.add(prefpanel, GBC.eol().fill(GBC.BOTH));

	}

	public void ok() {
   }

	// Maybe move this to prefPanel? There's no need for it to be here.
	private class scListModel extends AbstractTableModel {
		private String[] columnNames = new String[]{tr("ID"),tr("Action"), tr("Group"), tr("Shortcut")};
		private Collection<ShortCut> data;
		public scListModel() {
			data = ShortCut.listAll();
		}
		public int getColumnCount() {
			return columnNames.length;
		}
		public int getRowCount() {
			return data.size();
		}
		public String getColumnName(int col) {
			return columnNames[col];
		}
		public Object getValueAt(int row, int col) {
			ShortCut sc = (ShortCut)data.toArray()[row];
			if (col == 0) {
				return sc.getShortText();
			} else if (col == 1) {
				return sc.getLongText();
			} else if (col == 2) {
				if (sc.getRequestedGroup() == ShortCut.GROUP_NONE) {
					return tr("None");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_HOTKEY) {
					return tr("Hotkey");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_MENU) {
					return tr("Menu");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_EDIT) {
					return tr("Edit");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_LAYER) {
					return tr("Subwindow");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_DIRECT) {
					return tr("Direct");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_MNEMONIC) {
					return tr("Mnemonic");
				} else if (sc.getRequestedGroup() == ShortCut.GROUP_RESERVED) {
					return tr("System");
				} else {
					return tr("Unknown");
				}
			} else if (col == 3) {
				return sc.getKeyText();
			} else {
				// This is a kind of hack that allows the actions on the editing controls
				// to access the underlying shortcut object without introducing another
				// method. I opted to stay within the interface.
				return sc;
			}
		}
		public boolean isCellEditable(int row, int col) {
			return false;
		}
	}

}
