// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ConflictResolveCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.ConflictResolver;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;

public final class ConflictDialog extends ToggleDialog {

	public final Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();
	private final DefaultListModel model = new DefaultListModel();
	private final JList displaylist = new JList(model);

	public ConflictDialog() {
		super(tr("Conflict"), "conflict", tr("Merging conflicts."), KeyEvent.VK_C, 100);
		displaylist.setCellRenderer(new OsmPrimitivRenderer());
		displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		displaylist.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2)
					resolve();
			}
		});
		add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,2));
		buttonPanel.add(new SideButton(marktr("Resolve"), "conflict", "Conflict",
		tr("Open a merge dialog of all selected items in the list above."), new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				resolve();
			}
		}));

		buttonPanel.add(new SideButton(marktr("Select"), "select", "Conflict",
		tr("Set the selected elements on the map to the selected items in the list above."), new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
				for (Object o : displaylist.getSelectedValues())
					sel.add((OsmPrimitive)o);
				Main.ds.setSelected(sel);
			}
		}));
		add(buttonPanel, BorderLayout.SOUTH);

		DataSet.selListeners.add(new SelectionChangedListener(){
			public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
				displaylist.clearSelection();
				for (OsmPrimitive osm : newSelection) {
					if (conflicts.containsKey(osm)) {
						int pos = model.indexOf(osm);
						displaylist.addSelectionInterval(pos, pos);
					}
				}
			}
		});
		displaylist.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				Main.map.mapView.repaint();
			}
		});
	}

	private final void resolve() {
		if (displaylist.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(Main.parent,tr("Please select something from the conflict list."));
			return;
		}
		Map<OsmPrimitive, OsmPrimitive> sel = new HashMap<OsmPrimitive, OsmPrimitive>();
		for (int i : displaylist.getSelectedIndices()) {
			OsmPrimitive s = (OsmPrimitive)model.get(i);
			sel.put(s, conflicts.get(s));
		}
		ConflictResolver resolver = new ConflictResolver(sel);
		int answer = JOptionPane.showConfirmDialog(Main.parent, resolver, tr("Resolve Conflicts"), JOptionPane.OK_CANCEL_OPTION);
		if (answer != JOptionPane.OK_OPTION)
			return;
		Main.main.undoRedo.add(new ConflictResolveCommand(resolver.conflicts, sel));
		Main.map.mapView.repaint();
	}

	public final void rebuildList() {
		model.removeAllElements();
		for (OsmPrimitive osm : this.conflicts.keySet())
			if (osm instanceof Node)
				model.addElement(osm);
		for (OsmPrimitive osm : this.conflicts.keySet())
			if (osm instanceof Way)
				model.addElement(osm);
	}

	public final void add(Map<OsmPrimitive, OsmPrimitive> conflicts) {
		this.conflicts.putAll(conflicts);
		rebuildList();
	}

	/**
	 * Paint all conflicts that can be expressed on the main window.
	 */
	public void paintConflicts(final Graphics g, final NavigatableComponent nc) {
		Color preferencesColor = Main.pref.getColor("conflict", Color.gray);
		if (preferencesColor.equals(Color.BLACK))
			return;
		g.setColor(preferencesColor);
		Visitor conflictPainter = new Visitor(){
			public void visit(Node n) {
				Point p = nc.getPoint(n.eastNorth);
				g.drawRect(p.x-1, p.y-1, 2, 2);
			}
			public void visit(Node n1, Node n2) {
				Point p1 = nc.getPoint(n1.eastNorth);
				Point p2 = nc.getPoint(n2.eastNorth);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			public void visit(Way w) {
				Node lastN = null;
				for (Node n : w.nodes) {
					if (lastN == null) {
						lastN = n;
						continue;
					}
					visit(lastN, n);
					lastN = n;
				}
			}
			public void visit(Relation e) {
				for (RelationMember em : e.members)
					em.member.visit(this);
			}
		};
		for (Object o : displaylist.getSelectedValues())
			conflicts.get(o).visit(conflictPainter);
	}
}
