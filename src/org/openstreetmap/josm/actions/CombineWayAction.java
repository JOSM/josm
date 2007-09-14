// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.GBC;

/**
 * Combines multiple ways into one.
 * 
 * @author Imi
 */
public class CombineWayAction extends JosmAction implements SelectionChangedListener {

	public CombineWayAction() {
		super(tr("Combine Way"), "combineway", tr("Combine several ways into one."), KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
		DataSet.listeners.add(this);
	}

	public void actionPerformed(ActionEvent event) {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		LinkedList<Way> selectedWays = new LinkedList<Way>();
		
		for (OsmPrimitive osm : selection)
			if (osm instanceof Way)
				selectedWays.add((Way)osm);

		if (selectedWays.size() < 2) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least two ways to combine."));
			return;
		}

		// collect properties for later conflict resolving
		Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
		for (Way w : selectedWays) {
			for (Entry<String,String> e : w.entrySet()) {
				if (!props.containsKey(e.getKey()))
					props.put(e.getKey(), new TreeSet<String>());
				props.get(e.getKey()).add(e.getValue());
			}
		}
		
		Way oldWay = selectedWays.poll();
		Way newWay = new Way(oldWay);
		LinkedList<Command> cmds = new LinkedList<Command>();
		
		for (Way w : selectedWays)
			newWay.segments.addAll(w.segments);
		
		// display conflict dialog
		Map<String, JComboBox> components = new HashMap<String, JComboBox>();
		JPanel p = new JPanel(new GridBagLayout());
		for (Entry<String, Set<String>> e : props.entrySet()) {
			if (e.getValue().size() > 1) {
				JComboBox c = new JComboBox(e.getValue().toArray());
				c.setEditable(true);
				p.add(new JLabel(e.getKey()), GBC.std());
				p.add(Box.createHorizontalStrut(10), GBC.std());
				p.add(c, GBC.eol());
				components.put(e.getKey(), c);
			} else
				newWay.put(e.getKey(), e.getValue().iterator().next());
		}
		if (!components.isEmpty()) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, p, tr("Enter values for all conflicts."), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return;
			for (Entry<String, JComboBox> e : components.entrySet())
				newWay.put(e.getKey(), e.getValue().getEditor().getItem().toString());
		}

		cmds.add(new DeleteCommand(selectedWays));
		cmds.add(new ChangeCommand(oldWay, newWay));
		Main.main.undoRedo.add(new SequenceCommand(tr("Combine {0} ways", selectedWays.size()), cmds));
		Main.ds.setSelected(oldWay);
	}

	/**
	 * Enable the "Combine way" menu option if more then one way is selected
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		boolean first = false;
		for (OsmPrimitive osm : newSelection) {
			if (osm instanceof Way) {
				if (first) {
					setEnabled(true);
					return;
				}
				first = true;
			}
		}
		setEnabled(false);
	}
}
