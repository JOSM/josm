// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Displays a dialog with all users who have last edited something in the
 * selection area, along with the number of objects.
 *
 */
public class UserListDialog extends ToggleDialog implements SelectionChangedListener, MapView.LayerChangeListener {

    /**
     * The display list.
     */
    private JTable userTable;
    private UserTableModel model;
    private SelectUsersPrimitivesAction selectionUsersPrimitivesAction;
    private ShowUserInfoAction showUserInfoAction;

    public UserListDialog() {
        super(tr("Authors"), "userlist", tr("Open a list of people working on the selected objects."),
                Shortcut.registerShortcut("subwindow:authors", tr("Toggle: {0}", tr("Authors")), KeyEvent.VK_A, Shortcut.ALT_SHIFT), 150);

        build();
    }

    @Override
    public void showNotify() {
        DataSet.addSelectionListener(this);
        MapView.addLayerChangeListener(this);
    }

    @Override
    public void hideNotify() {
        MapView.removeLayerChangeListener(this);
        DataSet.removeSelectionListener(this);
    }

    protected void build() {
        model = new UserTableModel();
        userTable = new JTable(model);
        userTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userTable.addMouseListener(new DoubleClickAdapter());

        // -- select users primitives action
        //
        selectionUsersPrimitivesAction = new SelectUsersPrimitivesAction();
        userTable.getSelectionModel().addListSelectionListener(selectionUsersPrimitivesAction);

        // -- info action
        //
        showUserInfoAction = new ShowUserInfoAction();
        userTable.getSelectionModel().addListSelectionListener(showUserInfoAction);

        createLayout(userTable, true, Arrays.asList(new SideButton[] {
            new SideButton(selectionUsersPrimitivesAction),
            new SideButton(showUserInfoAction)
        }));
    }

    /**
     * Called when the selection in the dataset changed.
     * @param newSelection The new selection array.
     */
    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        refresh(newSelection);
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (newLayer instanceof OsmDataLayer) {
            refresh(((OsmDataLayer) newLayer).data.getAllSelected());
        } else {
            refresh(null);
        }
    }

    @Override
    public void layerAdded(Layer newLayer) {
        // do nothing
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        // do nothing
    }

    public void refresh(Collection<? extends OsmPrimitive> fromPrimitives) {
        model.populate(fromPrimitives);
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                if (model.getRowCount() != 0) {
                    setTitle(trn("{0} Author", "{0} Authors", model.getRowCount() , model.getRowCount()));
                } else {
                    setTitle(tr("Authors"));
                }
            }
        });
    }

    @Override
    public void showDialog() {
        super.showDialog();
        Layer layer = Main.main.getActiveLayer();
        if (layer instanceof OsmDataLayer) {
            refresh(((OsmDataLayer)layer).data.getAllSelected());
        }

    }

    class SelectUsersPrimitivesAction extends AbstractAction implements ListSelectionListener{
        public SelectUsersPrimitivesAction() {
            putValue(NAME, tr("Select"));
            putValue(SHORT_DESCRIPTION, tr("Select objects submitted by this user"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            updateEnabledState();
        }

        public void select() {
            int[] indexes = userTable.getSelectedRows();
            if (indexes == null || indexes.length == 0) return;
            model.selectPrimitivesOwnedBy(userTable.getSelectedRows());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select();
        }

        protected void updateEnabledState() {
            setEnabled(userTable != null && userTable.getSelectedRowCount() > 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /*
     * Action for launching the info page of a user
     */
    class ShowUserInfoAction extends AbstractInfoAction implements ListSelectionListener {

        public ShowUserInfoAction() {
            super(false);
            putValue(NAME, tr("Show info"));
            putValue(SHORT_DESCRIPTION, tr("Launches a browser with information about the user"));
            putValue(SMALL_ICON, ImageProvider.get("about"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = userTable.getSelectedRows();
            if (rows == null || rows.length == 0) return;
            List<User> users = model.getSelectedUsers(rows);
            if (users.isEmpty()) return;
            if (users.size() > 10) {
                Main.warn(tr("Only launching info browsers for the first {0} of {1} selected users", 10, users.size()));
            }
            int num = Math.min(10, users.size());
            Iterator<User> it = users.iterator();
            while(it.hasNext() && num > 0) {
                String url = createInfoUrl(it.next());
                if (url == null) {
                    break;
                }
                OpenBrowser.displayUrl(url);
                num--;
            }
        }

        @Override
        protected String createInfoUrl(Object infoObject) {
            User user = (User)infoObject;
            try {
                return getBaseUserUrl() + "/" + URLEncoder.encode(user.getName(), "UTF-8").replaceAll("\\+", "%20");
            } catch(UnsupportedEncodingException e) {
                Main.error(e);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to create an URL because the encoding ''{0}''<br>"
                                + "was missing on this system.</html>", "UTF-8"),
                                tr("Missing encoding"),
                                JOptionPane.ERROR_MESSAGE
                );
                return null;
            }
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(userTable != null && userTable.getSelectedRowCount() > 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class DoubleClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount()==2) {
                selectionUsersPrimitivesAction.select();
            }
        }
    }

    /**
     * Action for selecting the primitives contributed by the currently selected
     * users.
     *
     */
    private static class UserInfo implements Comparable<UserInfo> {
        public User user;
        public int count;
        public double percent;
        UserInfo(User user, int count, double percent) {
            this.user=user;
            this.count=count;
            this.percent = percent;
        }
        @Override
        public int compareTo(UserInfo o) {
            if (count < o.count) return 1;
            if (count > o.count) return -1;
            if (user== null || user.getName() == null) return 1;
            if (o.user == null || o.user.getName() == null) return -1;
            return user.getName().compareTo(o.user.getName());
        }

        public String getName() {
            if (user == null)
                return tr("<new object>");
            return user.getName();
        }
    }

    /**
     * The table model for the users
     *
     */
    static class UserTableModel extends DefaultTableModel {
        private List<UserInfo> data;

        public UserTableModel() {
            setColumnIdentifiers(new String[]{tr("Author"),tr("# Objects"),"%"});
            data = new ArrayList<UserInfo>();
        }

        protected Map<User, Integer> computeStatistics(Collection<? extends OsmPrimitive> primitives) {
            HashMap<User, Integer> ret = new HashMap<User, Integer>();
            if (primitives == null || primitives.isEmpty()) return ret;
            for (OsmPrimitive primitive: primitives) {
                if (ret.containsKey(primitive.getUser())) {
                    ret.put(primitive.getUser(), ret.get(primitive.getUser()) + 1);
                } else {
                    ret.put(primitive.getUser(), 1);
                }
            }
            return ret;
        }

        public void populate(Collection<? extends OsmPrimitive> primitives) {
            Map<User,Integer> statistics = computeStatistics(primitives);
            data.clear();
            if (primitives != null) {
                for (Map.Entry<User, Integer> entry: statistics.entrySet()) {
                    data.add(new UserInfo(entry.getKey(), entry.getValue(), (double)entry.getValue() /  (double)primitives.size()));
                }
            }
            Collections.sort(data);
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    fireTableDataChanged();
                }
            });
        }

        @Override
        public int getRowCount() {
            if (data == null) return 0;
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            UserInfo info = data.get(row);
            switch(column) {
            case 0: /* author */ return info.getName() == null ? "" : info.getName();
            case 1: /* count */ return info.count;
            case 2: /* percent */ return NumberFormat.getPercentInstance().format(info.percent);
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void selectPrimitivesOwnedBy(int [] rows) {
            Set<User> users= new HashSet<User>();
            for (int index: rows) {
                users.add(data.get(index).user);
            }
            Collection<OsmPrimitive> selected = Main.main.getCurrentDataSet().getAllSelected();
            Collection<OsmPrimitive> byUser = new LinkedList<OsmPrimitive>();
            for (OsmPrimitive p : selected) {
                if (users.contains(p.getUser())) {
                    byUser.add(p);
                }
            }
            Main.main.getCurrentDataSet().setSelected(byUser);
        }

        public List<User> getSelectedUsers(int[] rows) {
            LinkedList<User> ret = new LinkedList<User>();
            if (rows == null || rows.length == 0) return ret;
            for (int row: rows) {
                if (data.get(row).user == null) {
                    continue;
                }
                ret.add(data.get(row).user);
            }
            return ret;
        }
    }
}
