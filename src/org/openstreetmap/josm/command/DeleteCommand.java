// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand extends Command {

	/**
	 * The primitive that get deleted.
	 */
	private final Collection<? extends OsmPrimitive> data;

	public DeleteCommand(Collection<? extends OsmPrimitive> data) {
		this.data = data;
	}

	@Override public void executeCommand() {
		super.executeCommand();
		for (OsmPrimitive osm : data) {
			osm.delete(true);
		}
	}
	
	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.addAll(data);
	}

	@Override public MutableTreeNode description() {
		NameVisitor v = new NameVisitor();

		if (data.size() == 1) {
			data.iterator().next().visit(v);
			return new DefaultMutableTreeNode(new JLabel(tr("Delete")+" "+tr(v.className)+" "+v.name, v.icon, JLabel.HORIZONTAL));
		}

		String cname = null;
		for (OsmPrimitive osm : data) {
			osm.visit(v);
			if (cname == null)
				cname = v.className;
			else if (!cname.equals(v.className))
				cname = "object";
		}
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JLabel(
				tr("Delete")+" "+data.size()+" "+trn(cname, cname+"s", data.size()), ImageProvider.get("data", cname), JLabel.HORIZONTAL));
		for (OsmPrimitive osm : data) {
			osm.visit(v);
			root.add(new DefaultMutableTreeNode(v.toLabel()));
		}
		return root;
	}
}
