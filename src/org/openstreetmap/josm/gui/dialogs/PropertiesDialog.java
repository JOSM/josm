// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.ForwardActionListener;
import org.openstreetmap.josm.gui.tagging.TaggingCellRenderer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.tools.AutoCompleteComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This dialog displays the properties of the current selected primitives.
 *
 * If no object is selected, the dialog list is empty.
 * If only one is selected, all properties of this object are selected.
 * If more than one object are selected, the sum of all properties are displayed. If the
 * different objects share the same property, the shared value is displayed. If they have
 * different values, all of them are put in a combo box and the string "&lt;different&gt;"
 * is displayed in italic.
 *
 * Below the list, the user can click on an add, modify and delete property button to
 * edit the table selection value.
 *
 * The command is applied to all selected entries.
 *
 * @author imi
 */
public class PropertiesDialog extends ToggleDialog implements SelectionChangedListener {

	/**
	 * Used to display relation names in the membership table
	 */
	private NameVisitor nameVisitor = new NameVisitor();
	
	/**
	 * Watches for double clicks and from editing or new property, depending on the
	 * location, the click was.
	 * @author imi
	 */
	public class DblClickWatch extends MouseAdapter {
		@Override public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() < 2)
				return;
	
			if (e.getSource() == propertyTable)
			{
				int row = propertyTable.rowAtPoint(e.getPoint());
				if (row > -1) {
					propertyEdit(row);
					return;
			}
			} else if (e.getSource() == membershipTable) {
				int row = membershipTable.rowAtPoint(e.getPoint());
				if (row > -1) {
					membershipEdit(row);
					return;
				}
			}
			add();
		}
	}

	/**
	 * Edit the value in the properties table row
	 * @param row The row of the table from which the value is edited.
	 */
	void propertyEdit(int row) {
		String key = propertyData.getValueAt(row, 0).toString();
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (sel.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select the objects you want to change properties for."));
			return;
		}
		String msg = "<html>"+trn("This will change {0} object.", "This will change {0} objects.", sel.size(), sel.size())+"<br><br>("+tr("An empty value deletes the key.", key)+")</html>";
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(msg), BorderLayout.NORTH);

		final TreeMap<String, TreeSet<String>> allData = createAutoCompletionInfo(true);

		JPanel p = new JPanel(new GridBagLayout());
		panel.add(p, BorderLayout.CENTER);

		final AutoCompleteComboBox keys = new AutoCompleteComboBox();
		keys.setPossibleItems(allData.keySet());
		keys.setEditable(true);
		keys.setSelectedItem(key);

		p.add(new JLabel(tr("Key")), GBC.std());
		p.add(Box.createHorizontalStrut(10), GBC.std());
		p.add(keys, GBC.eol().fill(GBC.HORIZONTAL));

		final AutoCompleteComboBox values = new AutoCompleteComboBox();
		values.setEditable(true);
		Collection<String> newItems;
		if (allData.containsKey(key)) {
			newItems = allData.get(key);
		} else {
			newItems = Collections.emptyList();
		}
		values.setPossibleItems(newItems);
		values.setSelectedItem(null);
		final String selection= ((JComboBox)propertyData.getValueAt(row, 1)).getEditor().getItem().toString(); 
		values.setSelectedItem(selection);
		values.getEditor().setItem(selection);
		p.add(new JLabel(tr("Value")), GBC.std());
		p.add(Box.createHorizontalStrut(10), GBC.std());
		p.add(values, GBC.eol().fill(GBC.HORIZONTAL));
		addFocusAdapter(allData, keys, values);

		final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
			@Override public void selectInitialValue() {
				values.requestFocusInWindow();
				values.getEditor().selectAll();
			}
		};
		final JDialog dlg = optionPane.createDialog(Main.parent, tr("Change values?"));

		values.getEditor().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dlg.setVisible(false);
				optionPane.setValue(JOptionPane.OK_OPTION);
			}
		});

		String oldValue = values.getEditor().getItem().toString();
		dlg.setVisible(true);

		Object answer = optionPane.getValue();
		if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE ||
				(answer instanceof Integer && (Integer)answer != JOptionPane.OK_OPTION)) {
			values.getEditor().setItem(oldValue);
			return;
		}

		String value = values.getEditor().getItem().toString();
		if (value.equals(tr("<different>")))
			return;
		if (value.equals(""))
			value = null; // delete the key
		String newkey = keys.getEditor().getItem().toString();
		if (newkey.equals("")) {
			newkey = key;
			value = null; // delete the key instead
		}
		if (key.equals(newkey) || value == null)
			Main.main.undoRedo.add(new ChangePropertyCommand(sel, newkey, value));
		else {
			Main.main.undoRedo.add(new SequenceCommand(trn("Change properties of {0} object", "Change properties of {0} objects", sel.size(), sel.size()),
					new ChangePropertyCommand(sel, key, null),
					new ChangePropertyCommand(sel, newkey, value)));
		}

			selectionChanged(sel); // update whole table
		Main.parent.repaint(); // repaint all - drawing could have been changed
	}

	/**
	 * This simply fires up an relation editor for the relation shown; everything else
	 * is the editor's business.
	 * 
	 * @param row
	 */
	void membershipEdit(int row) {	
		final RelationEditor editor = new RelationEditor((Relation)membershipData.getValueAt(row, 0));
		editor.setVisible(true);
	}

	/**
	 * Open the add selection dialog and add a new key/value to the table (and
	 * to the dataset, of course).
	 */
	void add() {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (sel.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select objects for which you want to change properties."));
			return;
		}

		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("<html>"+trn("This will change {0} object.","This will change {0} objects.", sel.size(),sel.size())+"<br><br>"+tr("Please select a key")),
				BorderLayout.NORTH);
		final TreeMap<String, TreeSet<String>> allData = createAutoCompletionInfo(false);
		final AutoCompleteComboBox keys = new AutoCompleteComboBox();
		keys.setPossibleItems(allData.keySet());
		keys.setEditable(true);
		
		p.add(keys, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout());
		p.add(p2, BorderLayout.SOUTH);
		p2.add(new JLabel(tr("Please select a value")), BorderLayout.NORTH);
		final AutoCompleteComboBox values = new AutoCompleteComboBox();
		values.setEditable(true);
		p2.add(values, BorderLayout.CENTER);

		JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
			@Override public void selectInitialValue() {
				keys.requestFocusInWindow();
				keys.getEditor().selectAll();
			}
		};
		pane.createDialog(Main.parent, tr("Change values?")).setVisible(true);
		if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
			return;
		String key = keys.getEditor().getItem().toString();
		String value = values.getEditor().getItem().toString();
		if (value.equals(""))
			return;
		Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, value));
		selectionChanged(sel); // update table
		Main.parent.repaint(); // repaint all - drawing could have been changed
	}

	/**
	 * @param allData
	 * @param keys
	 * @param values
	 */
	private void addFocusAdapter(
	        final TreeMap<String, TreeSet<String>> allData,
	        final AutoCompleteComboBox keys, final AutoCompleteComboBox values) {
		// get the combo box' editor component
		JTextComponent editor = (JTextComponent)values.getEditor()
		        .getEditorComponent();
		// Refresh the values model when focus is gained
		editor.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) {
				String key = keys.getEditor().getItem().toString();
				Collection<String> newItems;
				if (allData.containsKey(key)) {
					newItems = allData.get(key);
				} else {
					newItems = Collections.emptyList();
				}
				values.setPossibleItems(newItems);
			}
		});
	}

	/**
	 * @return
	 */
	private TreeMap<String, TreeSet<String>> createAutoCompletionInfo(
	        boolean edit) {
		final TreeMap<String, TreeSet<String>> allData = new TreeMap<String, TreeSet<String>>();
		for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives()) {
			for (String key : osm.keySet()) {
				TreeSet<String> values = null;
				if (allData.containsKey(key))
					values = allData.get(key);
				else {
					values = new TreeSet<String>();
					allData.put(key, values);
				}
				values.add(osm.get(key));
			}
		}
		if (!edit) {
			for (int i = 0; i < propertyData.getRowCount(); ++i)
				allData.remove(propertyData.getValueAt(i, 0));
		}
		return allData;
	}

	/**
	 * Delete the keys from the given row.
	 * @param row	The row, which key gets deleted from the dataset.
	 */
	private void delete(int row) {
		String key = propertyData.getValueAt(row, 0).toString();
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, null));
		selectionChanged(sel); // update table
	}

	/**
	 * The property data.
	 */
	private final DefaultTableModel propertyData = new DefaultTableModel() {
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 1 ? Relation.class : String.class;
		}
	};

	/**
	 * The membership data.
	 */
	private final DefaultTableModel membershipData = new DefaultTableModel() {
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}
	};
	
	/**
	 * The properties list.
	 */
	private final JTable propertyTable = new JTable(propertyData);
	private final JTable membershipTable = new JTable(membershipData);

	public JComboBox taggingPresets = new JComboBox();


	/**
	 * Create a new PropertiesDialog
	 */
	public PropertiesDialog(MapFrame mapFrame) {
		super(tr("Properties/Memberships"), "propertiesdialog", tr("Properties for selected objects."), KeyEvent.VK_P, 150);

		// ---------------------------------------
		// This drop-down is really deprecated but we offer people a chance to 
		// activate it if they really want. Presets should be used from the 
		// menu.
		if (TaggingPresetPreference.taggingPresets.size() > 0 && 
				Main.pref.getBoolean("taggingpresets.in-properties-dialog", false)) {
			Vector<ActionListener> allPresets = new Vector<ActionListener>();
			for (final TaggingPreset p : TaggingPresetPreference.taggingPresets)
				allPresets.add(new ForwardActionListener(this, p));

			TaggingPreset empty = new TaggingPreset();
			// empty.setName("this drop-down will be removed soon");
			allPresets.add(0, new ForwardActionListener(this, empty));
			taggingPresets.setModel(new DefaultComboBoxModel(allPresets));
			JPanel north = new JPanel(new GridBagLayout());
			north.add(getComponent(0),GBC.eol().fill(GBC.HORIZONTAL));
			north.add(taggingPresets,GBC.eol().fill(GBC.HORIZONTAL));
			add(north, BorderLayout.NORTH);
		}
		taggingPresets.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				TaggingPreset preset = ((ForwardActionListener)taggingPresets.getSelectedItem()).preset;
				preset.actionPerformed(e);
				taggingPresets.setSelectedItem(null);
			}
		});
		taggingPresets.setRenderer(new TaggingCellRenderer());

		// setting up the properties table
		
		propertyData.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
		propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	
		propertyTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
				if (c instanceof JLabel) {
					String str = ((JComboBox) value).getEditor().getItem().toString();
					((JLabel)c).setText(str);
					if (str.equals(tr("<different>")))
						c.setFont(c.getFont().deriveFont(Font.ITALIC));
				}
				return c;
			}
		});
		
		// setting up the membership table
		
		membershipData.setColumnIdentifiers(new String[]{tr("Member Of"),tr("Role")});
		membershipTable.setRowSelectionAllowed(false);
		
		membershipTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
				if (c instanceof JLabel) {
					nameVisitor.visit((Relation)value);
					((JLabel)c).setText(nameVisitor.name);
				}
				return c;
			}
		});
		
		// combine both tables and wrap them in a scrollPane
		JPanel bothTables = new JPanel();
		bothTables.setLayout(new GridBagLayout());
		bothTables.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
		bothTables.add(propertyTable, GBC.eol().fill(GBC.BOTH));
		bothTables.add(membershipTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
		bothTables.add(membershipTable, GBC.eol().fill(GBC.BOTH));
		
		DblClickWatch dblClickWatch = new DblClickWatch();
		propertyTable.addMouseListener(dblClickWatch);
		membershipTable.addMouseListener(dblClickWatch);
		JScrollPane scrollPane = new JScrollPane(bothTables);
		scrollPane.addMouseListener(dblClickWatch);
		add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,3));
		ActionListener buttonAction = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int sel = propertyTable.getSelectedRow();
				if (e.getActionCommand().equals("Add"))
					add();
				else if (e.getActionCommand().equals("Edit")) {
					if (sel == -1)
						JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to edit."));
					else
						propertyEdit(sel);
				} else if (e.getActionCommand().equals("Delete")) {
					if (sel == -1)
						JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."));
					else
						delete(sel);
				}
			}
		};
		
		buttonPanel.add(createButton(marktr("Add"),tr("Add a new key/value pair to all objects"), KeyEvent.VK_A, buttonAction));
		buttonPanel.add(createButton(marktr("Edit"),tr( "Edit the value of the selected key for all objects"), KeyEvent.VK_E, buttonAction));
		buttonPanel.add(createButton(marktr("Delete"),tr("Delete the selected key in all objects"), KeyEvent.VK_D, buttonAction));
		add(buttonPanel, BorderLayout.SOUTH);

		DataSet.selListeners.add(this);
	}

	private JButton createButton(String name, String tooltip, int mnemonic, ActionListener actionListener) {
		JButton b = new JButton(tr(name), ImageProvider.get("dialogs", name.toLowerCase()));
		b.setActionCommand(name);
		b.addActionListener(actionListener);
		b.setToolTipText(tooltip);
		b.setMnemonic(mnemonic);
		b.putClientProperty("help", "Dialog/Properties/"+name);
		return b;
	}

	@Override public void setVisible(boolean b) {
		super.setVisible(b);
		if (b)
			selectionChanged(Main.ds.getSelected());
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (!isVisible())
			return;
		if (propertyTable == null)
			return; // selection changed may be received in base class constructor before init
		if (propertyTable.getCellEditor() != null)
			propertyTable.getCellEditor().cancelCellEditing();

		// re-load property data
		
		propertyData.setRowCount(0);

		Map<String, Integer> valueCount = new HashMap<String, Integer>();
		TreeMap<String, Collection<String>> props = new TreeMap<String, Collection<String>>();
		for (OsmPrimitive osm : newSelection) {
			for (Entry<String, String> e : osm.entrySet()) {
				Collection<String> value = props.get(e.getKey());
				if (value == null) {
					value = new TreeSet<String>();
					props.put(e.getKey(), value);
				}
				value.add(e.getValue());
				valueCount.put(e.getKey(), valueCount.containsKey(e.getKey()) ? valueCount.get(e.getKey())+1 : 1);
			}
		}
		for (Entry<String, Collection<String>> e : props.entrySet()) {
            JComboBox value = new JComboBox(e.getValue().toArray());
            value.setEditable(true);
            value.getEditor().setItem(e.getValue().size() > 1 || valueCount.get(e.getKey()) != newSelection.size() ? tr("<different>") : e.getValue().iterator().next());
            propertyData.addRow(new Object[]{e.getKey(), value});
		}
		
		// re-load membership data
		// this is rather expensive since we have to walk through all members of all existing relationships.
		// could use back references here for speed if necessary.
		
		membershipData.setRowCount(0);
		
		Map<Relation, Integer> valueCountM = new HashMap<Relation, Integer>();
		TreeMap<Relation, Collection<String>> roles = new TreeMap<Relation, Collection<String>>();
		for (Relation r : Main.ds.relations) {
			if (!r.deleted && !r.incomplete) {
				for (RelationMember m : r.members) {
					if (newSelection.contains(m.member)) {
						Collection<String> value = roles.get(r);
						if (value == null) {
							value = new TreeSet<String>();
							roles.put(r, value);
						}
						value.add(m.role);
						valueCountM.put(r, valueCount.containsKey(r) ? valueCount.get(r)+1 : 1);
					}
				}
			}
		}
		
		for (Entry<Relation, Collection<String>> e : roles.entrySet()) {
			//JComboBox value = new JComboBox(e.getValue().toArray());
			//value.setEditable(true);
			//value.getEditor().setItem(e.getValue().size() > 1 || valueCount.get(e.getKey()) != newSelection.size() ? tr("<different>") : e.getValue().iterator().next());
			String value = e.getValue().size() > 1 || valueCountM.get(e.getKey()) != newSelection.size() ? tr("<different>") : e.getValue().iterator().next();
			membershipData.addRow(new Object[]{e.getKey(), value});
		}
	}
}
