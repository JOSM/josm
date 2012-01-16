// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.util.NameVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that replaces the key of several objects
 *
 */
public class ChangePropertyKeyCommand extends Command {
    /**
     * All primitives, that are affected with this command.
     */
    private final List<OsmPrimitive> objects;
    /**
     * The key that is subject to change.
     */
    private final String key;
    /**
     * The mew key.
     */
    private final String newKey;

    /**
     * Constructor
     *
     * @param objects all objects subject to change replacement
     * @param key The key to replace
     * @param newKey the new value of the key
     */
    public ChangePropertyKeyCommand(Collection<? extends OsmPrimitive> objects, String key, String newKey) {
        this.objects = new LinkedList<OsmPrimitive>(objects);
        this.key = key;
        this.newKey = newKey;
    }

    @Override
    public boolean executeCommand() {
        if (!super.executeCommand())
            return false; // save old
        for (OsmPrimitive osm : objects) {
            if (osm.hasKeys()) {
                osm.setModified(true);
                String oldValue = osm.get(key);
                osm.put(newKey, oldValue);
                osm.remove(key);
            }
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.addAll(objects);
    }

    @Override
    public JLabel getDescription() {
        String text = tr( "Replace \"{0}\" by \"{1}\" for", key, newKey);
        if (objects.size() == 1) {
            NameVisitor v = new NameVisitor();
            objects.iterator().next().visit(v);
            text += " "+tr(v.className)+" "+v.name;
        } else {
            text += " "+objects.size()+" "+trn("object","objects",objects.size());
        }
        return new JLabel(text, ImageProvider.get("data", "key"), JLabel.HORIZONTAL);
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        if (objects.size() == 1)
            return null;
        List<PseudoCommand> children = new ArrayList<PseudoCommand>();

        final NameVisitor v = new NameVisitor();
        for (final OsmPrimitive osm : objects) {
            osm.visit(v);
            children.add(new PseudoCommand() {
                @Override
                public JLabel getDescription() {
                    return v.toLabel();
                }
                @Override
                public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
                    return Collections.singleton(osm);
                }
            });
        }
        return children;
    }
}
