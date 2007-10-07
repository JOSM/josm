// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A small tool dialog for displaying the current selection. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 *
 * @author imi
 */
public class SelectionListDialog extends ToggleDialog implements SelectionChangedListener {

	/**
	 * The selection's list data.
	 */
	private final DefaultListModel list = new DefaultListModel();
	/**
	 * The display list.
	 */
	private JList displaylist = new JList(list);

	public SelectionListDialog() {
		super(tr("Current Selection"), "selectionlist", tr("Open a selection list window."), KeyEvent.VK_E, 150);
		displaylist.setCellRenderer(new OsmPrimitivRenderer());
		displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		displaylist.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() < 2)
					return;
				updateMap();
			}
		});

		add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,2));

		buttonPanel.add(createButton("Select", "mapmode/selection/select", "Set the selected elements on the map to the selected items in the list above.", new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateMap();
			}
		}));

		buttonPanel.add(createButton("Reload", "dialogs/refresh", "Refresh the selection list.", new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				selectionChanged(Main.ds.getSelected());
            }
		}));

		buttonPanel.add(createButton("Search", "dialogs/search", "Search for objects.", Main.main.menu.search));

		add(buttonPanel, BorderLayout.SOUTH);
		selectionChanged(Main.ds.getSelected());

		DataSet.selListeners.add(this);
	}

	private JButton createButton(String name, String icon, String tooltip, ActionListener action) {
		JButton button = new JButton(tr(name), ImageProvider.get(icon));
		button.setToolTipText(tr(tooltip));
		button.addActionListener(action);
		button.putClientProperty("help", "Dialog/SelectionList/"+name);
		return button;
	}

	@Override public void setVisible(boolean b) {
		super.setVisible(b);
		if (b)
			selectionChanged(Main.ds.getSelected());
	}


	/**
	 * Called when the selection in the dataset changed.
	 * @param newSelection The new selection array.
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (list == null)
			return; // selection changed may be received in base class constructor before init
		if (!isVisible())
			return;
		OsmPrimitive[] selArr = new OsmPrimitive[newSelection.size()];
		selArr = newSelection.toArray(selArr);
		Arrays.sort(selArr);
		list.setSize(selArr.length);
		int i = 0;
		for (OsmPrimitive osm : selArr)
			list.setElementAt(osm, i++);
	}

	/**
	 * Sets the selection of the map to the current selected items.
	 */
	public void updateMap() {
		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
		for (int i = 0; i < list.getSize(); ++i)
			if (displaylist.isSelectedIndex(i))
				sel.add((OsmPrimitive)list.get(i));
		Main.ds.setSelected(sel);
	}
}
