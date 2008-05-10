// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictItem;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ConflictResolver;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.tools.ImageProvider;

public class ConflictResolveCommand extends Command {

	private final Collection<ConflictItem> conflicts;
	private final Map<OsmPrimitive, OsmPrimitive> resolved;
	private Map<OsmPrimitive, OsmPrimitive> origAllConflicts;
	private final ConflictDialog conflictDialog;

	public ConflictResolveCommand(List<ConflictItem> conflicts, Map<OsmPrimitive, OsmPrimitive> resolved) {
		this.conflicts = conflicts;
		this.resolved = resolved;
		conflictDialog = Main.map.conflictDialog;
	}

	@Override public boolean executeCommand() {
		super.executeCommand();

		origAllConflicts = new HashMap<OsmPrimitive, OsmPrimitive>(conflictDialog.conflicts);
		
		Set<OsmPrimitive> completed = new HashSet<OsmPrimitive>(resolved.keySet());
		for (ConflictItem ci : conflicts) {
			for (Entry<OsmPrimitive, OsmPrimitive> e : resolved.entrySet()) {
				if (ci.resolution == ConflictResolver.Resolution.THEIR)
					ci.apply(e.getKey(), e.getValue());
				else if (ci.resolution == ConflictResolver.Resolution.MY)
					ci.apply(e.getValue(), e.getKey());
				else if (ci.hasConflict(e.getKey(), e.getValue()))
					completed.remove(e.getKey());
			}
		}
		if (!completed.isEmpty()) {
			for (OsmPrimitive k : completed)
				conflictDialog.conflicts.remove(k);
			conflictDialog.rebuildList();
 		}
		return true;
	}

	@Override public void undoCommand() {
		super.undoCommand();
		Main.map.conflictDialog.conflicts.clear();
		Main.map.conflictDialog.conflicts.putAll(origAllConflicts);
		Main.map.conflictDialog.rebuildList();
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		modified.addAll(resolved.keySet());
	}

	@Override public MutableTreeNode description() {
		int i = 0;
		for (ConflictItem c : conflicts)
			if (c.resolution != null)
				i++;
		return new DefaultMutableTreeNode(new JLabel(tr("Resolve {0} conflicts in {1} objects",i,resolved.size()), ImageProvider.get("data", "object"), JLabel.HORIZONTAL));
    }
}
