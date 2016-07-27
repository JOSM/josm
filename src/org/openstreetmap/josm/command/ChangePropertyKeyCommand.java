// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.util.NameVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that replaces the key of one or several objects
 *
 */
public class ChangePropertyKeyCommand extends Command {
    /**
     * All primitives, that are affected with this command.
     */
    private final List<? extends OsmPrimitive> objects;
    /**
     * The key that is subject to change.
     */
    private final String key;
    /**
     * The mew key.
     */
    private final String newKey;

    /**
     * Constructs a new {@code ChangePropertyKeyCommand}.
     *
     * @param object the object subject to change replacement
     * @param key The key to replace
     * @param newKey the new value of the key
     * @since 6329
     */
    public ChangePropertyKeyCommand(OsmPrimitive object, String key, String newKey) {
        this(Collections.singleton(object), key, newKey);
    }

    /**
     * Constructs a new {@code ChangePropertyKeyCommand}.
     *
     * @param objects all objects subject to change replacement
     * @param key The key to replace
     * @param newKey the new value of the key
     */
    public ChangePropertyKeyCommand(Collection<? extends OsmPrimitive> objects, String key, String newKey) {
        this.objects = new LinkedList<>(objects);
        this.key = key;
        this.newKey = newKey;
    }

    @Override
    public boolean executeCommand() {
        if (!super.executeCommand())
            return false; // save old
        for (OsmPrimitive osm : objects) {
            if (osm.hasKey(key) || osm.hasKey(newKey)) {
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
    public String getDescriptionText() {
        String text = tr("Replace \"{0}\" by \"{1}\" for", key, newKey);
        if (objects.size() == 1) {
            NameVisitor v = new NameVisitor();
            objects.get(0).accept(v);
            text += " "+tr(v.className)+" "+v.name;
        } else {
            text += " "+objects.size()+" "+trn("object", "objects", objects.size());
        }
        return text;
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "key");
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        if (objects.size() == 1)
            return null;
        List<PseudoCommand> children = new ArrayList<>();

        final NameVisitor v = new NameVisitor();
        for (final OsmPrimitive osm : objects) {
            osm.accept(v);
            final String name = v.name;
            final Icon icon = v.icon;
            children.add(new PseudoCommand() {
                @Override
                public String getDescriptionText() {
                    return name;
                }

                @Override
                public Icon getDescriptionIcon() {
                    return icon;
                }

                @Override
                public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
                    return Collections.singleton(osm);
                }
            });
        }
        return children;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), objects, key, newKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ChangePropertyKeyCommand that = (ChangePropertyKeyCommand) obj;
        return Objects.equals(objects, that.objects) &&
                Objects.equals(key, that.key) &&
                Objects.equals(newKey, that.newKey);
    }
}
