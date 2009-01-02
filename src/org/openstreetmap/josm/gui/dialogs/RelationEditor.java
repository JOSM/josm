package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.io.OsmServerObjectReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
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
public class RelationEditor extends JFrame {

    /**
     * The relation that this editor is working on, and the clone made for
     * editing.
     */
    private final Relation relation;
    private final Relation clone;
    private JLabel status;

    /**
     * True if the relation is ordered (API 0.6). False for API 0.5.
     */
    boolean ordered;

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

    // =================== FIXME FIXME FIXME =====================
    // As soon as API 0.5 is dead, drop all the collation stuff from here ...

    /**
     * Collator for sorting the roles and entries of the member table.
     */
    private static final Collator collator;
    static {
        collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
    }

    /**
     * Compare role strings.
     */
    private static int compareRole(String s1, String s2) {
        int last1 = s1.lastIndexOf('_');
        if (last1 > 0) {
            int last2 = s2.lastIndexOf('_');
            if (last2 == last1) {
                String prefix1 = s1.substring(0, last1);
                String prefix2 = s2.substring(0, last2);

                if (prefix1.equalsIgnoreCase(prefix2)) {
                    // Both roles have the same prefix, now determine the
                    // suffix.
                    String suffix1 = s1.substring(last1 + 1, s1.length());
                    String suffix2 = s2.substring(last2 + 1, s2.length());

                    if (suffix1.matches("\\d+") && suffix2.matches("\\d+")) {
                        // Suffix is an number -> compare it.
                        int i1 = Integer.parseInt(suffix1);
                        int i2 = Integer.parseInt(suffix2);

                        return i1 - i2;
                    }
                }
            }
        }
        if(s1.length() == 0 && s2.length() != 0)
            return 1;
        else if(s2.length() == 0 && s1.length() != 0)
            return -1;

        // Default handling if the role name is nothing like "stop_xx"
        return collator.compare(s1, s2);
    }


    /**
     * Compare two OsmPrimitives.
     */
    private static int compareMembers(OsmPrimitive o1, OsmPrimitive o2) {
        return collator.compare(o1.getName(), o2.getName());
    }

    private final Comparator<RelationMember> memberComparator = new Comparator<RelationMember>() {
        public int compare(RelationMember r1, RelationMember r2) {
            int roleResult = compareRole(r1.role, r2.role);

            if (roleResult == 0)
                roleResult = compareMembers(r1.member, r2.member);

            return roleResult;
        }
    };

    // =================== FIXME FIXME FIXME =====================
    // ... until here, and also get rid of the "Collections.sort..." below.

    /**
     * Creates a new relation editor for the given relation. The relation
     * will be saved if the user selects "ok" in the editor.
     *
     * If no relation is given, will create an editor for a new relation.
     *
     * @param relation relation to edit, or null to create a new one.
     */
    public RelationEditor(Relation relation)
    {
        this(relation, null);
    }

    /**
     * Creates a new relation editor for the given relation. The relation
     * will be saved if the user selects "ok" in the editor.
     *
     * If no relation is given, will create an editor for a new relation.
     *
     * @param relation relation to edit, or null to create a new one.
     */
    public RelationEditor(Relation relation, Collection<RelationMember> selectedMembers )
    {
        super(relation == null ? tr("Create new relation") :
            relation.id == 0 ? tr ("Edit new relation") :
            tr("Edit relation #{0}", relation.id));
        this.relation = relation;

        ordered = Main.pref.get("osm-server.version", "0.5").equals("0.6");

        if (relation == null) {
            // create a new relation
            this.clone = new Relation();
        } else {
            // edit an existing relation
            this.clone = new Relation(relation);
            if (!ordered) Collections.sort(this.clone.members, memberComparator);
        }

        getContentPane().setLayout(new BorderLayout());
        JTabbedPane tabPane = new JTabbedPane();
        getContentPane().add(tabPane, BorderLayout.CENTER);

        // (ab)use JOptionPane to make this look familiar;
        // hook up with JOptionPane's property change event
        // to detect button click
        final JOptionPane okcancel = new JOptionPane("",
            JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
        getContentPane().add(okcancel, BorderLayout.SOUTH);

        okcancel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY) && event.getNewValue() != null) {
                    if ((Integer)event.getNewValue() == JOptionPane.OK_OPTION) {
                        // clicked ok!
                        if (RelationEditor.this.relation == null) {
                            Main.main.undoRedo.add(new AddCommand(clone));
                            DataSet.fireSelectionChanged(Main.ds.getSelected());
                        } else if (!RelationEditor.this.relation.realEqual(clone, true)) {
                            Main.main.undoRedo.add(new ChangeCommand(RelationEditor.this.relation, clone));
                            DataSet.fireSelectionChanged(Main.ds.getSelected());
                        }
                    }
                    setVisible(false);
                }
            }
        });

        JLabel help = new JLabel("<html><em>"+
            tr("This is the basic relation editor which allows you to change the relation's tags " +
            "as well as the members. In addition to this we should have a smart editor that " +
            "detects the type of relationship and limits your choices in a sensible way.")+"</em></html>");
        getContentPane().add(help, BorderLayout.NORTH);
        try { setAlwaysOnTop(true); } catch (SecurityException sx) {}

        // Basic Editor panel has two blocks;
        // a tag table at the top and a membership list below.

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
                            if (key.length() > 0 && value.length() > 0) clone.put(key, value);
                        }
                        refreshTables();
                    }
                }
            }
        });
        propertyTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // setting up the member table

        memberData.setColumnIdentifiers(new String[]{tr("Role"),tr("Occupied By")});
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
        memberTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // combine both tables and wrap them in a scrollPane
        JPanel bothTables = new JPanel();
        bothTables.setLayout(new GridBagLayout());
        bothTables.add(new JLabel(tr("Tags (empty value deletes tag)")), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(new JScrollPane(propertyTable), GBC.eop().fill(GBC.BOTH));
        bothTables.add(status = new JLabel(tr("Members")), GBC.eol().fill(GBC.HORIZONTAL));
        if (ordered) {
            JPanel upDownPanel = new JPanel();
            upDownPanel.setLayout(new BoxLayout(upDownPanel, BoxLayout.Y_AXIS));

            upDownPanel.add(createButton(null, "moveup", tr("Move the currently selected members up"),
                    KeyEvent.VK_U, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveMembers(-1);
                }
            }));
            upDownPanel.add(createButton(null, "movedown", tr("Move the currently selected members down"),
                    KeyEvent.VK_N, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    moveMembers(1);
                }
            }));


            bothTables.add(new JScrollPane(memberTable), GBC.std().fill(GBC.BOTH));
            bothTables.add(upDownPanel, GBC.eol().fill(GBC.VERTICAL));
        } else {
            bothTables.add(new JScrollPane(memberTable), GBC.eol().fill(GBC.BOTH));
        }

        JPanel buttonPanel = new JPanel(new GridLayout(1,3));

        buttonPanel.add(createButton(marktr("Add Selected"),"addselected",
        tr("Add all currently selected objects as members"), KeyEvent.VK_A, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addSelected();
            }
        }));

        buttonPanel.add(createButton(marktr("Delete Selected"),"deleteselected",
        tr("Delete all currently selected objects from relation"), KeyEvent.VK_R, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        }));

        buttonPanel.add(createButton(marktr("Delete"),"delete",
        tr("Remove the member in the current table row from this relation"), KeyEvent.VK_D, new ActionListener() {
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

        buttonPanel.add(createButton(marktr("Select"),"select",
        tr("Highlight the member from the current table row as JOSM's selection"), KeyEvent.VK_S, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<OsmPrimitive> sel;
                int cnt = memberTable.getSelectedRowCount();
                if(cnt > 0)
                {
                    sel = new ArrayList<OsmPrimitive>(cnt);
                    for (int i : memberTable.getSelectedRows())
                        sel.add((OsmPrimitive)memberTable.getValueAt(i, 1));
                }
                else
                {
                    cnt = memberTable.getRowCount();
                    sel = new ArrayList<OsmPrimitive>(cnt);
                    for (int i = 0; i < cnt; ++i)
                        sel.add((OsmPrimitive)memberTable.getValueAt(i, 1));
                }
                Main.ds.setSelected(sel);
            }
        }));
        buttonPanel.add(createButton(marktr("Download Members"),"down",
        tr("Download all incomplete ways and nodes in relation"), KeyEvent.VK_L, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadRelationMembers();
                refreshTables();
            }
        }));

        bothTables.add(buttonPanel, GBC.eop().fill(GBC.HORIZONTAL));

        tabPane.add(bothTables, "Basic");

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

        setSize(new Dimension(600, 500));
        setLocationRelativeTo(Main.parent);
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
        for (RelationMember em : clone.members) {
            memberData.addRow(new Object[]{em.role, em.member});
        }
        status.setText(tr("Members: {0}", clone.members.size()));
    }

    private JButton createButton(String name, String iconName, String tooltip, int mnemonic, ActionListener actionListener) {
        JButton b = new JButton(tr(name), ImageProvider.get("dialogs", iconName));
        b.setActionCommand(name);
        b.addActionListener(actionListener);
        b.setToolTipText(tooltip);
        b.setMnemonic(mnemonic);
        b.putClientProperty("help", "Dialog/Properties/"+name);
        return b;
    }

    private void addSelected() {
        for (OsmPrimitive p : Main.ds.getSelected()) {
            boolean skip = false;
            // ordered relations may have the same member multiple times.
            // TODO: visual indication of the fact that one is there more than once?
            if (!ordered)
            {
                for (RelationMember rm : clone.members) {
                    if (rm.member == p || p == relation)
                    {
                        skip = true;
                        break;
                    }
                }
            }
            if (!skip)
            {
                RelationMember em = new RelationMember();
                em.member = p;
                em.role = "";
                // when working with ordered relations, we make an effort to
                // add the element before the first selected member.
                int[] rows = memberTable.getSelectedRows();
                if (ordered && rows.length > 0) {
                    clone.members.add(rows[0], em);
                } else {
                    clone.members.add(em);
                }
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
                while (m[i] != null) i++;
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
            OsmServerObjectReader reader = new OsmServerObjectReader(clone.id, OsmServerObjectReader.TYPE_REL, true);
            try {
                DataSet dataSet = reader.parseOsm();
                if (dataSet != null) {
                    final MergeVisitor visitor = new MergeVisitor(Main.main
                            .editLayer().data, dataSet);
                    for (final OsmPrimitive osm : dataSet.allPrimitives())
                        osm.visit(visitor);
                    visitor.fixReferences();

                    // copy the merged layer's data source info
                    for (DataSource src : dataSet.dataSources)
                        Main.main.editLayer().data.dataSources.add(src);
                    Main.main.editLayer().fireDataChange();

                    if (visitor.conflicts.isEmpty())
                        return;
                    final ConflictDialog dlg = Main.map.conflictDialog;
                    dlg.add(visitor.conflicts);
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("There were conflicts during import."));
                    if (!dlg.isVisible())
                        dlg.action
                                .actionPerformed(new ActionEvent(this, 0, ""));
                }

            } catch (SAXException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,tr("Error parsing server response.")+": "+e.getMessage(),
                tr("Error"), JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,tr("Cannot connect to server.")+": "+e.getMessage(),
                tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
