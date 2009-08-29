package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A dialog showing all known relations, with buttons to add, edit, and
 * delete them.
 *
 * We don't have such dialogs for nodes, segments, and ways, because those
 * objects are visible on the map and can be selected there. Relations are not.
 */
public class RelationListDialog extends ToggleDialog implements LayerChangeListener, DataChangeListener {
    private static final Logger logger = Logger.getLogger(RelationListDialog.class.getName());

    /**
     * The selection's list data.
     */
    private final DefaultListModel list = new DefaultListModel();

    /**
     * The display list.
     */
    private JList displaylist = new JList(list);

    /** the edit action */
    private EditAction editAction;
    /** the delete action */
    private DeleteAction deleteAction;


    /**
     * constructor
     */
    public RelationListDialog() {
        super(tr("Relations"), "relationlist", tr("Open a list of all relations."),
                Shortcut.registerShortcut("subwindow:relations", tr("Toggle: {0}", tr("Relations")), KeyEvent.VK_R, Shortcut.GROUP_LAYER), 150);

        // create the list of relations
        //
        displaylist.setCellRenderer(new OsmPrimitivRenderer());
        displaylist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displaylist.addMouseListener(new DoubleClickAdapter());
        add(new JScrollPane(displaylist), BorderLayout.CENTER);

        // create the panel with buttons
        //
        JPanel buttonPanel = new JPanel(new GridLayout(1,3));

        // the new action
        //
        NewAction newAction = new NewAction();
        Layer.listeners.add(newAction);
        buttonPanel.add(new SideButton(newAction), GBC.std());

        // the edit action
        //
        editAction = new EditAction();
        displaylist.addListSelectionListener(editAction);
        buttonPanel.add(new SideButton(editAction), GBC.std());

        // the edit action
        //
        DuplicateAction duplicateAction = new DuplicateAction();
        displaylist.addListSelectionListener(duplicateAction);
        buttonPanel.add(new SideButton(duplicateAction), GBC.std());

        // the delete action
        //
        deleteAction = new DeleteAction();
        displaylist.addListSelectionListener(deleteAction);
        buttonPanel.add(new SideButton(deleteAction), GBC.eol());
        add(buttonPanel, BorderLayout.SOUTH);

        // register as layer listener
        //
        Layer.listeners.add(this);
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            updateList();
        }
    }

    protected int getNumRelations() {
        if (Main.main.getCurrentDataSet() == null) return 0;
        return Main.main.getCurrentDataSet().relations.size();
    }

    public void updateList() {
        if (Main.main.getCurrentDataSet() == null) {
            list.setSize(0);
            return;
        }
        Relation selected = getSelected();
        list.setSize(getNumRelations());
        if (getNumRelations() > 0 ) {
            int i = 0;
            for (OsmPrimitive e : DataSet.sort(Main.main.getCurrentDataSet().relations)) {
                if (!e.deleted && !e.incomplete) {
                    list.setElementAt(e, i++);
                }
            }
            list.setSize(i);
        }
        if(getNumRelations() != 0) {
            setTitle(tr("Relations: {0}", Main.main.getCurrentDataSet().relations.size()));
        } else {
            setTitle(tr("Relations"));
        }
        selectRelation(selected);
    }

    public void activeLayerChange(Layer a, Layer b) {
        updateList();
    }

    public void layerRemoved(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).listenerDataChanged.remove(this);
        }
        updateList();
    }

    public void layerAdded(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).listenerDataChanged.add(this);
        }
    }

    public void dataChanged(OsmDataLayer l) {
        updateList();
    }

    /**
     * Returns the currently selected relation, or null.
     *
     * @return the currently selected relation, or null
     */
    public Relation getCurrentRelation() {
        return (Relation) displaylist.getSelectedValue();
    }

    /**
     * Adds a selection listener to the relation list.
     *
     * @param listener the listener to add
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        displaylist.addListSelectionListener(listener);
    }

    /**
     * Removes a selection listener from the relation list.
     *
     * @param listener the listener to remove
     */
    public void removeListSelectionListener(ListSelectionListener listener) {
        displaylist.removeListSelectionListener(listener);
    }

    /**
     * @return The selected relation in the list
     */
    private Relation getSelected() {
        if(list.size() == 1) {
            displaylist.setSelectedIndex(0);
        }
        return (Relation) displaylist.getSelectedValue();
    }

    /**
     * Selects the relation <code>relation</code> in the list of relations.
     *
     * @param relation  the relation
     */
    public void selectRelation(Relation relation) {
        if (relation == null)
        {
            displaylist.clearSelection();
            return;
        }
        int i = -1;
        for (i=0; i < list.getSize(); i++) {
            Relation r = (Relation)list.get(i);
            if (r == relation) {
                break;
            }
        }
        if (i >= 0 && i < list.getSize()) {
            displaylist.setSelectedIndex(i);
            displaylist.ensureIndexIsVisible(i);
        }
        else
        {
            displaylist.clearSelection();
        }
    }

    class DoubleClickAdapter extends MouseAdapter {
        protected void setCurrentRelationAsSelection() {
            Main.main.getCurrentDataSet().setSelected((Relation)displaylist.getSelectedValue());
        }

        protected void editCurrentRelation() {
            new EditAction().launchEditor(getSelected());
        }

        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                if (e.isControlDown()) {
                    editCurrentRelation();
                } else {
                    setCurrentRelationAsSelection();
                }
            }
        }
    }

    /**
     * The edit action
     *
     */
    class EditAction extends AbstractAction implements ListSelectionListener{
        public EditAction() {
            putValue(SHORT_DESCRIPTION,tr( "Open an editor for the selected relation"));
            putValue(NAME, tr("Edit"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            setEnabled(false);
        }
        protected Collection<RelationMember> getMembersForCurrentSelection(Relation r) {
            Collection<RelationMember> members = new HashSet<RelationMember>();
            Collection<OsmPrimitive> selection = Main.map.mapView.getEditLayer().data.getSelected();
            for (RelationMember member: r.getMembers()) {
                if (selection.contains(member.getMember())) {
                    members.add(member);
                }
            }
            return members;
        }

        public void launchEditor(Relation toEdit) {
            if (toEdit == null)
                return;
            RelationEditor.getEditor(Main.map.mapView.getEditLayer(),toEdit, getMembersForCurrentSelection(toEdit)).setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            launchEditor(getSelected());
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }
    }

    /**
     * The delete action
     *
     */
    class DeleteAction extends AbstractAction implements ListSelectionListener, Runnable {
        class AbortException extends Exception {}

        public DeleteAction() {
            putValue(SHORT_DESCRIPTION,tr("Delete the selected relation"));
            putValue(NAME, tr("Delete"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            setEnabled(false);
        }

        public void run() {
            if (!isEnabled()) return;
            Relation toDelete = getSelected();
            if (toDelete == null)
                return;
            org.openstreetmap.josm.actions.mapmode.DeleteAction.deleteRelation(
                    Main.main.getEditLayer(),
                    toDelete
            );
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }
    }

    /**
     * The edit action
     *
     */
    class NewAction extends AbstractAction implements LayerChangeListener{
        public NewAction() {
            putValue(SHORT_DESCRIPTION,tr("Create a new relation"));
            putValue(NAME, tr("New"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "addrelation"));
            setEnabled(false);
        }

        public void run() {
            RelationEditor.getEditor(Main.map.mapView.getEditLayer(),null, null).setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        protected void updateEnabledState() {
            setEnabled(Main.main != null && Main.main.getEditLayer() != null);
        }

        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    /**
     * Creates a new relation with a copy of the current editor state
     *
     */
    class DuplicateAction extends AbstractAction implements ListSelectionListener {
        public DuplicateAction() {
            putValue(SHORT_DESCRIPTION, tr("Create a copy of this relation and open it in another editor window"));
            // FIXME provide an icon
            putValue(SMALL_ICON, ImageProvider.get("duplicate"));
            putValue(NAME, tr("Duplicate"));
            updateEnabledState();
        }

        public void launchEditorForDuplicate(Relation original) {
            Relation copy = new Relation();
            copy.cloneFrom(original);
            copy.id = 0;
            copy.modified = true;
            RelationEditor editor = RelationEditor.getEditor(
                    Main.main.getEditLayer(),
                    copy,
                    null /* no selected members */
            );
            editor.setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            launchEditorForDuplicate(getSelected());
        }

        protected void updateEnabledState() {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length == 1);
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }
}
