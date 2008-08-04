// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * History dialog works like follows:
 *
 * There is a history cache hold in the back for primitives of the last refresh.
 * When the user refreshes, this cache is cleared and all currently selected items
 * are reloaded.
 * If the user has selected at least one primitive not in the cache, the list
 * is not displayed. Elsewhere, the list of all changes of all currently selected
 * objects are displayed.
 *
 * @author imi
 */
public class HistoryDialog extends ToggleDialog implements SelectionChangedListener {

	public static final Date unifyDate(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		return c.getTime();
	}

	private static class HistoryItem implements Comparable<HistoryItem> {
		OsmPrimitive osm;
		boolean visible;

		public int compareTo(HistoryItem o) {
			return unifyDate(osm.getTimestamp()).compareTo(unifyDate(o.osm.getTimestamp()));
		}
	}

	private final DefaultTableModel data = new DefaultTableModel(){
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
	};

	/**
	 * Main table. 3 columns:
	 * Object | Date | visible (icon, no text)
	 */
	private JTable history = new JTable(data);
	private JScrollPane historyPane = new JScrollPane(history);

	private Map<OsmPrimitive, List<HistoryItem>> cache = new HashMap<OsmPrimitive, List<HistoryItem>>();
	private JLabel notLoaded = new JLabel("<html><i>"+tr("Click Reload to refresh list")+"</i></html>");

	public HistoryDialog() {
		super(tr("History"), "history", tr("Display the history of all selected items."), KeyEvent.VK_H, 150);
		historyPane.setVisible(false);
		notLoaded.setVisible(true);
		notLoaded.setHorizontalAlignment(JLabel.CENTER);

		history.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
		data.setColumnIdentifiers(new Object[]{tr("Object"),tr("Date"),""});
		history.getColumnModel().getColumn(0).setPreferredWidth(200);
		history.getColumnModel().getColumn(1).setPreferredWidth(200);
		history.getColumnModel().getColumn(2).setPreferredWidth(20);
		final TableCellRenderer oldRenderer = history.getTableHeader().getDefaultRenderer();
		history.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JComponent c = (JComponent)oldRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (!value.equals(""))
					return c;
				JLabel l = new JLabel(ImageProvider.get("misc","showhide"));
				l.setForeground(c.getForeground());
				l.setBackground(c.getBackground());
				l.setFont(c.getFont());
				l.setBorder(c.getBorder());
				l.setOpaque(true);
				return l;
			}
		});

		JPanel centerPanel = new JPanel(new GridBagLayout());
		centerPanel.add(notLoaded, GBC.eol().fill(GBC.BOTH));
		centerPanel.add(historyPane, GBC.eol().fill(GBC.BOTH));
		add(centerPanel, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(1,2));
		buttons.add(new SideButton(marktr("Reload"), "refresh", "History", tr("Reload all currently selected objects and refresh the list."),
		new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				reload();
			}
		}));
		buttons.add(new SideButton(marktr("Revert"), "revert", "History",
		tr("Revert the state of all currently selected objects to the version selected in the history list."), new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(Main.parent, tr("Not implemented yet."));
			}
		}));
		add(buttons, BorderLayout.SOUTH);

		DataSet.selListeners.add(this);
	}


	@Override public void setVisible(boolean b) {
		super.setVisible(b);
		if (b)
			update();
	}


	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (isVisible())
			update();
	}

	/**
	 * Identify all new objects in the selection and if any, hide the list.
	 * Else, update the list with the selected items shown.
	 */
	private void update() {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (!cache.keySet().containsAll(sel)) {
			historyPane.setVisible(false);
			notLoaded.setVisible(true);
		} else {
			SortedSet<HistoryItem> orderedHistory = new TreeSet<HistoryItem>();
			for (OsmPrimitive osm : sel)
				orderedHistory.addAll(cache.get(osm));
			data.setRowCount(0);
			for (HistoryItem i : orderedHistory)
				data.addRow(new Object[]{i.osm, i.osm.timestamp, i.visible});
			historyPane.setVisible(true);
			notLoaded.setVisible(false);
		}
	}

	void reload() {
		JOptionPane.showMessageDialog(Main.parent, tr("Not implemented yet."));
	}
}
