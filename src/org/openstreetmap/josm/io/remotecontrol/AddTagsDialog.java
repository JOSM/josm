// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.GBC;

/**
 * 
 * @author master
 * 
 * Dialog to add tags as part of the remotecontrol
 * Existing Keys get grey color and unchecked selectboxes so they will not overwrite the old Key-Value-Pairs by default.
 * You can choose the tags you want to add by selectboxes. You can edit the tags before you apply them.
 *
 */
public class AddTagsDialog extends ExtendedDialog implements SelectionChangedListener {


	private final JTable propertyTable;
	private Collection<? extends OsmPrimitive> sel;
	boolean[] existing;

	public AddTagsDialog(String[][] tags) {
		super(Main.parent, tr("Add tags to selected objects"), new String[] { tr("Add tags"), tr("Cancel")},
				false,
				true);

		DataSet.addSelectionListener(this);


		DefaultTableModel tm = new DefaultTableModel(new String[] {tr("Assume"), tr("Key"), tr("Value")}, tags.length) {
			@Override
			public Class getColumnClass(int c) {
				return getValueAt(0, c).getClass();
			}

		};

		sel = Main.main.getCurrentDataSet().getSelected();
		existing = new boolean[tags.length];

		for (int i = 0; i<tags.length; i++) {
			existing[i] = false;
			String key = tags[i][0];
			Boolean b = Boolean.TRUE;
			for (OsmPrimitive osm : sel) {
				if (osm.keySet().contains(key)) {
					b = Boolean.FALSE;
					existing[i]=true;
					break;
				}
			}
			tm.setValueAt(b, i, 0);
			tm.setValueAt(tags[i][0], i, 1);
			tm.setValueAt(tags[i][1], i, 2);
		}

		propertyTable = new JTable(tm) {

			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (existing[row]) {
					c.setFont(c.getFont().deriveFont(Font.ITALIC));
					c.setForeground(new Color(100, 100, 100));
				} else {
					c.setFont(c.getFont().deriveFont(Font.PLAIN));
					c.setForeground(new Color(0, 0, 0));
				}
				return c;
			}
		};

		// a checkbox has a size of 15 px
		propertyTable.getColumnModel().getColumn(0).setMaxWidth(15);
		// get edit results if the table looses the focus, for example if a user clicks "add tags"
		propertyTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		// set the content of this AddTagsDialog consisting of the tableHeader and the table itself.
		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new GridBagLayout());
		tablePanel.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
		tablePanel.add(propertyTable, GBC.eol().fill(GBC.BOTH));
		setContent(tablePanel);

		// set the default Dimensions and show the dialog
		setPreferredSize(new Dimension(400,tablePanel.getPreferredSize().height+100));
		showDialog();
	}

	/**
	 * This method looks for existing tags in the current selection and sets the corresponding boolean in the boolean array existing[]
	 */
	private void findExistingTags() {
		TableModel tm = propertyTable.getModel();
		for (int i=0; i<tm.getRowCount(); i++) {
			String key = (String)tm.getValueAt(i, 1);
			existing[i] = false;
			for (OsmPrimitive osm : sel) {
				if (osm.keySet().contains(key)) {
					existing[i] = true;
					break;
				}
			}
		}
		propertyTable.repaint();
	}

	/**
	 * If you click the "Add tags" button build a ChangePropertyCommand for every key that has a checked checkbox to apply the key value pair to all selected osm objects.
	 * You get a entry for every key in the command queue.
	 */
	@Override
	protected void buttonAction(int buttonIndex, ActionEvent evt) {
		if (buttonIndex == 0) {
			TableModel tm = propertyTable.getModel();
			for (int i=0; i<tm.getRowCount(); i++) {
				if ((Boolean)tm.getValueAt(i, 0)) {
					Main.main.undoRedo.add(new ChangePropertyCommand(sel, (String)tm.getValueAt(i, 1), (String)tm.getValueAt(i, 2)));
				}
			}
		}
		setVisible(false);
	}

	@Override
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		sel = newSelection;
		findExistingTags();
	}

}
