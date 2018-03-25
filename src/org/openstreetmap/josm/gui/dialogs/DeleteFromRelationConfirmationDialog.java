// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This dialog is used to get a user confirmation that a collection of primitives can be removed
 * from their parent relations.
 * @since 2308
 */
public class DeleteFromRelationConfirmationDialog extends JDialog implements TableModelListener {
    /** the unique instance of this dialog */
    private static DeleteFromRelationConfirmationDialog instance;

    /**
     * Replies the unique instance of this dialog
     *
     * @return The unique instance of this dialog
     */
    public static synchronized DeleteFromRelationConfirmationDialog getInstance() {
        if (instance == null) {
            instance = new DeleteFromRelationConfirmationDialog();
        }
        return instance;
    }

    /** the data model */
    private RelationMemberTableModel model;
    private final HtmlPanel htmlPanel = new HtmlPanel();
    private boolean canceled;
    private final JButton btnOK = new JButton(new OKAction());

    protected JPanel buildRelationMemberTablePanel() {
        JTable table = new JTable(model, new RelationMemberTableColumnModel());
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(new JScrollPane(table));
        return pnl;
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout());
        pnl.add(btnOK);
        btnOK.setFocusable(true);
        pnl.add(new JButton(new CancelAction()));
        pnl.add(new JButton(new ContextSensitiveHelpAction(ht("/Action/Delete#DeleteFromRelations"))));
        return pnl;
    }

    protected final void build() {
        model = new RelationMemberTableModel();
        model.addTableModelListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(htmlPanel, BorderLayout.NORTH);
        getContentPane().add(buildRelationMemberTablePanel(), BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);

        HelpUtil.setHelpContext(this.getRootPane(), ht("/Action/Delete#DeleteFromRelations"));

        addWindowListener(new WindowEventHandler());
    }

    protected void updateMessage() {
        int numObjectsToDelete = model.getNumObjectsToDelete();
        int numParentRelations = model.getNumParentRelations();
        final String msg1 = trn(
                "Please confirm to remove <strong>{0} object</strong>.",
                "Please confirm to remove <strong>{0} objects</strong>.",
                numObjectsToDelete, numObjectsToDelete);
        final String msg2 = trn(
                "{0} relation is affected.",
                "{0} relations are affected.",
                numParentRelations, numParentRelations);
        @I18n.QuirkyPluralString
        final String msg = "<html>" + msg1 + ' ' + msg2 + "</html>";
        htmlPanel.getEditorPane().setText(msg);
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

    /**
     * Constructs a new {@code DeleteFromRelationConfirmationDialog}.
     */
    public DeleteFromRelationConfirmationDialog() {
        super(GuiHelper.getFrameForComponent(Main.parent), "", ModalityType.DOCUMENT_MODAL);
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
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            Main.parent,
                            new Dimension(400, 200)
                    )
            ).applySafe(this);
            setCanceled(false);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateMessage();
        updateTitle();
    }

    /**
     * The table model which manages the list of relation-to-child references
     */
    public static class RelationMemberTableModel extends DefaultTableModel {
        private static class RelationToChildReferenceComparator implements Comparator<RelationToChildReference>, Serializable {
            @Override
            public int compare(RelationToChildReference o1, RelationToChildReference o2) {
                NameFormatter nf = DefaultNameFormatter.getInstance();
                int cmp = o1.getChild().getDisplayName(nf).compareTo(o2.getChild().getDisplayName(nf));
                if (cmp != 0) return cmp;
                cmp = o1.getParent().getDisplayName(nf).compareTo(o2.getParent().getDisplayName(nf));
                if (cmp != 0) return cmp;
                return Integer.compare(o1.getPosition(), o2.getPosition());
            }
        }

        private final transient List<RelationToChildReference> data;

        /**
         * Constructs a new {@code RelationMemberTableModel}.
         */
        public RelationMemberTableModel() {
            data = new ArrayList<>();
        }

        @Override
        public int getRowCount() {
            if (data == null) return 0;
            return data.size();
        }

        /**
         * Sets the data that should be displayed in the list.
         * @param references A list of references to display
         */
        public void populate(Collection<RelationToChildReference> references) {
            data.clear();
            if (references != null) {
                data.addAll(references);
            }
            data.sort(new RelationToChildReferenceComparator());
            fireTableDataChanged();
        }

        /**
         * Gets the list of children that are currently displayed.
         * @return The children.
         */
        public Set<OsmPrimitive> getObjectsToDelete() {
            Set<OsmPrimitive> ret = new HashSet<>();
            for (RelationToChildReference ref: data) {
                ret.add(ref.getChild());
            }
            return ret;
        }

        /**
         * Gets the number of elements {@link #getObjectsToDelete()} would return.
         * @return That number.
         */
        public int getNumObjectsToDelete() {
            return getObjectsToDelete().size();
        }

        /**
         * Gets the set of parent relations
         * @return All parent relations of the references
         */
        public Set<OsmPrimitive> getParentRelations() {
            Set<OsmPrimitive> ret = new HashSet<>();
            for (RelationToChildReference ref: data) {
                ret.add(ref.getParent());
            }
            return ret;
        }

        /**
         * Gets the number of elements {@link #getParentRelations()} would return.
         * @return That number.
         */
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
            case 2: return ref.getPosition()+1;
            case 3: return ref.getRole();
            default:
                assert false : "Illegal column index";
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    private static class RelationMemberTableColumnModel extends DefaultTableColumnModel {

        protected final void createColumns() {

            // column 0 - To Delete
            TableColumn col = new TableColumn(0);
            col.setHeaderValue(tr("To delete"));
            col.setResizable(true);
            col.setWidth(100);
            col.setPreferredWidth(100);
            col.setCellRenderer(new PrimitiveRenderer());
            addColumn(col);

            // column 0 - From Relation
            col = new TableColumn(1);
            col.setHeaderValue(tr("From Relation"));
            col.setResizable(true);
            col.setWidth(100);
            col.setPreferredWidth(100);
            col.setCellRenderer(new PrimitiveRenderer());
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

        RelationMemberTableColumnModel() {
            createColumns();
        }
    }

    class OKAction extends AbstractAction {
        OKAction() {
            putValue(NAME, tr("OK"));
            new ImageProvider("ok").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and remove the object from the relations"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCanceled(false);
            setVisible(false);
        }
    }

    class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and to abort deleting the objects"));
        }

        @Override
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
