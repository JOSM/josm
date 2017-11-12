// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.tagging.ac;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A sorted set of {@link AutoCompletionItem}s.
 *
 * Items are sorted with higher priority first, then according to lexicographic order
 * on the value of the {@code AutoCompletionListItem}.
 *
 * @since 12859 (extracted from {@code gui.tagging.ac.AutoCompletionList})
 */
public class AutoCompletionSet extends TreeSet<AutoCompletionItem> {

    // Keep a separate tree set of values for determining fast if a value is present
    private final Set<String> values = new TreeSet<>();

    @Override
    public boolean add(AutoCompletionItem e) {
        // Is there already an item for the value?
        String value = e.getValue();
        if (contains(value)) { // Fast
            Optional<AutoCompletionItem> result = stream().filter(i -> i.getValue().equals(e.getValue())).findFirst(); // Slow
            if (result.isPresent()) {
                AutoCompletionItem item = result.get();
                // yes: merge priorities
                AutoCompletionPriority newPriority = item.getPriority().mergeWith(e.getPriority());
                // if needed, remove/re-add the updated item to maintain set ordering
                if (!item.getPriority().equals(newPriority)) {
                    super.remove(item);
                    item.setPriority(newPriority);
                    return super.add(item);
                } else {
                    return false;
                }
            } else {
                // Should never happen if values is correctly synchronized with this set
                throw new IllegalStateException(value);
            }
        } else {
            values.add(value);
            return super.add(e);
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof AutoCompletionItem) {
            values.remove(((AutoCompletionItem) o).getValue());
        }
        return super.remove(o);
    }

    @Override
    public void clear() {
        values.clear();
        super.clear();
    }

    /**
     * Adds a list of strings to this list. Only strings which
     * are not null and which do not exist yet in the list are added.
     *
     * @param values a list of strings to add
     * @param priority the priority to use
     * @return {@code true} if this set changed as a result of the call
     */
    public boolean addAll(Collection<String> values, AutoCompletionPriority priority) {
        return addAll(values.stream().filter(Objects::nonNull).map(v -> new AutoCompletionItem(v, priority)).collect(Collectors.toList()));
    }

    /**
     * Adds values that have been entered by the user.
     * @param values values that have been entered by the user
     * @return {@code true} if this set changed as a result of the call
     */
    public boolean addUserInput(Collection<String> values) {
        int i = 0;
        boolean modified = false;
        for (String value : values) {
            if (value != null && add(new AutoCompletionItem(value, new AutoCompletionPriority(false, false, false, i++)))) {
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Checks whether an item with the given value is already in the list. Ignores priority of the items.
     *
     * @param value the value of an auto completion item
     * @return true, if value is in the list; false, otherwise
     */
    public boolean contains(String value) {
        return values.contains(value);
    }

    /**
     * Removes the auto completion item with key <code>key</code>
     * @param key the key
     * @return {@code true} if an element was removed
     */
    public boolean remove(String key) {
        return values.remove(key) && removeIf(i -> i.getValue().equals(key));
    }
}
