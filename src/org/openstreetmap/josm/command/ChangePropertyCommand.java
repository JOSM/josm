// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that manipulate the key/value structure of several objects. Manages deletion,
 * adding and modify of values and keys.
 *
 * @author imi
 */
public class ChangePropertyCommand extends Command {
    /**
     * All primitives that are affected with this command.
     */
    private final List<OsmPrimitive> objects;
    /**
     * The key that is subject to change.
     */
    private final String key;
    /**
     * The key value. If it is <code>null</code>, delete all key references with the given
     * key. Otherwise, change the properties of all objects to the given value or create keys of
     * those objects that do not have the key yet.
     */
    private final String value;

    public ChangePropertyCommand(Collection<? extends OsmPrimitive> objects, String key, String value) {
        this.objects = new LinkedList<OsmPrimitive>();
        this.key = key;
        this.value = value;
        if (value == null) {
            for (OsmPrimitive osm : objects) {
                if(osm.get(key) != null)
                    this.objects.add(osm);
            }
        } else {
            for (OsmPrimitive osm : objects) {
                String val = osm.get(key);
                if (val == null || !value.equals(val)) {
                    this.objects.add(osm);
                }
            }
        }
    }

    public ChangePropertyCommand(OsmPrimitive object, String key, String value) {
        this.objects = new LinkedList<OsmPrimitive>();
        this.key = key;
        this.value = value;
        String val = object.get(key);
        if ((value == null && val != null)
        || (value != null && (val == null || !value.equals(val))))
            this.objects.add(object);
    }

    @Override public boolean executeCommand() {
        super.executeCommand(); // save old
        if (value == null) {
            for (OsmPrimitive osm : objects) {
                osm.modified = true;
                osm.remove(key);
            }
        } else {
            for (OsmPrimitive osm : objects) {
                osm.modified = true;
                osm.put(key, value);
            }
        }
        return true;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.addAll(objects);
    }

    @Override public MutableTreeNode description() {
        String text;
        if (objects.size() == 1) {
            NameVisitor v = new NameVisitor();
            objects.iterator().next().visit(v);
            text = value == null
            ? tr("Remove \"{0}\" for {1} ''{2}''", key, tr(v.className), v.name)
            : tr("Set {0}={1} for {2} ''{3}''",key,value, tr(v.className), v.name);
        }
        else
        {
            text = value == null
            ? tr("Remove \"{0}\" for {1} {2}", key, objects.size(), trn("object","objects",objects.size()))
            : tr("Set {0}={1} for {2} {3}",key,value, objects.size(), trn("object","objects",objects.size()));
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JLabel(text, ImageProvider.get("data", "key"), JLabel.HORIZONTAL));
        if (objects.size() == 1)
            return root;
        NameVisitor v = new NameVisitor();
        for (OsmPrimitive osm : objects) {
            osm.visit(v);
            root.add(new DefaultMutableTreeNode(v.toLabel()));
        }
        return root;
    }
}
