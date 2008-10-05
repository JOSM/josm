// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.ShortCut;

/**
 * Displays a dialog with all users who have last edited something in the
 * selection area, along with the number of objects.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class UserListDialog extends ToggleDialog implements SelectionChangedListener {

	/**
	 * The display list.
	 */
	private final DefaultTableModel data = new DefaultTableModel() {
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 0 ? String.class : Integer.class;
		}
	};

	private JTable userTable = new JTable(data);

    private static User anonymousUser = User.get("(anonymous users)");

	public UserListDialog() {
		super(tr("Authors"), "userlist", tr("Open a list of people working on the selected objects."),
		ShortCut.registerShortCut("subwindow:authors", tr("Toggle authors window"), KeyEvent.VK_A, ShortCut.GROUP_LAYER), 150);

		data.setColumnIdentifiers(new String[]{tr("Author"),tr("# Objects"),"%"});
		userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(new JScrollPane(userTable), BorderLayout.CENTER);
		selectionChanged(Main.ds.getSelected());

		DataSet.selListeners.add(this);
	}

	@Override public void setVisible(boolean b) {
		super.setVisible(b);
		if (b)
			selectionChanged(Main.ds.getSelected());
	}

	/**
	 * Called when the selection in the dataset changed.
	 * @param newSelection The new selection array.
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (!isVisible())
			return;

		class UserCount {
			User user;
			int count;
			UserCount(User user, int count) { this.user=user; this.count=count; }
		}

		if (data == null)
			return; // selection changed may be received in base class constructor before init

		data.setRowCount(0);

		HashMap<User,UserCount> counters = new HashMap<User,UserCount>();
		int all = 0;
		for (OsmPrimitive p : newSelection) {
            User u = p.user;
            if (u == null) u = anonymousUser;
            UserCount uc = counters.get(u);
            if (uc == null)
                counters.put(u, uc = new UserCount(u, 0));
            uc.count++;
            all++;
		}
		UserCount[] ucArr = new UserCount[counters.size()];
		counters.values().toArray(ucArr);
		Arrays.sort(ucArr, new Comparator<UserCount>() {
			public int compare(UserCount a, UserCount b) {
				return (a.count<b.count) ? 1 : (a.count>b.count) ? -1 : 0;
			}
		});

		for (UserCount uc : ucArr) {
			data.addRow(new Object[] { uc.user.name, uc.count, uc.count * 100 / all });
		}
	}

}
