package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.layer.DataChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.ImageProvider;

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
		super(tr("Relations"), "relationlist", tr("Open a list of all relations."), KeyEvent.VK_N, 150);
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

		JPanel buttonPanel = new JPanel(new GridLayout(1,3));

		buttonPanel.add(createButton(marktr("Add Relation"), "addrelation", tr("Create a new relation"), KeyEvent.VK_A, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// call relation editor with null argument to create new relation
				new RelationEditor(null).setVisible(true);
			}
		}));
		
		buttonPanel.add(createButton(marktr("Edit"), "edit", tr( "Open an editor for the selected relation"), KeyEvent.VK_E, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Relation toEdit = (Relation) displaylist.getSelectedValue();
				if (toEdit != null)
					new RelationEditor(toEdit).setVisible(true);				
			}
		}));
		
		buttonPanel.add(createButton(marktr("Delete"), "delete", tr("Delete the selected relation"), KeyEvent.VK_D, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Relation toDelete = (Relation) displaylist.getSelectedValue();
				if (toDelete != null) {
					Main.main.editLayer().add(new DeleteCommand(Collections.singleton(toDelete)));
				}
			}
		}));
		Layer.listeners.add(this);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JButton createButton(String name, String imagename, String tooltip, int mnemonic, ActionListener actionListener) {
		JButton b = new JButton(tr(name), ImageProvider.get("dialogs", imagename));
		b.setActionCommand(name);
		b.addActionListener(actionListener);
		b.setToolTipText(tooltip);
		b.setMnemonic(mnemonic);
		b.putClientProperty("help", "Dialog/Properties/"+name);
		return b;
	}

	@Override public void setVisible(boolean b) {
		super.setVisible(b);
		if (b) updateList();
	}
	
	public void updateList() {
		list.setSize(Main.ds.relations.size());
		int i = 0;
		for (Relation e : Main.ds.relations) {
			if (!e.deleted)
				list.setElementAt(e, i++);
		}
		list.setSize(i);
	}
	
	public void activeLayerChange(Layer a, Layer b) {
		if (a instanceof OsmDataLayer && b instanceof OsmDataLayer) {
			((OsmDataLayer)a).listenerDataChanged.remove(this);
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
	
}
