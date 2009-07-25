package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.OptionPaneUtil;
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
        displaylist.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    Main.main.getCurrentDataSet().setSelected((Relation)displaylist.getSelectedValue());
                }
            }
        });
        add(new JScrollPane(displaylist), BorderLayout.CENTER);

        // create the panel with buttons
        //
        JPanel buttonPanel = new JPanel(new GridLayout(1,3));
        buttonPanel.add(new SideButton(marktr("New"), "addrelation", "Selection", tr("Create a new relation"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // call relation editor with null argument to create new relation
                RelationEditor.getEditor(Main.map.mapView.getEditLayer(),null, null).setVisible(true);
            }
        }), GBC.std());

        // the edit action
        //
        editAction = new EditAction();
        displaylist.addListSelectionListener(editAction);
        buttonPanel.add(new SideButton(editAction), GBC.std());

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
            setTitle(tr("Relations: {0}", Main.main.getCurrentDataSet().relations.size()), true);
        } else {
            setTitle(tr("Relations"), false);
        }
        selectRelation(selected);
    }

    public void activeLayerChange(Layer a, Layer b) {
        if ((a == null || a instanceof OsmDataLayer) && b instanceof OsmDataLayer) {
            if (a != null) {
                ((OsmDataLayer)a).listenerDataChanged.remove(this);
            }
            ((OsmDataLayer)b).listenerDataChanged.add(this);
            updateList();
            repaint();
        }
    }

    public void layerRemoved(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).listenerDataChanged.remove(this);
        }
    }

    public void layerAdded(Layer a) {
        if (a instanceof OsmDataLayer) {
            ((OsmDataLayer)a).listenerDataChanged.add(this);
        }
    }

    public void dataChanged(OsmDataLayer l) {
        updateList();
        repaint();
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

    /**
     * The edit action
     *
     */
    class EditAction extends AbstractAction implements ListSelectionListener, Runnable{
        public EditAction() {
            putValue(SHORT_DESCRIPTION,tr( "Open an editor for the selected relation"));
            putValue(NAME, tr("Edit"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            setEnabled(false);
        }

        public void run() {
            if (!isEnabled()) return;
            Relation toEdit = getSelected();
            if (toEdit == null)
                return;
            RelationEditor.getEditor(Main.map.mapView.getEditLayer(),toEdit, null).setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            run();
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
            Main.main.undoRedo.add(
                    new DeleteCommand(Collections.singleton(toDelete)));
        }

        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(displaylist.getSelectedIndices() != null && displaylist.getSelectedIndices().length > 0);
        }
    }
}
