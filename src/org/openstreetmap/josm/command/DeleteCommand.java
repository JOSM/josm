// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Collections;

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

    /** 
     * Constructor for a collection of data
     */
	public DeleteCommand(Collection<? extends OsmPrimitive> data) {
		this.data = data;
	}
    /** 
     * Constructor for a single data item. Use the collection 
     * constructor to delete multiple objects.
     */
    public DeleteCommand(OsmPrimitive data) {
        this.data = Collections.singleton(data);
    }

	@Override public boolean executeCommand() {
		super.executeCommand();
		for (OsmPrimitive osm : data) {
			osm.delete(true);
		}
		return true;
	}
	
	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.addAll(data);
	}

	@Override public MutableTreeNode description() {
		NameVisitor v = new NameVisitor();

		if (data.size() == 1) {
			data.iterator().next().visit(v);
			return new DefaultMutableTreeNode(new JLabel(tr("Delete {1} {0}", v.name, tr(v.className)), v.icon, JLabel.HORIZONTAL));
		}

		String cname = null;
		String cnamem = null;
		for (OsmPrimitive osm : data) {
			osm.visit(v);
			if (cname == null)
			{
				cname = v.className;
				cnamem = v.classNamePlural;
			}
			else if (!cname.equals(v.className))
			{
				cname = "object";
				cnamem = trn("object", "objects", 2);
			}
		}
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JLabel(
				tr("Delete {0} {1}", data.size(), trn(cname, cnamem, data.size())), ImageProvider.get("data", cname), JLabel.HORIZONTAL));
		for (OsmPrimitive osm : data) {
			osm.visit(v);
			root.add(new DefaultMutableTreeNode(v.toLabel()));
		}
		return root;
	}
}
