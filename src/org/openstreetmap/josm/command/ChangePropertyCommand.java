// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
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
     * Key and value pairs. If value is <code>null</code>, delete all key references with the given
     * key. Otherwise, change the tags of all objects to the given value or create keys of
     * those objects that do not have the key yet.
     */
    private final AbstractMap<String, String> tags;

    /**
     * Creates a command to change multiple tags of multiple objects
     *
     * @param objects the objects to modify
     * @param tags the tags to set
     */
    public ChangePropertyCommand(Collection<? extends OsmPrimitive> objects, AbstractMap<String, String> tags) {
        super();
        this.objects = new LinkedList<OsmPrimitive>();
        this.tags = tags;
        init(objects);
    }

    /**
     * Creates a command to change one tag of multiple objects
     *
     * @param objects the objects to modify
     * @param key the key of the tag to set
     * @param value the value of the key to set
     */
    public ChangePropertyCommand(Collection<? extends OsmPrimitive> objects, String key, String value) {
        this.objects = new LinkedList<OsmPrimitive>();
        this.tags = new HashMap<String, String>(1);
        this.tags.put(key, value);
        init(objects);
    }

    /**
     * Creates a command to change one tag of one object
     *
     * @param object the object to modify
     * @param key the key of the tag to set
     * @param value the value of the key to set
     */
    public ChangePropertyCommand(OsmPrimitive object, String key, String value) {
        this(Arrays.asList(object), key, value);
    }

    /**
     * Initialize the instance by finding what objects will be modified
     *
     * @param objects the objects to (possibly) modify
     */
    private void init(Collection<? extends OsmPrimitive> objects) {
        // determine what objects will be modified
        for (OsmPrimitive osm : objects) {
            boolean modified = false;

            // loop over all tags
            for (Map.Entry<String, String> tag : this.tags.entrySet()) {
                String oldVal = osm.get(tag.getKey());
                String newVal = tag.getValue();

                if (newVal == null || newVal.isEmpty()) {
                    if (oldVal != null)
                        // new value is null and tag exists (will delete tag)
                        modified = true;
                }
                else if (oldVal == null || !newVal.equals(oldVal))
                    // new value is not null and is different from current value
                    modified = true;
            }
            if (modified)
                this.objects.add(osm);
        }
    }

    @Override public boolean executeCommand() {
        Main.main.getCurrentDataSet().beginUpdate();
        try {
            super.executeCommand(); // save old

            for (OsmPrimitive osm : objects) {
                // loop over all tags
                for (Map.Entry<String, String> tag : this.tags.entrySet()) {
                    String oldVal = osm.get(tag.getKey());
                    String newVal = tag.getValue();

                    if (newVal == null || newVal.isEmpty()) {
                        if (oldVal != null)
                            osm.remove(tag.getKey());
                    }
                    else if (oldVal == null || !newVal.equals(oldVal))
                        osm.put(tag.getKey(), newVal);
                }
                // init() only keeps modified primitives. Therefore the modified
                // bit can be set without further checks.
                osm.setModified(true);
            }
            return true;
        }
        finally {
            Main.main.getCurrentDataSet().endUpdate();
        }
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.addAll(objects);
    }

    @Override
    public String getDescriptionText() {
        String text;
        if (objects.size() == 1 && tags.size() == 1) {
            OsmPrimitive primitive = objects.iterator().next();
            String msg = "";
            Map.Entry<String, String> entry = tags.entrySet().iterator().next();
            if (entry.getValue() == null) {
                switch(OsmPrimitiveType.from(primitive)) {
                case NODE: msg = marktr("Remove \"{0}\" for node ''{1}''"); break;
                case WAY: msg = marktr("Remove \"{0}\" for way ''{1}''"); break;
                case RELATION: msg = marktr("Remove \"{0}\" for relation ''{1}''"); break;
                }
                text = tr(msg, entry.getKey(), primitive.getDisplayName(DefaultNameFormatter.getInstance()));
            } else {
                switch(OsmPrimitiveType.from(primitive)) {
                case NODE: msg = marktr("Set {0}={1} for node ''{2}''"); break;
                case WAY: msg = marktr("Set {0}={1} for way ''{2}''"); break;
                case RELATION: msg = marktr("Set {0}={1} for relation ''{2}''"); break;
                }
                text = tr(msg, entry.getKey(), entry.getValue(), primitive.getDisplayName(DefaultNameFormatter.getInstance()));
            }
        } else if (objects.size() > 1 && tags.size() == 1) {
            Map.Entry<String, String> entry = tags.entrySet().iterator().next();
            if (entry.getValue() == null) {
                /* I18n: plural form for objects, but value < 2 not possible! */
                text = trn("Remove \"{0}\" for {1} object", "Remove \"{0}\" for {1} objects", objects.size(), entry.getKey(), objects.size());
            } else {
                /* I18n: plural form for objects, but value < 2 not possible! */
                text = trn("Set {0}={1} for {2} object", "Set {0}={1} for {2} objects", objects.size(), entry.getKey(), entry.getValue(), objects.size());
            }
        }
        else {
            boolean allnull = true;
            for (Map.Entry<String, String> tag : this.tags.entrySet()) {
                if (tag.getValue() != null) {
                    allnull = false;
                    break;
                }
            }

            if (allnull) {
                /* I18n: plural form detected for objects only (but value < 2 not possible!), try to do your best for tags */ 
                text = trn("Deleted {0} tags for {1} object", "Deleted {0} tags for {1} objects", objects.size(), tags.size(), objects.size());
            } else {
                /* I18n: plural form detected for objects only (but value < 2 not possible!), try to do your best for tags */
                text = trn("Set {0} tags for {1} object", "Set {0} tags for {1} objects", objects.size(), tags.size(), objects.size());
            }
        }
        return text;
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "key");
    }

    @Override public Collection<PseudoCommand> getChildren() {
        if (objects.size() == 1)
            return null;
        List<PseudoCommand> children = new ArrayList<PseudoCommand>();
        for (final OsmPrimitive osm : objects) {
            children.add(new PseudoCommand() {
                @Override public String getDescriptionText() {
                    return osm.getDisplayName(DefaultNameFormatter.getInstance());
                }

                @Override public Icon getDescriptionIcon() {
                    return ImageProvider.get(osm.getDisplayType());
                }

                @Override public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
                    return Collections.singleton(osm);
                }

            });
        }
        return children;
    }

    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }
}
