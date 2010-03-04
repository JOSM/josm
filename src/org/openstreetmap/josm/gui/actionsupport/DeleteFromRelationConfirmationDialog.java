// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.actionsupport;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * This dialog is used to get a user confirmation that a collection of primitives can be removed
 * from their parent relations.
 *
 */
public class DeleteFromRelationConfirmationDialog extends JDialog implements TableModelListener {
    /** the unique instance of this dialog */
    static private DeleteFromRelationConfirmationDialog instance;

    /**
     * Replies the unique instance of this dialog
     *
     * @return
     */
    static public DeleteFromRelationConfirmationDialog getInstance() {
        if (instance == null) {
            instance = new DeleteFromRelationConfirmationDialog();
        }
        return instance;
    }

    /** the data model */
    private RelationMemberTableModel model;
    private JEditorPane jepMessage;
    private boolean canceled;
    private SideButton btnOK;

    protected JPanel buildMessagePanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        jepMessage = new JEditorPane("text/html", "");
        jepMessage.setOpaque(false);
        jepMessage.setEditable(false);
        Font f = UIManager.getFont("Label.font");
        StyleSheet ss = new StyleSheet();
        String rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                f.isBold() ? "bold" : "normal",
                        f.isItalic() ? "italic" : "normal"
        );
        rule = "body {" + rule + "}";
        rule = MessageFormat.format(
                "font-family: ''{0}'';font-size: {1,number}pt; font-weight: {2}; font-style: {3}",
                f.getName(),
                f.getSize(),
                "bold",
                f.isItalic() ? "italic" : "normal"
        );
        rule = "strong {" + rule + "}";
        ss.addRule(rule);
        ss.addRule("a {text-decoration: underline; color: blue}");
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(ss);
        jepMessage.setEditorKit(kit);

        pnl.setLayout(new BorderLayout());
        pnl.add(jepMessage, BorderLayout.CENTER);
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return pnl;
    }

    protected JPanel buildRelationMemberTablePanel() {
        JTable table = new JTable(model, new RelationMemberTableColumnModel());
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(new JScrollPane(table));
        return pnl;
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout());
        pnl.add(btnOK = new SideButton(new OKAction()));
        btnOK.setFocusable(true);
        pnl.add(new SideButton(new CancelAction()));
        pnl.add(new SideButton(new ContextSensitiveHelpAction(ht("/Action/Delete#DeleteFromRelations"))));
        return pnl;
    }

    protected void build() {
        model = new RelationMemberTableModel();
        model.addTableModelListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildMessagePanel(), BorderLayout.NORTH);
        getContentPane().add(buildRelationMemberTablePanel(), BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);

        HelpUtil.setHelpContext(this.getRootPane(), ht("/Action/Delete#DeleteFromRelations"));

        addWindowListener(new WindowEventHandler());
    }

    protected void updateMessage() {
        int numObjectsToDelete = model.getNumObjectsToDelete();
        int numParentRelations = model.getNumParentRelations();
        String msg;
        if (numObjectsToDelete == 1 && numParentRelations == 1) {
            msg = tr("<html>Please confirm to remove <strong>1 object</strong> from <strong>1 relation</strong>.</html>");
        } else if (numObjectsToDelete == 1 && numParentRelations > 1) {
            msg = tr("<html>Please confirm to remove <strong>1 object</strong> from <strong>{0} relations</strong>.</html>", numParentRelations);
        } else if (numObjectsToDelete > 1 && numParentRelations == 1) {
            msg = tr("<html>Please confirm to remove <strong>1 object</strong> from <strong>{0} relations</strong>.</html>", numParentRelations);
        } else {
            msg = tr("<html>Please confirm to remove <strong>{0} objects</strong> from <strong>{1} relations</strong>.</html>", numObjectsToDelete,numParentRelations);
        }
        jepMessage.setText(msg);
        invalidate();
    }

    protected void updateTitle() {
        int numObjectsToDelete = model.getNumObjectsToDelete();
        if (numObjectsToDelete > 0) {
            setTitle(trn("Deleting {0} object", "Deleting {0} objects", numObjectsToDelete, numObjectsToDelete));
        } else {
            setTitle(tr("Delete objects"));
        }
    }

    public DeleteFromRelationConfirmationDialog() {
        super(JOptionPane.getFrameForComponent(Main.parent), "", true /* modal */);
        build();
    }

    /**
     * Replies the data model used in this dialog
     *
     * @return the data model
     */
    public RelationMemberTableModel getModel() {
        return model;
    }

    /**
     * Replies true if the dialog was canceled
     *
     * @return true if the dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName()  + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(400,200)
                    )
            ).applySafe(this);
            setCanceled(false);
        } else if(!visible && isShowing()) {
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    public void tableChanged(TableModelEvent e) {
        updateMessage();
        updateTitle();
    }

    /**
     * The table model which manages the list of relation-to-child references
     *
     */
    public static class RelationMemberTableModel extends DefaultTableModel {
        private ArrayList<RelationToChildReference> data;

        public RelationMemberTableModel() {
            data = new ArrayList<RelationToChildReference>();
        }

        @Override
        public int getRowCount() {
            if (data == null) return 0;
            return data.size();
        }

        protected void sort() {
            Collections.sort(
                    data,
                    new Comparator<RelationToChildReference>() {
                        private NameFormatter nf = DefaultNameFormatter.getInstance();
                        public int compare(RelationToChildReference o1, RelationToChildReference o2) {
                            int cmp = o1.getChild().getDisplayName(nf).compareTo(o2.getChild().getDisplayName(nf));
                            if (cmp != 0) return cmp;
                            cmp = o1.getParent().getDisplayName(nf).compareTo(o2.getParent().getDisplayName(nf));
                            if (cmp != 0) return cmp;
                            return Integer.valueOf(o1.getPosition()).compareTo(o2.getPosition());
                        }
                    }
            );
        }

        public void populate(Collection<RelationToChildReference> references) {
            data.clear();
            if (references != null) {
                data.addAll(references);
            }
            sort();
            fireTableDataChanged();
        }

        public Set<OsmPrimitive> getObjectsToDelete() {
            HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
            for (RelationToChildReference ref: data) {
                ret.add(ref.getChild());
            }
            return ret;
        }

        public int getNumObjectsToDelete() {
            return getObjectsToDelete().size();
        }

        public Set<OsmPrimitive> getParentRelations() {
            HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
            for (RelationToChildReference ref: data) {
                ret.add(ref.getParent());
            }
            return ret;
        }

        public int getNumParentRelations() {
            return getParentRelations().size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null) return null;
            RelationToChildReference ref = data.get(rowIndex);
            switch(columnIndex) {
            case 0: return ref.getChild();
            case 1: return ref.getParent();
            case 2: return ref.getPosition();
            case 3: return ref.getRole();
            default:
                assert false: "Illegal column index";
            }
            return null;
        }

    }

    private static class RelationMemberTableColumnModel extends DefaultTableColumnModel{

        protected void createColumns() {
            TableColumn col = null;

            // column 0 - To Delete
            col = new TableColumn(0);
            col.setHeaderValue(tr("To delete"));
            col.setResizable(true);
            col.setWidth(100);
            col.setPreferredWidth(100);
            col.setCellRenderer(new OsmPrimitivRenderer());
            addColumn(col);

            // column 0 - From Relation
            col = new TableColumn(1);
            col.setHeaderValue(tr("From Relation"));
            col.setResizable(true);
            col.setWidth(100);
            col.setPreferredWidth(100);
            col.setCellRenderer(new OsmPrimitivRenderer());
            addColumn(col);

            // column 1 - Pos.
            col = new TableColumn(2);
            col.setHeaderValue(tr("Pos."));
            col.setResizable(true);
            col.setWidth(30);
            col.setPreferredWidth(30);
            addColumn(col);

            // column 2 - Role
            col = new TableColumn(3);
            col.setHeaderValue(tr("Role"));
            col.setResizable(true);
            col.setWidth(50);
            col.setPreferredWidth(50);
            addColumn(col);
        }

        public RelationMemberTableColumnModel() {
            createColumns();
        }
    }

    class OKAction extends AbstractAction {
        public OKAction() {
            putValue(NAME, tr("OK"));
            putValue(SMALL_ICON, ImageProvider.get("ok"));
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and remove the object from the relations"));
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and to abort deleting the objects"));
        }

        public void actionPerformed(ActionEvent e) {
            setCanceled(true);
            setVisible(false);
        }
    }

    class WindowEventHandler extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            setCanceled(true);
        }

        @Override
        public void windowOpened(WindowEvent e) {
            btnOK.requestFocusInWindow();
        }
    }
}
