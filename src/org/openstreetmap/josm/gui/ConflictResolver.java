// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictItem;
import org.openstreetmap.josm.data.conflict.DeleteConflict;
import org.openstreetmap.josm.data.conflict.PositionConflict;
import org.openstreetmap.josm.data.conflict.PropertyConflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A panel which implement the conflict resolving of a set of primitive-pairs. There will be
 * three tables in the screen, one for each both sides and one resulting table. The user can
 * move items from either one of the sides ("my" and "their") to the resulting table.
 * 
 * @author Imi
 */
public class ConflictResolver extends JPanel {

	public static enum Resolution {MY, THEIR}

	private final class ConflictTableModel implements TableModel {
		private final Resolution resolution;
		public ConflictTableModel(Resolution resolution) {
			this.resolution = resolution;
		}

		public int getRowCount() {
			return conflicts.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			ConflictItem ci = conflicts.get(rowIndex);
			if (columnIndex == 0)
				return ci.key();
			Resolution r = resolution == null ? ci.resolution : resolution;
			if (r == null)
				return "<html><i>???</i></html>";
			JLabel l = new JLabel(r == Resolution.MY ? ci.my : ci.their);
			if (ci.resolution == resolution && resolution != null)
				l.setFont(l.getFont().deriveFont(Font.BOLD));
			return l;
		}

		public String getColumnName(int columnIndex) {return columnIndex == 0 ? tr("Key") : tr("Value");}
		public int getColumnCount() {return 2;}
		public boolean isCellEditable(int row, int column) {return false;}
		public Class<?> getColumnClass(int columnIndex) {return Object.class;}

		public void addTableModelListener(TableModelListener l) {}
		public void removeTableModelListener(TableModelListener l) {}
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
	}

	private final class DblClickListener extends MouseAdapter {
		private final Resolution resolution;
		public DblClickListener(Resolution resolution) {
			this.resolution = resolution;
		}
		@Override public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() >= 2) {
				int sel = ((JTable)e.getSource()).getSelectedRow();
				if (sel == -1)
					return;
				ConflictResolver.this.conflicts.get(sel).resolution = resolution;
				repaint();
			}
		}
	}
	private final class ResolveAction extends AbstractAction {
		private final Resolution resolution;
		public ResolveAction(String name, Resolution resolution) {
			super(null, ImageProvider.get("dialogs", name));
			this.resolution = resolution;
		}
		public void actionPerformed(ActionEvent e) {
			int sel = myTable.getSelectedRow();
			if (sel == -1)
				return;
			conflicts.get(sel).resolution = resolution;
			if (sel == myTable.getRowCount()-1)
				myTable.clearSelection();
			else
				myTable.getSelectionModel().setSelectionInterval(sel+1, sel+1);
			repaint();
		}
	}

	public final List<ConflictItem> conflicts = new ArrayList<ConflictItem>();

	private final ConflictTableModel my = new ConflictTableModel(Resolution.MY);
	private final JTable myTable;
	private final ConflictTableModel their = new ConflictTableModel(Resolution.THEIR);
	private final JTable theirTable;
	private final ConflictTableModel resolve = new ConflictTableModel(null);
	private final JTable resolveTable;

	
	public ConflictResolver(Map<OsmPrimitive, OsmPrimitive> conflicts) {
		super(new GridBagLayout());
		Collection<ConflictItem> possibleConflicts = new ArrayList<ConflictItem>();
		possibleConflicts.add(new DeleteConflict());
		possibleConflicts.add(new PositionConflict());
		TreeSet<String> allkeys = new TreeSet<String>();
		for (Entry<OsmPrimitive, OsmPrimitive> e : conflicts.entrySet()) {
			allkeys.addAll(e.getKey().keySet());
			allkeys.addAll(e.getValue().keySet());
		}
		for (String s : allkeys)
			possibleConflicts.add(new PropertyConflict(s));
		
		for (Entry<OsmPrimitive, OsmPrimitive> e : conflicts.entrySet()) {
			for (Iterator<ConflictItem> it = possibleConflicts.iterator(); it.hasNext();) {
				ConflictItem ci = it.next();
				if (ci.hasConflict(e.getKey(), e.getValue())) {
					ci.initialize(conflicts);
					this.conflicts.add(ci);
					it.remove();
				}
			}
		}
		

		// have to initialize the JTables here and not in the declaration, because its constructor
		// may access this.conflicts (indirectly)
		myTable = new JTable(my);
		theirTable = new JTable(their);
		resolveTable = new JTable(resolve);
		
		myTable.setPreferredScrollableViewportSize(new Dimension(250,70));
		theirTable.setPreferredScrollableViewportSize(new Dimension(250,70));
		resolveTable.setPreferredScrollableViewportSize(new Dimension(250,70));

		TableCellRenderer renderer = new DefaultTableCellRenderer(){
			final Font defFont = new JLabel().getFont();
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel c = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				c.setIcon(null);
				c.setFont(defFont);
				if (value instanceof JLabel) {
					JLabel l = (JLabel)value;
					String text = l.getText();
					c.setText(text);
					c.setFont(l.getFont());
					if (text.startsWith("<html>") && l.getFont().isBold())
						c.setText("<html>"+"<b>"+text.substring(6, text.length()-12));
				} else {
					String s = value.toString();
					int i = s.indexOf('|');
					if (i != -1) {
						c.setIcon(ImageProvider.get("data", s.substring(0,i)));
						c.setText(s.substring(i+1));
					}
				}
				return c;
			}
		};
		myTable.setDefaultRenderer(Object.class, renderer);
		theirTable.setDefaultRenderer(Object.class, renderer);
		resolveTable.setDefaultRenderer(Object.class, renderer);

		myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		theirTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resolveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionListener selListener = new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if (((ListSelectionModel)e.getSource()).isSelectionEmpty()) {
					myTable.clearSelection();
					theirTable.clearSelection();
					resolveTable.clearSelection();
				} else {
					int i = ((ListSelectionModel)e.getSource()).getMinSelectionIndex();
					myTable.scrollRectToVisible(myTable.getCellRect(i, 0, true));
					myTable.getSelectionModel().setSelectionInterval(i, i);
					theirTable.scrollRectToVisible(theirTable.getCellRect(i, 0, true));
					theirTable.getSelectionModel().setSelectionInterval(i, i);
					resolveTable.scrollRectToVisible(resolveTable.getCellRect(i, 0, true));
					resolveTable.getSelectionModel().setSelectionInterval(i, i);
				}
			}
		};
		myTable.getSelectionModel().addListSelectionListener(selListener);
		theirTable.getSelectionModel().addListSelectionListener(selListener);
		resolveTable.getSelectionModel().addListSelectionListener(selListener);
		myTable.getSelectionModel().setSelectionInterval(0,0);

		myTable.addMouseListener(new DblClickListener(Resolution.MY));
		theirTable.addMouseListener(new DblClickListener(Resolution.THEIR));
		resolveTable.addMouseListener(new DblClickListener(null));

		add(new JLabel(trn("{0} object has conflicts:","{0} objects have conflicts:",conflicts.size(),conflicts.size())), GBC.eol().insets(0,0,0,10));

		JPanel p = new JPanel(new GridBagLayout());
		p.add(new JLabel(tr("my version:")), GBC.eol());
		p.add(new JScrollPane(myTable), GBC.eol().fill(GBC.BOTH));
		p.add(new JButton(new ResolveAction("down", Resolution.MY)), GBC.eol().anchor(GBC.CENTER).insets(0,5,0,0));
		add(p, GBC.std().insets(0,0,5,0));

		p = new JPanel(new GridBagLayout());
		p.add(new JLabel(tr("their version:")), GBC.eol());
		p.add(new JScrollPane(theirTable), GBC.eol().fill(GBC.BOTH));
		p.add(new JButton(new ResolveAction("down", Resolution.THEIR)), GBC.eol().anchor(GBC.CENTER).insets(0,5,0,0));
		add(p, GBC.eop().insets(5,0,0,0));

		add(new JButton(new ResolveAction("up", null)), GBC.eol().anchor(GBC.CENTER));
		add(new JLabel(tr("resolved version:")), GBC.eol().insets(0,5,0,0));
		add(new JScrollPane(resolveTable), GBC.eol().anchor(GBC.CENTER).fill(GBC.BOTH));
	}
}
