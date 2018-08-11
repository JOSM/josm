// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Displays a dialog with all users who have last edited something in the
 * selection area, along with the number of objects.
 * @since 237
 */
public class UserListDialog extends ToggleDialog implements DataSelectionListener, ActiveLayerChangeListener {

    /**
     * The display list.
     */
    private JTable userTable;
    private UserTableModel model;
    private SelectUsersPrimitivesAction selectionUsersPrimitivesAction;
    private final JPopupMenu popupMenu = new JPopupMenu();

    /**
     * Constructs a new {@code UserListDialog}.
     */
    public UserListDialog() {
        super(tr("Authors"), "userlist", tr("Open a list of people working on the selected objects."),
                Shortcut.registerShortcut("subwindow:authors", tr("Toggle: {0}", tr("Authors")), KeyEvent.VK_A, Shortcut.ALT_SHIFT), 150);
        build();
    }

    @Override
    public void showNotify() {
        SelectionEventManager.getInstance().addSelectionListenerForEdt(this);
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
    }

    @Override
    public void hideNotify() {
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
        SelectionEventManager.getInstance().removeSelectionListener(this);
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
        ShowUserInfoAction showUserInfoAction = new ShowUserInfoAction();
        userTable.getSelectionModel().addListSelectionListener(showUserInfoAction);

        createLayout(userTable, true, Arrays.asList(
            new SideButton(selectionUsersPrimitivesAction),
            new SideButton(showUserInfoAction)
        ));

        // -- popup menu
        popupMenu.add(new AbstractAction(tr("Copy")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClipboardUtils.copyString(getSelectedUsers().stream().map(User::getName).collect(Collectors.joining(", ")));
            }
        });
        userTable.addMouseListener(new PopupMenuLauncher(popupMenu));
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        refresh(event.getSelection());
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        Layer activeLayer = e.getSource().getActiveLayer();
        refreshForActiveLayer(activeLayer);
    }

    private void refreshForActiveLayer(Layer activeLayer) {
        if (activeLayer instanceof OsmDataLayer) {
            refresh(((OsmDataLayer) activeLayer).data.getAllSelected());
        } else {
            refresh(null);
        }
    }

    /**
     * Refreshes user list from given collection of OSM primitives.
     * @param fromPrimitives OSM primitives to fetch users from
     */
    public void refresh(Collection<? extends OsmPrimitive> fromPrimitives) {
        GuiHelper.runInEDT(() -> {
            model.populate(fromPrimitives);
            if (model.getRowCount() != 0) {
                setTitle(trn("{0} Author", "{0} Authors", model.getRowCount(), model.getRowCount()));
            } else {
                setTitle(tr("Authors"));
            }
        });
    }

    @Override
    public void showDialog() {
        super.showDialog();
        refreshForActiveLayer(MainApplication.getLayerManager().getActiveLayer());
    }

    private List<User> getSelectedUsers() {
        int[] rows = userTable.getSelectedRows();
        return rows.length == 0 ? Collections.emptyList() : model.getSelectedUsers(rows);
    }

    class SelectUsersPrimitivesAction extends AbstractAction implements ListSelectionListener {

        /**
         * Constructs a new {@code SelectUsersPrimitivesAction}.
         */
        SelectUsersPrimitivesAction() {
            putValue(NAME, tr("Select"));
            putValue(SHORT_DESCRIPTION, tr("Select objects submitted by this user"));
            new ImageProvider("dialogs", "select").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        public void select() {
            int[] indexes = userTable.getSelectedRows();
            if (indexes.length == 0)
                return;
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

    /**
     * Action for launching the info page of a user.
     */
    class ShowUserInfoAction extends AbstractInfoAction implements ListSelectionListener {

        ShowUserInfoAction() {
            super(false);
            putValue(NAME, tr("Show info"));
            putValue(SHORT_DESCRIPTION, tr("Launches a browser with information about the user"));
            new ImageProvider("help/internet").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<User> users = getSelectedUsers();
            if (users.isEmpty())
                return;
            if (users.size() > 10) {
                Logging.warn(tr("Only launching info browsers for the first {0} of {1} selected users", 10, users.size()));
            }
            int num = Math.min(10, users.size());
            Iterator<User> it = users.iterator();
            while (it.hasNext() && num > 0) {
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
            if (infoObject instanceof User) {
                User user = (User) infoObject;
                return Config.getUrls().getBaseUserUrl() + '/' + Utils.encodeUrl(user.getName()).replaceAll("\\+", "%20");
            } else {
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
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                selectionUsersPrimitivesAction.select();
            }
        }
    }

    /**
     * Action for selecting the primitives contributed by the currently selected users.
     *
     */
    private static class UserInfo implements Comparable<UserInfo> {
        public final User user;
        public final int count;
        public final double percent;

        UserInfo(User user, int count, double percent) {
            this.user = user;
            this.count = count;
            this.percent = percent;
        }

        @Override
        public int compareTo(UserInfo o) {
            if (count < o.count)
                return 1;
            if (count > o.count)
                return -1;
            if (user == null || user.getName() == null)
                return 1;
            if (o.user == null || o.user.getName() == null)
                return -1;
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
        private final transient List<UserInfo> data;

        UserTableModel() {
            setColumnIdentifiers(new String[]{tr("Author"), tr("# Objects"), "%"});
            data = new ArrayList<>();
        }

        protected Map<User, Integer> computeStatistics(Collection<? extends OsmPrimitive> primitives) {
            Map<User, Integer> ret = new HashMap<>();
            if (primitives == null || primitives.isEmpty())
                return ret;
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
            GuiHelper.assertCallFromEdt();
            Map<User, Integer> statistics = computeStatistics(primitives);
            data.clear();
            if (primitives != null) {
                for (Map.Entry<User, Integer> entry: statistics.entrySet()) {
                    data.add(new UserInfo(entry.getKey(), entry.getValue(), (double) entry.getValue() / (double) primitives.size()));
                }
            }
            Collections.sort(data);
            this.fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            if (data == null)
                return 0;
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            UserInfo info = data.get(row);
            switch(column) {
            case 0: /* author */ return info.getName() == null ? "" : info.getName();
            case 1: /* count */ return info.count;
            case 2: /* percent */ return NumberFormat.getPercentInstance().format(info.percent);
            default: return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void selectPrimitivesOwnedBy(int... rows) {
            Set<User> users = new HashSet<>();
            for (int index: rows) {
                users.add(data.get(index).user);
            }
            OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
            Collection<? extends IPrimitive> selected = ds.getAllSelected();
            Collection<IPrimitive> byUser = new LinkedList<>();
            for (IPrimitive p : selected) {
                if (users.contains(p.getUser())) {
                    byUser.add(p);
                }
            }
            ds.setSelected(byUser);
        }

        public List<User> getSelectedUsers(int... rows) {
            List<User> ret = new LinkedList<>();
            if (rows == null || rows.length == 0)
                return ret;
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
