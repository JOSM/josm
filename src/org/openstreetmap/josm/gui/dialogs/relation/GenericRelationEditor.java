package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This dialog is for editing relations.
 *
 * In the basic form, it provides two tables, one with the relation tags
 * and one with the relation members. (Relation tags can be edited through
 * the normal properties dialog as well, if you manage to get a relation
 * selected!)
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class GenericRelationEditor extends RelationEditor {

    private JLabel status;

    /**
     * The property data.
     */
    private final DefaultTableModel propertyData = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) {
            return true;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };

    /**
     * The membership data.
     */
    private final DefaultTableModel memberData = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) {
            return column == 0;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 1 ? OsmPrimitive.class : String.class;
        }
    };

    /**
     * The properties and membership lists.
     */
    private final JTable propertyTable = new JTable(propertyData);
    private final JTable memberTable = new JTable(memberData);

    // We need this twice, so cache result
    protected final static String applyChangesText = tr("Apply Changes");

    /**
     * Creates a new relation editor for the given relation. The relation
     * will be saved if the user selects "ok" in the editor.
     *
     * If no relation is given, will create an editor for a new relation.
     *
     * @param relation relation to edit, or null to create a new one.
     */
    public GenericRelationEditor(Relation relation, Collection<RelationMember> selectedMembers )
    {
        // Initalizes ExtendedDialog
        super(relation, selectedMembers);

        JPanel bothTables = setupBasicLayout(selectedMembers);

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.add(bothTables, tr("Basic"));

        // This sets the minimum size before scrollbars appear on the dialog
        tabPane.setPreferredSize(new Dimension(100, 100));
        contentConstraints = GBC.eol().fill().insets(5,10,5,0);
        setupDialog(tabPane, new String[] { "ok.png", "cancel.png" });
        // FIXME: Make it remember last screen position
        setSize(findMaxDialogSize());

        try { setAlwaysOnTop(true); } catch (SecurityException sx) {}
        setVisible(true);
    }


    /**
     * Basic Editor panel has two blocks: a tag table at the top and a membership list below
     * @param selectedMembers
     * @return a JPanel with the described layout
     */
    private JPanel setupBasicLayout(Collection<RelationMember> selectedMembers) {
        // setting up the properties table
        propertyData.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
        propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propertyData.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent tme) {
                if (tme.getType() == TableModelEvent.UPDATE) {
                    int row = tme.getFirstRow();

                    if (!(tme.getColumn() == 0 && row == propertyData.getRowCount() -1)) {
                        clone.entrySet().clear();
                        for (int i = 0; i < propertyData.getRowCount(); i++) {
                            String key = propertyData.getValueAt(i, 0).toString();
                            String value = propertyData.getValueAt(i, 1).toString();
                            if (key.length() > 0 && value.length() > 0) {
                                clone.put(key, value);
                            }
                        }
                        refreshTables();
                    }
                }
            }
        });
        propertyTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // setting up the member table

        memberData.setColumnIdentifiers(new String[]{tr("Role"),tr("Occupied By"), tr("linked")});
        memberTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        memberTable.getColumnModel().getColumn(1).setCellRenderer(new OsmPrimitivRenderer());
        memberData.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent tme) {
                if (tme.getType() == TableModelEvent.UPDATE && tme.getColumn() == 0) {
                    int row = tme.getFirstRow();
                    clone.members.get(row).role = memberData.getValueAt(row, 0).toString();
                }
            }
        });
        ListSelectionModel lsm = memberTable.getSelectionModel();
        lsm.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                ArrayList<OsmPrimitive> sel;
                int cnt = memberTable.getSelectedRowCount();
                if(cnt > 0)
                {
                    sel = new ArrayList<OsmPrimitive>(cnt);
                    for (int i : memberTable.getSelectedRows()) {
                        sel.add((OsmPrimitive)memberTable.getValueAt(i, 1));
                    }
                }
                else
                {
                    cnt = memberTable.getRowCount();
                    sel = new ArrayList<OsmPrimitive>(cnt);
                    for (int i = 0; i < cnt; ++i) {
                        sel.add((OsmPrimitive)memberTable.getValueAt(i, 1));
                    }
                }
                Main.ds.setSelected(sel);
            }
        });
        memberTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // combine both tables and wrap them in a scrollPane
        JPanel bothTables = new JPanel();
        bothTables.setLayout(new GridBagLayout());
        bothTables.add(new JLabel(tr("Tags (empty value deletes tag)")), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(new JScrollPane(propertyTable), GBC.eop().fill(GBC.BOTH));
        bothTables.add(status = new JLabel(tr("Members")), GBC.eol().fill(GBC.HORIZONTAL));
        // this is not exactly pretty but the four buttons simply don't fit in one line.
        // we should have smaller buttons for situations like this.
        JPanel buttonPanel = setupBasicButtons();

        bothTables.add(new JScrollPane(memberTable), GBC.eol().fill(GBC.BOTH));
        bothTables.add(buttonPanel, GBC.eop().fill(GBC.HORIZONTAL));
        refreshTables();

        if (selectedMembers != null) {
            boolean scrolled = false;
            for (int i = 0; i < memberData.getRowCount(); i++) {
                for (RelationMember m : selectedMembers) {
                    if (m.member == memberData.getValueAt(i, 1)
                            && m.role.equals(memberData.getValueAt(i, 0))) {
                        memberTable.addRowSelectionInterval(i, i);
                        if (!scrolled) {
                            // Ensure that the first member is visible
                            memberTable.scrollRectToVisible(memberTable.getCellRect(i, 0, true));
                            scrolled = true;
                        }
                        break;
                    }
                }

            }
        }
        return bothTables;
    }

    /**
     * Creates the buttons for the basic editing layout
     * @return JPanel with basic buttons
     */
    private JPanel setupBasicButtons() {
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3));

        buttonPanel.add(createButton(marktr("Move Up"), "moveup", tr("Move the currently selected members up"), KeyEvent.VK_N, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveMembers(-1);
            }
        }));

        buttonPanel.add(createButton(marktr("Add Selected"),"addselected",
                tr("Add all currently selected objects as members"), KeyEvent.VK_D, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addSelected();
            }
        }));

        buttonPanel.add(createButton(marktr("Remove Selected"),"removeselected",
                tr("Remove all currently selected objects from relation"), KeyEvent.VK_S, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        }));

        buttonPanel.add(createButton(marktr("Move Down"), "movedown", tr("Move the currently selected members down"), KeyEvent.VK_J, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveMembers(1);
            }
        }));

        buttonPanel.add(createButton(marktr("Remove"),"remove",
                tr("Remove the member in the current table row from this relation"), KeyEvent.VK_M, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] rows = memberTable.getSelectedRows();
                RelationMember mem = new RelationMember();
                for (int row : rows) {
                    mem.role = memberTable.getValueAt(row, 0).toString();
                    mem.member = (OsmPrimitive) memberTable.getValueAt(row, 1);
                    clone.members.remove(mem);
                }
                refreshTables();
            }
        }));

        buttonPanel.add(createButton(marktr("Download Members"),"downloadincomplete",
                tr("Download all incomplete ways and nodes in relation"), KeyEvent.VK_K, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadRelationMembers();
                refreshTables();
            }
        }));

        return buttonPanel;
    }

    /**
     * This function saves the user's changes. Must be invoked manually.
     */
    private void applyChanges() {
        if (GenericRelationEditor.this.relation == null) {
            // If the user wanted to create a new relation, but hasn't added any members or
            // tags, don't add an empty relation
            if(clone.members.size() == 0 && !clone.isTagged())
                return;
            Main.main.undoRedo.add(new AddCommand(clone));
            DataSet.fireSelectionChanged(Main.ds.getSelected());
        } else if (!GenericRelationEditor.this.relation.realEqual(clone, true)) {
            Main.main.undoRedo.add(new ChangeCommand(GenericRelationEditor.this.relation, clone));
            DataSet.fireSelectionChanged(Main.ds.getSelected());
        }
    }

    @Override
    protected void buttonAction(ActionEvent evt) {
        String a = evt.getActionCommand();
        if(applyChangesText.equals(a)) {
            applyChanges();
        }

        setVisible(false);
    }

    @Override
    protected Dimension findMaxDialogSize() {
        // FIXME: Make it remember dialog size
        return new Dimension(600, 500);
    }

    private void refreshTables() {
        // re-load property data

        propertyData.setRowCount(0);
        for (Entry<String, String> e : clone.entrySet()) {
            propertyData.addRow(new Object[]{e.getKey(), e.getValue()});
        }
        propertyData.addRow(new Object[]{"", ""});

        // re-load membership data

        memberData.setRowCount(0);
        for (int i=0; i<clone.members.size(); i++) {

            // this whole section is aimed at finding out whether the
            // relation member is "linked" with the next, i.e. whether
            // (if both are ways) these ways are connected. It should
            // really produce a much more beautiful output (with a linkage
            // symbol somehow places betweeen the two member lines!), and
            // it should cache results, so... FIXME ;-)

            RelationMember em = clone.members.get(i);
            boolean linked = false;
            RelationMember m = em;
            RelationMember way1 = null;
            RelationMember way2 = null;
            int depth = 0;

            while (m != null && depth < 10) {
                if (m.member instanceof Way) {
                    way1 = m;
                    break;
                } else if (m.member instanceof Relation) {
                    if (m.member == this.relation) {
                        break;
                    }
                    m = ((Relation)m.member).lastMember();
                    depth++;
                } else {
                    break;
                }
            }
            if (way1 != null) {
                int next = i+1;
                while (next < clone.members.size()) {
                    m = clone.members.get(next++);
                    depth = 0;
                    while (m != null && depth < 10) {
                        if (m.member instanceof Way) {
                            way2 = m;
                            break;
                        } else if (m.member instanceof Relation) {
                            if (m.member == this.relation) {
                                break;
                            }
                            m = ((Relation)(m.member)).firstMember();
                            depth++;
                        } else {
                            break;
                        }
                    }
                    if (way2 != null) {
                        break;
                    }
                }
            }
            if (way2 != null) {
                Node way1first = ((Way)(way1.member)).firstNode();
                Node way1last = ((Way)(way1.member)).lastNode();
                Node way2first = ((Way)(way2.member)).firstNode();
                Node way2last = ((Way)(way2.member)).lastNode();
                if (way1.role.equals("forward")) {
                    way1first = null;
                } else if (way1.role.equals("backward")) {
                    way1last = null;
                }
                if (way2.role.equals("forward")) {
                    way2last = null;
                } else if (way2.role.equals("backward")) {
                    way2first = null;
                }

                if (way1first != null && way2first != null && way1first.equals(way2first)) {
                    linked = true;
                } else if (way1first != null && way2last != null && way1first.equals(way2last)) {
                    linked = true;
                } else if (way1last != null && way2first != null && way1last.equals(way2first)) {
                    linked = true;
                } else if (way1last != null && way2last != null && way1last.equals(way2last)) {
                    linked = true;
                }

                // end of section to determine linkedness.

                memberData.addRow(new Object[]{em.role, em.member, linked ? tr("yes") : tr("no")});
            } else {
                memberData.addRow(new Object[]{em.role, em.member, ""});
            }
        }
        status.setText(tr("Members: {0}", clone.members.size()));
    }

    private SideButton createButton(String name, String iconName, String tooltip, int mnemonic, ActionListener actionListener) {
        return
        new SideButton(name, iconName, "relationEditor",
                tooltip,
                Shortcut.registerShortcut("relationeditor:"+iconName,
                        tr("Relation Editor: {0}", name == null ? tooltip : name),
                        mnemonic,
                        Shortcut.GROUP_MNEMONIC),
                        actionListener
        );
    }

    private void addSelected() {
        for (OsmPrimitive p : Main.ds.getSelected()) {
            // ordered relations may have the same member multiple times.
            // TODO: visual indication of the fact that one is there more than once?
            RelationMember em = new RelationMember();
            em.member = p;
            em.role = "";
            // when working with ordered relations, we make an effort to
            // add the element before the first selected member.
            int[] rows = memberTable.getSelectedRows();
            if (rows.length > 0) {
                clone.members.add(rows[0], em);
            } else {
                clone.members.add(em);
            }
        }
        refreshTables();
    }

    private void deleteSelected() {
        for (OsmPrimitive p : Main.ds.getSelected()) {
            Relation c = new Relation(clone);
            for (RelationMember rm : c.members) {
                if (rm.member == p)
                {
                    RelationMember mem = new RelationMember();
                    mem.role = rm.role;
                    mem.member = rm.member;
                    clone.members.remove(mem);
                }
            }
        }
        refreshTables();
    }

    private void moveMembers(int direction) {
        int[] rows = memberTable.getSelectedRows();
        if (rows.length == 0) return;

        // check if user attempted to move anything beyond the boundary of the list
        if (rows[0] + direction < 0) return;
        if (rows[rows.length-1] + direction >= clone.members.size()) return;

        RelationMember m[] = new RelationMember[clone.members.size()];

        // first move all selected rows from the member list into a new array,
        // displaced by the move amount
        for (Integer i: rows) {
            m[i+direction] = clone.members.get(i);
            clone.members.set(i, null);
        }

        // now fill the empty spots in the destination array with the remaining
        // elements.
        int i = 0;
        for (RelationMember rm : clone.members) {
            if (rm != null) {
                while (m[i] != null) {
                    i++;
                }
                m[i++] = rm;
            }
        }

        // and write the array back into the member list.
        clone.members.clear();
        clone.members.addAll(Arrays.asList(m));
        refreshTables();
        ListSelectionModel lsm = memberTable.getSelectionModel();
        lsm.setValueIsAdjusting(true);
        for (Integer j: rows) {
            lsm.addSelectionInterval(j + direction, j + direction);
        }
        lsm.setValueIsAdjusting(false);
    }

    private void downloadRelationMembers() {
        boolean download = false;
        for (RelationMember member : clone.members) {
            if (member.member.incomplete) {
                download = true;
                break;
            }
        }
        if (download) {
            OsmServerObjectReader reader = new OsmServerObjectReader(clone.id, OsmPrimitiveType.RELATION, true);
            try {
                DataSet dataSet = reader.parseOsm();
                if (dataSet != null) {
                    final MergeVisitor visitor = new MergeVisitor(Main.main
                            .editLayer().data, dataSet);
                    visitor.merge();

                    // copy the merged layer's data source info
                    for (DataSource src : dataSet.dataSources) {
                        Main.main.editLayer().data.dataSources.add(src);
                    }
                    Main.main.editLayer().fireDataChange();

                    if (visitor.getConflicts().isEmpty())
                        return;
                    final ConflictDialog dlg = Main.map.conflictDialog;
                    dlg.add(visitor.getConflicts());
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("There were conflicts during import."));
                    if (!dlg.isVisible()) {
                        dlg.action
                        .actionPerformed(new ActionEvent(this, 0, ""));
                    }
                }
            } catch(OsmTransferException e) {
                e.printStackTrace();
                if (e.getCause() != null) {
                    if (e.getCause() instanceof SAXException) {
                        JOptionPane.showMessageDialog(this,tr("Error parsing server response.")+": "+e.getCause().getMessage(),
                                tr("Error"), JOptionPane.ERROR_MESSAGE);
                    } else if(e.getCause() instanceof IOException) {
                        JOptionPane.showMessageDialog(this,tr("Cannot connect to server.")+": "+e.getCause().getMessage(),
                                tr("Error"), JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,tr("Error when communicating with server.")+": "+e.getMessage(),
                            tr("Error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
