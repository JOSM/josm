// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Displays a dialog with all users who have last edited something in the
 * selection area, along with the number of objects.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class UserListDialog extends ToggleDialog implements SelectionChangedListener, MouseListener, LayerChangeListener {

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
                Shortcut.registerShortcut("subwindow:authors", tr("Toggle: {0}", tr("Authors")), KeyEvent.VK_A, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);

        data.setColumnIdentifiers(new String[]{tr("Author"),tr("# Objects"),"%"});
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(userTable), BorderLayout.CENTER);
        if (Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
        userTable.addMouseListener(this);
        DataSet.selListeners.add(this);
        Layer.listeners.add(this);
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
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
            if (u == null) {
                u = anonymousUser;
            }
            UserCount uc = counters.get(u);
            if (uc == null) {
                counters.put(u, uc = new UserCount(u, 0));
            }
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

        if(ucArr.length != 0) {
            setTitle(tr("Authors: {0}", ucArr.length));
        } else {
            setTitle(tr("Authors"));
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount()==2) {
            int index = userTable.getSelectedRow();
            String userName = (String) data.getValueAt(index, 0);
            if (userName==null)
                return;
            Collection<OsmPrimitive> selected = Main.main.getCurrentDataSet().getSelected();
            Collection<OsmPrimitive> byUser = new LinkedList<OsmPrimitive>();
            for (OsmPrimitive p : selected) {
                if (p.user!= null && userName.equals(p.user.name)) {
                    byUser.add(p);
                }
            }
            Main.main.getCurrentDataSet().setSelected(byUser);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (newLayer instanceof OsmDataLayer) {
            OsmDataLayer dataLayer = (OsmDataLayer)newLayer;
            selectionChanged(dataLayer.data.getSelected());

        }
    }

    public void layerAdded(Layer newLayer) {
        // do nothing
    }

    public void layerRemoved(Layer oldLayer) {
        // do nothing
    }
}
