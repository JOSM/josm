// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that manipulate the key/value structure of several objects. Manages deletion,
 * adding and modify of values and keys.
 *
 * @author imi
 * @since 24
 */
public class ChangePropertyCommand extends Command {

    static final class OsmPseudoCommand implements PseudoCommand {
        private final OsmPrimitive osm;

        OsmPseudoCommand(OsmPrimitive osm) {
            this.osm = osm;
        }

        @Override
        public String getDescriptionText() {
            return osm.getDisplayName(DefaultNameFormatter.getInstance());
        }

        @Override
        public Icon getDescriptionIcon() {
            return ImageProvider.get(osm.getDisplayType());
        }

        @Override
        public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
            return Collections.singleton(osm);
        }
    }

    /**
     * All primitives that are affected with this command.
     */
    private final List<OsmPrimitive> objects = new LinkedList<>();

    /**
     * Key and value pairs. If value is <code>null</code>, delete all key references with the given
     * key. Otherwise, change the tags of all objects to the given value or create keys of
     * those objects that do not have the key yet.
     */
    private final Map<String, String> tags;

    /**
     * Creates a command to change multiple tags of multiple objects
     *
     * @param ds The target data set. Must not be {@code null}
     * @param objects the objects to modify. Must not be empty
     * @param tags the tags to set
     * @since 12726
     */
    public ChangePropertyCommand(DataSet ds, Collection<? extends OsmPrimitive> objects, Map<String, String> tags) {
        super(ds);
        this.tags = tags;
        init(objects);
    }

    /**
     * Creates a command to change multiple tags of multiple objects
     *
     * @param objects the objects to modify. Must not be empty, and objects must belong to a data set
     * @param tags the tags to set
     * @throws NullPointerException if objects is null or contain null item
     * @throws NoSuchElementException if objects is empty
     */
    public ChangePropertyCommand(Collection<? extends OsmPrimitive> objects, Map<String, String> tags) {
        this(objects.iterator().next().getDataSet(), objects, tags);
    }

    /**
     * Creates a command to change one tag of multiple objects
     *
     * @param objects the objects to modify. Must not be empty, and objects must belong to a data set
     * @param key the key of the tag to set
     * @param value the value of the key to set
     * @throws NullPointerException if objects is null or contain null item
     * @throws NoSuchElementException if objects is empty
     */
    public ChangePropertyCommand(Collection<? extends OsmPrimitive> objects, String key, String value) {
        super(objects.iterator().next().getDataSet());
        this.tags = Collections.singletonMap(key, value);
        init(objects);
    }

    /**
     * Creates a command to change one tag of one object
     *
     * @param object the object to modify. Must belong to a data set
     * @param key the key of the tag to set
     * @param value the value of the key to set
     * @throws NullPointerException if object is null
     */
    public ChangePropertyCommand(OsmPrimitive object, String key, String value) {
        this(Collections.singleton(object), key, value);
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
                    if (oldVal != null) {
                        // new value is null and tag exists (will delete tag)
                        modified = true;
                        break;
                    }
                } else if (oldVal == null || !newVal.equals(oldVal)) {
                    // new value is not null and is different from current value
                    modified = true;
                    break;
                }
            }
            if (modified)
                this.objects.add(osm);
        }
    }

    @Override
    public boolean executeCommand() {
        if (objects.isEmpty())
            return true;
        final DataSet dataSet = objects.get(0).getDataSet();
        if (dataSet != null) {
            dataSet.beginUpdate();
        }
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
                    } else if (oldVal == null || !newVal.equals(oldVal))
                        osm.put(tag.getKey(), newVal);
                }
                // init() only keeps modified primitives. Therefore the modified
                // bit can be set without further checks.
                osm.setModified(true);
            }
            return true;
        } finally {
            if (dataSet != null) {
                dataSet.endUpdate();
            }
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        modified.addAll(objects);
    }

    @Override
    public String getDescriptionText() {
        @I18n.QuirkyPluralString
        final String text;
        if (objects.size() == 1 && tags.size() == 1) {
            OsmPrimitive primitive = objects.get(0);
            String msg;
            Map.Entry<String, String> entry = tags.entrySet().iterator().next();
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                switch(OsmPrimitiveType.from(primitive)) {
                case NODE: msg = marktr("Remove \"{0}\" for node ''{1}''"); break;
                case WAY: msg = marktr("Remove \"{0}\" for way ''{1}''"); break;
                case RELATION: msg = marktr("Remove \"{0}\" for relation ''{1}''"); break;
                default: throw new AssertionError();
                }
                text = tr(msg, entry.getKey(), primitive.getDisplayName(DefaultNameFormatter.getInstance()));
            } else {
                switch(OsmPrimitiveType.from(primitive)) {
                case NODE: msg = marktr("Set {0}={1} for node ''{2}''"); break;
                case WAY: msg = marktr("Set {0}={1} for way ''{2}''"); break;
                case RELATION: msg = marktr("Set {0}={1} for relation ''{2}''"); break;
                default: throw new AssertionError();
                }
                text = tr(msg, entry.getKey(), entry.getValue(), primitive.getDisplayName(DefaultNameFormatter.getInstance()));
            }
        } else if (objects.size() > 1 && tags.size() == 1) {
            Map.Entry<String, String> entry = tags.entrySet().iterator().next();
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                /* I18n: plural form for objects, but value < 2 not possible! */
                text = trn("Remove \"{0}\" for {1} object", "Remove \"{0}\" for {1} objects", objects.size(), entry.getKey(), objects.size());
            } else {
                /* I18n: plural form for objects, but value < 2 not possible! */
                text = trn("Set {0}={1} for {2} object", "Set {0}={1} for {2} objects",
                        objects.size(), entry.getKey(), entry.getValue(), objects.size());
            }
        } else {
            boolean allNull = this.tags.entrySet().stream()
                    .allMatch(tag -> tag.getValue() == null || tag.getValue().isEmpty());

            if (allNull) {
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
        return ImageProvider.get("dialogs", "propertiesdialog", ImageProvider.ImageSizes.SMALLICON);
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        if (objects.size() == 1)
            return null;
        return objects.stream().map(OsmPseudoCommand::new).collect(Collectors.toList());
    }

    /**
     * Returns the number of objects that will effectively be modified, before the command is executed.
     * @return the number of objects that will effectively be modified (can be 0)
     * @see Command#getParticipatingPrimitives()
     * @since 8945
     */
    public final int getObjectsNumber() {
        return objects.size();
    }

    /**
     * Returns the tags to set (key/value pairs).
     * @return the tags to set (key/value pairs)
     */
    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), objects, tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ChangePropertyCommand that = (ChangePropertyCommand) obj;
        return Objects.equals(objects, that.objects) &&
                Objects.equals(tags, that.tags);
    }
}
