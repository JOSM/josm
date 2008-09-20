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
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.GBC;

/**
 * A dialog showing all known relations, with buttons to add, edit, and
 * delete them. 
 * 
 * We don't have such dialogs for nodes, segments, and ways, becaus those
 * objects are visible on the map and can be selected there. Relations are not.
 *
 * @author Frederik Ramm <frederik@remote.org>
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

	public RelationListDialog() {
		super(tr("Relations"), "relationlist", tr("Open a list of all relations."), KeyEvent.VK_R, 150);
		displaylist.setCellRenderer(new OsmPrimitivRenderer());
		displaylist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		displaylist.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() < 2)
					return;
				Relation toEdit = (Relation) displaylist.getSelectedValue();
				if (toEdit != null)
					new RelationEditor(toEdit).setVisible(true);
			}
		});

		add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,4));
		
		buttonPanel.add(new SideButton(marktr("New"), "addrelation", "Selection", tr("Create a new relation"), new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// call relation editor with null argument to create new relation
				new RelationEditor(null).setVisible(true);
			}
		}), GBC.std());
		
		buttonPanel.add(new SideButton(marktr("Select"), "select", "Selection", tr("Select this relation"), new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// replace selection with the relation from the list
				Main.ds.setSelected((Relation)displaylist.getSelectedValue());
			}
		}), GBC.std());
		
		buttonPanel.add(new SideButton(marktr("Edit"), "edit", "Selection", tr( "Open an editor for the selected relation"), new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Relation toEdit = (Relation) displaylist.getSelectedValue();
				if (toEdit != null)
					new RelationEditor(toEdit).setVisible(true);				
			}
		}), GBC.std());
		
		buttonPanel.add(new SideButton(marktr("Delete"), "delete", "Selection", tr("Delete the selected relation"), new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Relation toDelete = (Relation) displaylist.getSelectedValue();
				if (toDelete != null) {
					Main.main.undoRedo.add(
						new DeleteCommand(Collections.singleton(toDelete)));
				}
			}
		}), GBC.eol());
		Layer.listeners.add(this);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	@Override public void setVisible(boolean b) {
		super.setVisible(b);
		if (b) updateList();
	}
	
	public void updateList() {
		list.setSize(Main.ds.relations.size());
		int i = 0;
		for (OsmPrimitive e : Main.ds.sort(Main.ds.relations)) {
			if (!e.deleted && !e.incomplete)
				list.setElementAt(e, i++);
		}
		list.setSize(i);
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
}
