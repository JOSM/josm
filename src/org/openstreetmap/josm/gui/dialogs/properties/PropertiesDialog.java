// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.relation.DownloadMembersAction;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.actions.relation.SelectInRelationListAction;
import org.openstreetmap.josm.actions.relation.SelectMembersAction;
import org.openstreetmap.josm.actions.relation.SelectRelationAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel.PresetHandler;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

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
public class PropertiesDialog extends ToggleDialog implements SelectionChangedListener, MapView.EditLayerChangeListener, DataSetListenerAdapter.Listener {

    /**
     * hook for roadsigns plugin to display a small button in the upper right corner of this dialog
     */
    public static final JPanel pluginHook = new JPanel();

    /**
     * The property data of selected objects.
     */
    private final DefaultTableModel propertyData = new ReadOnlyTableModel();

    /**
     * The membership data of selected objects.
     */
    private final DefaultTableModel membershipData = new ReadOnlyTableModel();

    /**
     * The properties table.
     */
    private final JTable propertyTable = new JTable(propertyData);
    /**
     * The membership table.
     */
    private final JTable membershipTable = new JTable(membershipData);

    // Popup menus
    private final JPopupMenu propertyMenu = new JPopupMenu();
    private final JPopupMenu membershipMenu = new JPopupMenu();

    // Popup menu handlers
    private final PopupMenuHandler propertyMenuHandler = new PopupMenuHandler(propertyMenu);
    private final PopupMenuHandler membershipMenuHandler = new PopupMenuHandler(membershipMenu);

    private final Map<String, Map<String, Integer>> valueCount = new TreeMap<String, Map<String, Integer>>();
    /**
     * This sub-object is responsible for all adding and editing of properties
     */
    private final TagEditHelper editHelper = new TagEditHelper(propertyData, valueCount);

    private final DataSetListenerAdapter dataChangedAdapter = new DataSetListenerAdapter(this);
    private final HelpAction helpAction = new HelpAction();
    private final PasteValueAction pasteValueAction = new PasteValueAction();
    private final CopyValueAction copyValueAction = new CopyValueAction();
    private final CopyKeyValueAction copyKeyValueAction = new CopyKeyValueAction();
    private final CopyAllKeyValueAction copyAllKeyValueAction = new CopyAllKeyValueAction();
    private final SearchAction searchActionSame = new SearchAction(true);
    private final SearchAction searchActionAny = new SearchAction(false);
    private final AddAction addAction = new AddAction();
    private final EditAction editAction = new EditAction();
    private final DeleteAction deleteAction = new DeleteAction();
    private final JosmAction[] josmActions = new JosmAction[]{addAction, editAction, deleteAction};

    // relation actions
    private final SelectInRelationListAction setRelationSelectionAction = new SelectInRelationListAction();
    private final SelectRelationAction selectRelationAction = new SelectRelationAction(false);
    private final SelectRelationAction addRelationToSelectionAction = new SelectRelationAction(true);

    private final DownloadMembersAction downloadMembersAction = new DownloadMembersAction();
    private final DownloadSelectedIncompleteMembersAction downloadSelectedIncompleteMembersAction = new DownloadSelectedIncompleteMembersAction();

    private final SelectMembersAction selectMembersAction = new SelectMembersAction(false);
    private final SelectMembersAction addMembersToSelectionAction = new SelectMembersAction(true);

    private final HighlightHelper highlightHelper= new HighlightHelper();

    /**
     * The Add button (needed to be able to disable it)
     */
    private final SideButton btnAdd = new SideButton(addAction);
    /**
     * The Edit button (needed to be able to disable it)
     */
    private final SideButton btnEdit = new SideButton(editAction);
    /**
     * The Delete button (needed to be able to disable it)
     */
    private final SideButton btnDel = new SideButton(deleteAction);
    /**
     * Matching preset display class
     */
    private final PresetListPanel presets = new PresetListPanel();

    /**
     * Text to display when nothing selected.
     */
    private final JLabel selectSth = new JLabel("<html><p>"
            + tr("Select objects for which to change properties.") + "</p></html>");

    private PresetHandler presetHandler = new PresetHandler() {
        @Override public void updateTags(List<Tag> tags) {
            Command command = TaggingPreset.createCommand(getSelection(), tags);
            if (command != null) Main.main.undoRedo.add(command);
        }

        @Override public Collection<OsmPrimitive> getSelection() {
            if (Main.main == null) return null;
            if (Main.main.getCurrentDataSet() == null) return null;
            return Main.main.getCurrentDataSet().getSelected();
        }
    };

    // <editor-fold defaultstate="collapsed" desc="Dialog construction and helper methods">

    /**
     * Create a new PropertiesDialog
     * @param mapFrame The parent map fram
     */
    public PropertiesDialog(MapFrame mapFrame) {
        super(tr("Properties/Memberships"), "propertiesdialog", tr("Properties for selected objects."),
                Shortcut.registerShortcut("subwindow:properties", tr("Toggle: {0}", tr("Properties/Memberships")), KeyEvent.VK_P,
                        Shortcut.ALT_SHIFT), 150, true);

        setupPropertiesMenu();
        buildPropertiesTable();

        setupMembershipMenu();
        buildMembershipTable();

        // combine both tables and wrap them in a scrollPane
        JPanel bothTables = new JPanel();
        boolean top = Main.pref.getBoolean("properties.presets.top", true);
        bothTables.setLayout(new GridBagLayout());
        if(top) {
            bothTables.add(presets, GBC.std().fill(GBC.HORIZONTAL).insets(5, 2, 5, 2).anchor(GBC.NORTHWEST));
            double epsilon = Double.MIN_VALUE; // need to set a weight or else anchor value is ignored
            bothTables.add(pluginHook, GBC.eol().insets(0,1,1,1).anchor(GBC.NORTHEAST).weight(epsilon, epsilon));
        }
        bothTables.add(selectSth, GBC.eol().fill().insets(10, 10, 10, 10));
        bothTables.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(propertyTable, GBC.eol().fill(GBC.BOTH));
        bothTables.add(membershipTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(membershipTable, GBC.eol().fill(GBC.BOTH));
        if(!top) {
            bothTables.add(presets, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 2, 5, 2));
        }

        setupKeyboardShortcuts();

        // Let the action know when selection in the tables change
        propertyTable.getSelectionModel().addListSelectionListener(editAction);
        membershipTable.getSelectionModel().addListSelectionListener(editAction);
        propertyTable.getSelectionModel().addListSelectionListener(deleteAction);
        membershipTable.getSelectionModel().addListSelectionListener(deleteAction);


        JScrollPane scrollPane = (JScrollPane) createLayout(bothTables, true, Arrays.asList(new SideButton[] {
                this.btnAdd, this.btnEdit, this.btnDel
        }));

        MouseClickWatch mouseClickWatch = new MouseClickWatch();
        propertyTable.addMouseListener(mouseClickWatch);
        membershipTable.addMouseListener(mouseClickWatch);
        scrollPane.addMouseListener(mouseClickWatch);

        selectSth.setPreferredSize(scrollPane.getSize());
        presets.setSize(scrollPane.getSize());

        editHelper.loadTagsIfNeeded();
    }

    private void buildPropertiesTable() {
        // setting up the properties table

        propertyData.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
        propertyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        propertyTable.getTableHeader().setReorderingAllowed(false);

        propertyTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (value == null)
                    return this;
                if (c instanceof JLabel) {
                    String str = null;
                    if (value instanceof String) {
                        str = (String) value;
                    } else if (value instanceof Map<?, ?>) {
                        Map<?, ?> v = (Map<?, ?>) value;
                        if (v.size() != 1) {
                            str=tr("<different>");
                            c.setFont(c.getFont().deriveFont(Font.ITALIC));
                        } else {
                            final Map.Entry<?, ?> entry = v.entrySet().iterator().next();
                            str = (String) entry.getKey();
                        }
                    }
                    ((JLabel)c).putClientProperty("html.disable", Boolean.TRUE); // Fix #8730
                    ((JLabel)c).setText(str);
                }
                return c;
            }
        });
    }

    private void buildMembershipTable() {
        membershipData.setColumnIdentifiers(new String[]{tr("Member Of"),tr("Role"),tr("Position")});
        membershipTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        TableColumnModel mod = membershipTable.getColumnModel();
        membershipTable.getTableHeader().setReorderingAllowed(false);
        mod.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (value == null)
                    return this;
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    Relation r = (Relation)value;
                    label.setText(r.getDisplayName(DefaultNameFormatter.getInstance()));
                    if (r.isDisabledAndHidden()) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });

        mod.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value == null)
                    return this;
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                boolean isDisabledAndHidden = (((Relation)table.getValueAt(row, 0))).isDisabledAndHidden();
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    MemberInfo col = (MemberInfo) value;

                    String text = null;
                    for (RelationMember r : col.role) {
                        if (text == null) {
                            text = r.getRole();
                        }
                        else if (!text.equals(r.getRole())) {
                            text = tr("<different>");
                            break;
                        }
                    }

                    label.setText(text);
                    if (isDisabledAndHidden) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });

        mod.getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                boolean isDisabledAndHidden = (((Relation)table.getValueAt(row, 0))).isDisabledAndHidden();
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    label.setText(((MemberInfo) table.getValueAt(row, 1)).getPositionString());
                    if (isDisabledAndHidden) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });
        mod.getColumn(2).setPreferredWidth(20);
        mod.getColumn(1).setPreferredWidth(40);
        mod.getColumn(0).setPreferredWidth(200);
    }

    /**
     * creates the popup menu @field membershipMenu and its launcher on membership table
     */
    private void setupMembershipMenu() {
        // setting up the membership table
        membershipMenuHandler.addAction(setRelationSelectionAction);
        membershipMenuHandler.addAction(selectRelationAction);
        membershipMenuHandler.addAction(addRelationToSelectionAction);
        membershipMenuHandler.addAction(selectMembersAction);
        membershipMenuHandler.addAction(addMembersToSelectionAction);
        membershipMenu.addSeparator();
        membershipMenuHandler.addAction(downloadMembersAction);
        membershipMenuHandler.addAction(downloadSelectedIncompleteMembersAction);
        membershipMenu.addSeparator();
        membershipMenu.add(helpAction);

        membershipTable.addMouseListener(new PopupMenuLauncher(membershipMenu) {
            @Override
            protected int checkTableSelection(JTable table, Point p) {
                int row = super.checkTableSelection(table, p);
                List<Relation> rels = new ArrayList<Relation>();
                for (int i: table.getSelectedRows()) {
                    rels.add((Relation) table.getValueAt(i, 0));
                }
                membershipMenuHandler.setPrimitives(rels);
                return row;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                //update highlights
                if (Main.isDisplayingMapView()) {
                    int row = membershipTable.rowAtPoint(e.getPoint());
                    if (row>=0) {
                        if (highlightHelper.highlightOnly((Relation) membershipTable.getValueAt(row, 0))) {
                            Main.map.mapView.repaint();
                        }
                    }
                }
                super.mouseClicked(e);
            }

            @Override
            public void mouseExited(MouseEvent me) {
                highlightHelper.clear();
            }
        });
    }

    /**
     * creates the popup menu @field propertyMenu and its launcher on property table
     */
    private void setupPropertiesMenu() {
        propertyMenu.add(pasteValueAction);
        propertyMenu.add(copyValueAction);
        propertyMenu.add(copyKeyValueAction);
        propertyMenu.add(copyAllKeyValueAction);
        propertyMenu.addSeparator();
        propertyMenu.add(searchActionAny);
        propertyMenu.add(searchActionSame);
        propertyMenu.addSeparator();
        propertyMenu.add(helpAction);
        propertyTable.addMouseListener(new PopupMenuLauncher(propertyMenu));
    }

    /**
     * Assignas all needed keys like Enter and Spacebar to most important actions
     */
    private void setupKeyboardShortcuts() {

        // ENTER = editAction, open "edit" dialog
        propertyTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"onTableEnter");
        propertyTable.getActionMap().put("onTableEnter",editAction);
        membershipTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"onTableEnter");
        membershipTable.getActionMap().put("onTableEnter",editAction);

        // INSERT button = addAction, open "add property" dialog
        propertyTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),"onTableInsert");
        propertyTable.getActionMap().put("onTableInsert",addAction);

        // unassign some standard shortcuts for JTable to allow upload / download
        InputMapUtils.unassignCtrlShiftUpDown(propertyTable, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // unassign some standard shortcuts for correct copy-pasting, fix #8508
        propertyTable.setTransferHandler(null);

        propertyTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK),"onCopy");
        propertyTable.getActionMap().put("onCopy",copyKeyValueAction);

        // allow using enter to add tags for all look&feel configurations
        InputMapUtils.enableEnter(this.btnAdd);

        // DEL button = deleteAction
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),"delete"
                );
        getActionMap().put("delete", deleteAction);

        // F1 button = custom help action
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "onHelp");
        getActionMap().put("onHelp", helpAction);
    }

         /**
     * This simply fires up an {@link RelationEditor} for the relation shown; everything else
     * is the editor's business.
     *
     * @param row
     */
    private void editMembership(int row) {
        Relation relation = (Relation)membershipData.getValueAt(row, 0);
        Main.map.relationListDialog.selectRelation(relation);
        RelationEditor.getEditor(
                Main.map.mapView.getEditLayer(),
                relation,
                ((MemberInfo) membershipData.getValueAt(row, 1)).role).setVisible(true);
    }

    private int findRow(TableModel model, Object value) {
        for (int i=0; i<model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(value))
                return i;
        }
        return -1;
    }

    /**
     * Update selection status, call @{link #selectionChanged} function.
     */
    private void updateSelection() {
        if (Main.main.getCurrentDataSet() == null) {
            selectionChanged(Collections.<OsmPrimitive>emptyList());
        } else {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

   // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Event listeners methods">

    @Override
    public void showNotify() {
        DatasetEventManager.getInstance().addDatasetListener(dataChangedAdapter, FireMode.IN_EDT_CONSOLIDATED);
        SelectionEventManager.getInstance().addSelectionListener(this, FireMode.IN_EDT_CONSOLIDATED);
        MapView.addEditLayerChangeListener(this);
        for (JosmAction action : josmActions) {
            Main.registerActionShortcut(action);
        }
        updateSelection();
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(dataChangedAdapter);
        SelectionEventManager.getInstance().removeSelectionListener(this);
        MapView.removeEditLayerChangeListener(this);
        for (JosmAction action : josmActions) {
            Main.unregisterActionShortcut(action);
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (JosmAction action : josmActions) {
            action.destroy();
        }
        Container parent = pluginHook.getParent();
        if (parent != null) {
            parent.remove(pluginHook);
        }
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (!isVisible())
            return;
        if (propertyTable == null)
            return; // selection changed may be received in base class constructor before init
        if (propertyTable.getCellEditor() != null) {
            propertyTable.getCellEditor().cancelCellEditing();
        }

        String selectedTag;
        Relation selectedRelation = null;
        selectedTag = editHelper.getChangedKey(); // select last added or last edited key by default
        if (selectedTag == null && propertyTable.getSelectedRowCount() == 1) {
            selectedTag = (String)propertyData.getValueAt(propertyTable.getSelectedRow(), 0);
        }
        if (membershipTable.getSelectedRowCount() == 1) {
            selectedRelation = (Relation)membershipData.getValueAt(membershipTable.getSelectedRow(), 0);
        }

        // re-load property data
        propertyData.setRowCount(0);

        final Map<String, Integer> keyCount = new HashMap<String, Integer>();
        final Map<String, String> tags = new HashMap<String, String>();
        valueCount.clear();
        EnumSet<TaggingPresetType> types = EnumSet.noneOf(TaggingPresetType.class);
        for (OsmPrimitive osm : newSelection) {
            types.add(TaggingPresetType.forPrimitive(osm));
            for (String key : osm.keySet()) {
                String value = osm.get(key);
                keyCount.put(key, keyCount.containsKey(key) ? keyCount.get(key) + 1 : 1);
                if (valueCount.containsKey(key)) {
                    Map<String, Integer> v = valueCount.get(key);
                    v.put(value, v.containsKey(value) ? v.get(value) + 1 : 1);
                } else {
                    TreeMap<String, Integer> v = new TreeMap<String, Integer>();
                    v.put(value, 1);
                    valueCount.put(key, v);
                }
            }
        }
        for (Entry<String, Map<String, Integer>> e : valueCount.entrySet()) {
            int count = 0;
            for (Entry<String, Integer> e1 : e.getValue().entrySet()) {
                count += e1.getValue();
            }
            if (count < newSelection.size()) {
                e.getValue().put("", newSelection.size() - count);
            }
            propertyData.addRow(new Object[]{e.getKey(), e.getValue()});
            tags.put(e.getKey(), e.getValue().size() == 1
                    ? e.getValue().keySet().iterator().next() : tr("<different>"));
        }

        membershipData.setRowCount(0);

        Map<Relation, MemberInfo> roles = new HashMap<Relation, MemberInfo>();
        for (OsmPrimitive primitive: newSelection) {
            for (OsmPrimitive ref: primitive.getReferrers()) {
                if (ref instanceof Relation && !ref.isIncomplete() && !ref.isDeleted()) {
                    Relation r = (Relation) ref;
                    MemberInfo mi = roles.get(r);
                    if(mi == null) {
                        mi = new MemberInfo();
                    }
                    roles.put(r, mi);
                    int i = 1;
                    for (RelationMember m : r.getMembers()) {
                        if (m.getMember() == primitive) {
                            mi.add(m, i);
                        }
                        ++i;
                    }
                }
            }
        }

        List<Relation> sortedRelations = new ArrayList<Relation>(roles.keySet());
        Collections.sort(sortedRelations, new Comparator<Relation>() {
            @Override public int compare(Relation o1, Relation o2) {
                int comp = Boolean.valueOf(o1.isDisabledAndHidden()).compareTo(o2.isDisabledAndHidden());
                if (comp == 0) {
                    comp = o1.getDisplayName(DefaultNameFormatter.getInstance()).compareTo(o2.getDisplayName(DefaultNameFormatter.getInstance()));
                }
                return comp;
            }}
                );

        for (Relation r: sortedRelations) {
            membershipData.addRow(new Object[]{r, roles.get(r)});
        }

        presets.updatePresets(types, tags, presetHandler);

        membershipTable.getTableHeader().setVisible(membershipData.getRowCount() > 0);
        membershipTable.setVisible(membershipData.getRowCount() > 0);

        boolean hasSelection = !newSelection.isEmpty();
        boolean hasTags = hasSelection && propertyData.getRowCount() > 0;
        boolean hasMemberships = hasSelection && membershipData.getRowCount() > 0;
        btnAdd.setEnabled(hasSelection);
        btnEdit.setEnabled(hasTags || hasMemberships);
        btnDel.setEnabled(hasTags || hasMemberships);
        propertyTable.setVisible(hasTags);
        propertyTable.getTableHeader().setVisible(hasTags);
        selectSth.setVisible(!hasSelection);
        pluginHook.setVisible(hasSelection);

        int selectedIndex;
        if (selectedTag != null && (selectedIndex = findRow(propertyData, selectedTag)) != -1) {
            propertyTable.changeSelection(selectedIndex, 0, false, false);
        } else if (selectedRelation != null && (selectedIndex = findRow(membershipData, selectedRelation)) != -1) {
            membershipTable.changeSelection(selectedIndex, 0, false, false);
        } else if(hasTags) {
            propertyTable.changeSelection(0, 0, false, false);
        } else if(hasMemberships) {
            membershipTable.changeSelection(0, 0, false, false);
        }

        if(propertyData.getRowCount() != 0 || membershipData.getRowCount() != 0) {
            setTitle(tr("Properties: {0} / Memberships: {1}",
                    propertyData.getRowCount(), membershipData.getRowCount()));
        } else {
            setTitle(tr("Properties / Memberships"));
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* EditLayerChangeListener                                                            */
    /* ---------------------------------------------------------------------------------- */
    @Override
    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        if (newLayer == null) editHelper.saveTagsIfNeeded();
        // it is time to save history of tags
        GuiHelper.runInEDT(new Runnable() {
            @Override public void run() {
                updateSelection();
            }
        });
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        updateSelection();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Methods that are called by plugins to extend fuctionality ">

    /**
     * Replies the property popup menu handler.
     * @return The property popup menu handler
     */
    public PopupMenuHandler getPropertyPopupMenuHandler() {
        return propertyMenuHandler;
    }

    @SuppressWarnings("unchecked")
    public Tag getSelectedProperty() {
        int row = propertyTable.getSelectedRow();
        if (row == -1) return null;
        TreeMap<String, Integer> map = (TreeMap<String, Integer>) propertyData.getValueAt(row, 1);
        return new Tag(
                propertyData.getValueAt(row, 0).toString(),
                map.size() > 1 ? "" : map.keySet().iterator().next());
    }

    /**
     * Replies the membership popup menu handler.
     * @return The membership popup menu handler
     */
    public PopupMenuHandler getMembershipPopupMenuHandler() {
        return membershipMenuHandler;
    }

    public IRelation getSelectedMembershipRelation() {
        int row = membershipTable.getSelectedRow();
        return row > -1 ? (IRelation) membershipData.getValueAt(row, 0) : null;
    }

    // </editor-fold>

     /**
     * Class that watches for mouse clicks
     * @author imi
     */
    public class MouseClickWatch extends MouseAdapter {
        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2)
            {
                // single click, clear selection in other table not clicked in
                if (e.getSource() == propertyTable) {
                    membershipTable.clearSelection();
                } else if (e.getSource() == membershipTable) {
                    propertyTable.clearSelection();
                }
            }
            // double click, edit or add property
            else if (e.getSource() == propertyTable)
            {
                int row = propertyTable.rowAtPoint(e.getPoint());
                if (row > -1) {
                    boolean focusOnKey = (propertyTable.columnAtPoint(e.getPoint()) == 0);
                    editHelper.editProperty(row, focusOnKey);
                } else {
                    editHelper.addProperty();
                    btnAdd.requestFocusInWindow();
                }
            } else if (e.getSource() == membershipTable) {
                int row = membershipTable.rowAtPoint(e.getPoint());
                if (row > -1) {
                    editMembership(row);
                }
            }
            else
            {
                editHelper.addProperty();
                btnAdd.requestFocusInWindow();
            }
        }
        @Override public void mousePressed(MouseEvent e) {
            if (e.getSource() == propertyTable) {
                membershipTable.clearSelection();
            } else if (e.getSource() == membershipTable) {
                propertyTable.clearSelection();
            }
        }

    }

    static class MemberInfo {
        List<RelationMember> role = new ArrayList<RelationMember>();
        List<Integer> position = new ArrayList<Integer>();
        private String positionString = null;
        void add(RelationMember r, Integer p) {
            role.add(r);
            position.add(p);
        }
        String getPositionString() {
            if (positionString == null) {
                Collections.sort(position);
                positionString = String.valueOf(position.get(0));
                int cnt = 0;
                int last = position.get(0);
                for (int i = 1; i < position.size(); ++i) {
                    int cur = position.get(i);
                    if (cur == last + 1) {
                        ++cnt;
                    } else if (cnt == 0) {
                        positionString += "," + String.valueOf(cur);
                    } else {
                        positionString += "-" + String.valueOf(last);
                        positionString += "," + String.valueOf(cur);
                        cnt = 0;
                    }
                    last = cur;
                }
                if (cnt >= 1) {
                    positionString += "-" + String.valueOf(last);
                }
            }
            if (positionString.length() > 20) {
                positionString = positionString.substring(0, 17) + "...";
            }
            return positionString;
        }
    }

    /**
     * Class that allows fast creation of read-only table model with String columns
     */
    public static class ReadOnlyTableModel extends DefaultTableModel {
        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }

    /**
     * Action handling delete button press in properties dialog.
     */
    class DeleteAction extends JosmAction implements ListSelectionListener {

        public DeleteAction() {
            super(tr("Delete"), "dialogs/delete", tr("Delete the selected key in all objects"),
                    Shortcut.registerShortcut("properties:delete", tr("Delete Properties"), KeyEvent.VK_D,
                            Shortcut.ALT_CTRL_SHIFT), false);
            updateEnabledState();
        }

        protected void deleteProperties(int[] rows){
            // convert list of rows to HashMap (and find gap for nextKey)
            HashMap<String, String> tags = new HashMap<String, String>(rows.length);
            int nextKeyIndex = rows[0];
            for (int row : rows) {
                String key = propertyData.getValueAt(row, 0).toString();
                if (row == nextKeyIndex + 1) {
                    nextKeyIndex = row; // no gap yet
                }
                tags.put(key, null);
            }

            // find key to select after deleting other properties
            String nextKey = null;
            int rowCount = propertyData.getRowCount();
            if (rowCount > rows.length) {
                if (nextKeyIndex == rows[rows.length-1]) {
                    // no gap found, pick next or previous key in list
                    nextKeyIndex = (nextKeyIndex + 1 < rowCount ? nextKeyIndex + 1 : rows[0] - 1);
                } else {
                    // gap found
                    nextKeyIndex++;
                }
                nextKey = (String)propertyData.getValueAt(nextKeyIndex, 0);
            }

            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, tags));

            membershipTable.clearSelection();
            if (nextKey != null) {
                propertyTable.changeSelection(findRow(propertyData, nextKey), 0, false, false);
            }
        }

        protected void deleteFromRelation(int row) {
            Relation cur = (Relation)membershipData.getValueAt(row, 0);

            Relation nextRelation = null;
            int rowCount = membershipTable.getRowCount();
            if (rowCount > 1) {
                nextRelation = (Relation)membershipData.getValueAt((row + 1 < rowCount ? row + 1 : row - 1), 0);
            }

            ExtendedDialog ed = new ExtendedDialog(Main.parent,
                    tr("Change relation"),
                    new String[] {tr("Delete from relation"), tr("Cancel")});
            ed.setButtonIcons(new String[] {"dialogs/delete.png", "cancel.png"});
            ed.setContent(tr("Really delete selection from relation {0}?", cur.getDisplayName(DefaultNameFormatter.getInstance())));
            ed.toggleEnable("delete_from_relation");
            ed.showDialog();

            if(ed.getValue() != 1)
                return;

            Relation rel = new Relation(cur);
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            for (OsmPrimitive primitive: sel) {
                rel.removeMembersFor(primitive);
            }
            Main.main.undoRedo.add(new ChangeCommand(cur, rel));

            propertyTable.clearSelection();
            if (nextRelation != null) {
                membershipTable.changeSelection(findRow(membershipData, nextRelation), 0, false, false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (propertyTable.getSelectedRowCount() > 0) {
                int[] rows = propertyTable.getSelectedRows();
                deleteProperties(rows);
            } else if (membershipTable.getSelectedRowCount() > 0) {
                int[] rows = membershipTable.getSelectedRows();
                // delete from last relation to convserve row numbers in the table
                for (int i=rows.length-1; i>=0; i--) {
                    deleteFromRelation(rows[i]);
                }
            }
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(
                    (propertyTable != null && propertyTable.getSelectedRowCount() >= 1)
                    || (membershipTable != null && membershipTable.getSelectedRowCount() > 0)
                    );
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Action handling add button press in properties dialog.
     */
    class AddAction extends JosmAction {
        public AddAction() {
            super(tr("Add"), "dialogs/add", tr("Add a new key/value pair to all objects"),
                    Shortcut.registerShortcut("properties:add", tr("Add Property"), KeyEvent.VK_A,
                            Shortcut.ALT), false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            editHelper.addProperty();
            btnAdd.requestFocusInWindow();
        }
    }

    /**
     * Action handling edit button press in properties dialog.
     */
    class EditAction extends JosmAction implements ListSelectionListener {
        public EditAction() {
            super(tr("Edit"), "dialogs/edit", tr("Edit the value of the selected key for all objects"),
                    Shortcut.registerShortcut("properties:edit", tr("Edit Properties"), KeyEvent.VK_S,
                            Shortcut.ALT), false);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            if (propertyTable.getSelectedRowCount() == 1) {
                int row = propertyTable.getSelectedRow();
                editHelper.editProperty(row, false);
            } else if (membershipTable.getSelectedRowCount() == 1) {
                int row = membershipTable.getSelectedRow();
                editMembership(row);
            }
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(
                    (propertyTable != null && propertyTable.getSelectedRowCount() == 1)
                    ^ (membershipTable != null && membershipTable.getSelectedRowCount() == 1)
                    );
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class HelpAction extends AbstractAction {
        public HelpAction() {
            putValue(NAME, tr("Go to OSM wiki for tag help (F1)"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with wiki help for selected object"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "search"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String base = Main.pref.get("url.openstreetmap-wiki", "http://wiki.openstreetmap.org/wiki/");
                String lang = LanguageInfo.getWikiLanguagePrefix();
                final List<URI> uris = new ArrayList<URI>();
                int row;
                if (propertyTable.getSelectedRowCount() == 1) {
                    row = propertyTable.getSelectedRow();
                    String key = URLEncoder.encode(propertyData.getValueAt(row, 0).toString(), "UTF-8");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> m = (Map<String, Integer>) propertyData.getValueAt(row, 1);
                    String val = URLEncoder.encode(m.entrySet().iterator().next().getKey(), "UTF-8");

                    uris.add(new URI(String.format("%s%sTag:%s=%s", base, lang, key, val)));
                    uris.add(new URI(String.format("%sTag:%s=%s", base, key, val)));
                    uris.add(new URI(String.format("%s%sKey:%s", base, lang, key)));
                    uris.add(new URI(String.format("%sKey:%s", base, key)));
                    uris.add(new URI(String.format("%s%sMap_Features", base, lang)));
                    uris.add(new URI(String.format("%sMap_Features", base)));
                } else if (membershipTable.getSelectedRowCount() == 1) {
                    row = membershipTable.getSelectedRow();
                    String type = URLEncoder.encode(
                            ((Relation)membershipData.getValueAt(row, 0)).get("type"), "UTF-8"
                            );

                    if (type != null && !type.isEmpty()) {
                        uris.add(new URI(String.format("%s%sRelation:%s", base, lang, type)));
                        uris.add(new URI(String.format("%sRelation:%s", base, type)));
                    }

                    uris.add(new URI(String.format("%s%sRelations", base, lang)));
                    uris.add(new URI(String.format("%sRelations", base)));
                } else {
                    // give the generic help page, if more than one element is selected
                    uris.add(new URI(String.format("%s%sMap_Features", base, lang)));
                    uris.add(new URI(String.format("%sMap_Features", base)));
                }

                Main.worker.execute(new Runnable(){
                    @Override public void run() {
                        try {
                            // find a page that actually exists in the wiki
                            HttpURLConnection conn;
                            for (URI u : uris) {
                                conn = Utils.openHttpConnection(u.toURL());
                                conn.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);

                                if (conn.getResponseCode() != 200) {
                                    Main.info("{0} does not exist", u);
                                    conn.disconnect();
                                } else {
                                    int osize = conn.getContentLength();
                                    if (osize > -1) {
                                        conn.disconnect();
    
                                        conn = Utils.openHttpConnection(new URI(u.toString()
                                                .replace("=", "%3D") /* do not URLencode whole string! */
                                                .replaceFirst("/wiki/", "/w/index.php?redirect=no&title=")
                                                ).toURL());
                                        conn.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);
                                    }

                                    /* redirect pages have different content length, but retrieving a "nonredirect"
                                     *  page using index.php and the direct-link method gives slightly different
                                     *  content lengths, so we have to be fuzzy.. (this is UGLY, recode if u know better)
                                     */
                                    if (conn.getContentLength() != -1 && osize > -1 && Math.abs(conn.getContentLength() - osize) > 200) {
                                        Main.info("{0} is a mediawiki redirect", u);
                                        conn.disconnect();
                                    } else {
                                        Main.info("browsing to {0}", u);
                                        conn.disconnect();

                                        OpenBrowser.displayUrl(u.toString());
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    class PasteValueAction extends AbstractAction {
        public PasteValueAction() {
            putValue(NAME, tr("Paste Value"));
            putValue(SHORT_DESCRIPTION, tr("Paste the value of the selected tag from clipboard"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (propertyTable.getSelectedRowCount() != 1)
                return;
            String key = propertyData.getValueAt(propertyTable.getSelectedRow(), 0).toString();
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            String clipboard = Utils.getClipboardContent();
            if (sel.isEmpty() || clipboard == null)
                return;
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, Utils.strip(clipboard)));
        }
    }

    abstract class AbstractCopyAction extends AbstractAction {

        protected abstract Collection<String> getString(OsmPrimitive p, String key);

        @Override
        public void actionPerformed(ActionEvent ae) {
            int[] rows = propertyTable.getSelectedRows();
            Set<String> values = new TreeSet<String>();
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            if (rows.length == 0 || sel.isEmpty()) return;

            for (int row: rows) {
                String key = propertyData.getValueAt(row, 0).toString();
                if (sel.isEmpty())
                    return;
                for (OsmPrimitive p : sel) {
                    Collection<String> s = getString(p,key);
                    if (s != null) {
                        values.addAll(s);
                    }
                }
            }
            if (!values.isEmpty()) {
                Utils.copyToClipboard(Utils.join("\n", values));
            }
        }
    }

    class CopyValueAction extends AbstractCopyAction {

        public CopyValueAction() {
            putValue(NAME, tr("Copy Value"));
            putValue(SHORT_DESCRIPTION, tr("Copy the value of the selected tag to clipboard"));
        }

        @Override
        protected Collection<String> getString(OsmPrimitive p, String key) {
            String v = p.get(key);
            return v == null ? null : Collections.singleton(v);
        }
    }

    class CopyKeyValueAction extends AbstractCopyAction {

        public CopyKeyValueAction() {
            putValue(NAME, tr("Copy Key/Value"));
            putValue(SHORT_DESCRIPTION, tr("Copy the key and value of the selected tag to clipboard"));
        }

        @Override
        protected Collection<String> getString(OsmPrimitive p, String key) {
            String v = p.get(key);
            return v == null ? null : Collections.singleton(new Tag(key, v).toString());
        }
    }

    class CopyAllKeyValueAction extends AbstractCopyAction {

        public CopyAllKeyValueAction() {
            putValue(NAME, tr("Copy all Keys/Values"));
            putValue(SHORT_DESCRIPTION, tr("Copy the key and value of the all tags to clipboard"));
        }

        @Override
        protected Collection<String> getString(OsmPrimitive p, String key) {
            List<String> r = new LinkedList<String>();
            for (Entry<String, String> kv : p.getKeys().entrySet()) {
                r.add(new Tag(kv.getKey(), kv.getValue()).toString());
            }
            return r;
        }
    }

    class SearchAction extends AbstractAction {
        final boolean sameType;

        public SearchAction(boolean sameType) {
            this.sameType = sameType;
            if (sameType) {
                putValue(NAME, tr("Search Key/Value/Type"));
                putValue(SHORT_DESCRIPTION, tr("Search with the key and value of the selected tag, restrict to type (i.e., node/way/relation)"));
            } else {
                putValue(NAME, tr("Search Key/Value"));
                putValue(SHORT_DESCRIPTION, tr("Search with the key and value of the selected tag"));
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (propertyTable.getSelectedRowCount() != 1)
                return;
            String key = propertyData.getValueAt(propertyTable.getSelectedRow(), 0).toString();
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            if (sel.isEmpty())
                return;
            String sep = "";
            String s = "";
            for (OsmPrimitive p : sel) {
                String val = p.get(key);
                if (val == null) {
                    continue;
                }
                String t = "";
                if (!sameType) {
                    t = "";
                } else if (p instanceof Node) {
                    t = "type:node ";
                } else if (p instanceof Way) {
                    t = "type:way ";
                } else if (p instanceof Relation) {
                    t = "type:relation ";
                }
                s += sep + "(" + t + "\"" +
                        org.openstreetmap.josm.actions.search.SearchAction.escapeStringForSearch(key) + "\"=\"" +
                        org.openstreetmap.josm.actions.search.SearchAction.escapeStringForSearch(val) + "\")";
                sep = " OR ";
            }

            SearchSetting ss = new SearchSetting(s, SearchMode.replace, true, false, false);
            org.openstreetmap.josm.actions.search.SearchAction.searchWithoutHistory(ss);
        }
    }
}
