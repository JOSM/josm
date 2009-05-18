package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JList;
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
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.GBC;
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
    
    private SideButton sbEdit = new SideButton(marktr("Edit"), "edit", "Selection", tr( "Open an editor for the selected relation"), new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Relation toEdit = getSelected();
            if (toEdit == null)
                return;
            RelationEditor.getEditor(toEdit, null).setVisible(true);
        }
    });
    
    private SideButton sbDel = new SideButton(marktr("Delete"), "delete", "Selection", tr("Delete the selected relation"), new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Relation toDelete = getSelected();
            if (toDelete == null)
                return;
            
            Main.main.undoRedo.add(
                    new DeleteCommand(Collections.singleton(toDelete)));
        }
    });

    public RelationListDialog() {
        super(tr("Relations"), "relationlist", tr("Open a list of all relations."),
        Shortcut.registerShortcut("subwindow:relations", tr("Toggle: {0}", tr("Relations")), KeyEvent.VK_R, Shortcut.GROUP_LAYER), 150);
        displaylist.setCellRenderer(new OsmPrimitivRenderer());
        displaylist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        displaylist.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
                    Main.ds.setSelected((Relation)displaylist.getSelectedValue());
            }
        });

        add(new JScrollPane(displaylist), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1,4));

        buttonPanel.add(new SideButton(marktr("New"), "addrelation", "Selection", tr("Create a new relation"), new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // call relation editor with null argument to create new relation
                RelationEditor.getEditor(null, null).setVisible(true);
            }
        }), GBC.std());

        buttonPanel.add(sbEdit, GBC.std());

        buttonPanel.add(sbDel, GBC.eol());
        Layer.listeners.add(this);
        add(buttonPanel, BorderLayout.SOUTH);
        
        displaylist.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
               sbEdit.setEnabled(getSelected() != null);
               sbDel.setEnabled(getSelected() != null);
            }
        });
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) updateList();
    }

    public void updateList() {
        list.setSize(Main.ds.relations.size());
        int i = 0;
        for (OsmPrimitive e : DataSet.sort(Main.ds.relations)) {
            if (!e.deleted && !e.incomplete)
                list.setElementAt(e, i++);
        }
        list.setSize(i);

        if(Main.ds.relations.size() != 0) {
            setTitle(tr("Relations: {0}", Main.ds.relations.size()), true);
        } else {
            setTitle(tr("Relations"), false);
        }
        
        sbEdit.setEnabled(list.size() > 0);
        sbDel.setEnabled(list.size() > 0);
    }

    public void activeLayerChange(Layer a, Layer b) {
        if ((a == null || a instanceof OsmDataLayer) && b instanceof OsmDataLayer) {
            if (a != null) ((OsmDataLayer)a).listenerDataChanged.remove(this);
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
        if(list.size() == 1)
            displaylist.setSelectedIndex(0);
        return (Relation) displaylist.getSelectedValue();
    }
}
