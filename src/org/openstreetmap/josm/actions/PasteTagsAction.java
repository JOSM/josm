// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ShortCut;

public final class PasteTagsAction extends JosmAction implements SelectionChangedListener {

	public PasteTagsAction(JosmAction copyAction) {
		super(tr("Paste Tags"), "pastetags",
			tr("Apply tags of contents of paste buffer to all selected items."),
			ShortCut.registerShortCut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")), KeyEvent.VK_V, ShortCut.GROUP_MENU, ShortCut.SHIFT_DEFAULT), true);
		DataSet.selListeners.add(this);
		copyAction.addListener(this);
		setEnabled(false);
	}

	private void pasteKeys(Collection<Command> clist, Collection<? extends OsmPrimitive> pasteBufferSubset, Collection<OsmPrimitive> selectionSubset) {
		/* scan the paste buffer, and add tags to each of the selected objects.
		 * If a tag already exists, it is overwritten */
		if (selectionSubset != null && ! selectionSubset.isEmpty()) {
			for (Iterator<? extends OsmPrimitive> it = pasteBufferSubset.iterator(); it.hasNext();) {
				OsmPrimitive osm = it.next();
				Map<String, String> m = osm.keys;
				if(m != null)
				{
					for (String key : m.keySet()) {
						if (! key.equals("created_by"))
							clist.add(new ChangePropertyCommand(selectionSubset, key, osm.keys.get(key)));
					}
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		Collection<Command> clist = new LinkedList<Command>();
		pasteKeys(clist, Main.pasteBuffer.nodes, Main.ds.getSelectedNodes());
		pasteKeys(clist, Main.pasteBuffer.ways, Main.ds.getSelectedWays());
		pasteKeys(clist, Main.pasteBuffer.relations, Main.ds.getSelectedRelations());
		Main.main.undoRedo.add(new SequenceCommand(tr("Paste Tags"), clist));
		Main.ds.setSelected(Main.ds.getSelected()); // to force selection listeners, in particular the tag panel, to update
		Main.map.mapView.repaint();
	}

	private boolean containsSameKeysWithDifferentValues(Collection<? extends OsmPrimitive> osms) {
		Map<String,String> kvSeen = new HashMap<String,String>();
		for (Iterator<? extends OsmPrimitive> it = osms.iterator(); it.hasNext();) {
			OsmPrimitive osm = it.next();
			if (osm.keys == null || osm.keys.isEmpty())
				continue;
			for (String key : osm.keys.keySet()) {
				if (key.equals("created_by")) // we ignore created_by
					continue;
				String value = osm.keys.get(key);
				if (! kvSeen.containsKey(key))
					kvSeen.put(key, value);
				else if (! kvSeen.get(key).equals(value))
					return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether to enable the widget depending on the contents of the paste
	 * buffer and current selection
	 * @param pasteBuffer
	 */
	private void possiblyEnable(Collection<? extends OsmPrimitive> selection, DataSet pasteBuffer) {
		/* only enable if there is something selected to paste into and
			if we don't have conflicting keys in the pastebuffer */
		setEnabled(selection != null &&
				! selection.isEmpty() &&
				! pasteBuffer.allPrimitives().isEmpty() &&
				(Main.ds.getSelectedNodes().isEmpty() ||
					! containsSameKeysWithDifferentValues(pasteBuffer.nodes)) &&
				(Main.ds.getSelectedWays().isEmpty() ||
					! containsSameKeysWithDifferentValues(pasteBuffer.ways)) &&
				(Main.ds.getSelectedRelations().isEmpty() ||
					! containsSameKeysWithDifferentValues(pasteBuffer.relations)));
	}

	@Override public void pasteBufferChanged(DataSet newPasteBuffer) {
		possiblyEnable(Main.ds.getSelected(), newPasteBuffer);
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		possiblyEnable(newSelection, Main.pasteBuffer);
	}
}
