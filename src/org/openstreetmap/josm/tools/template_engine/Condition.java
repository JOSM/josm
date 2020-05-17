// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link TemplateEntry} that applies other templates based on conditions.
 * <p>
 * It goes through a number of template entries and executes the first one that is valid.
 */
public class Condition implements TemplateEntry {

    private final List<TemplateEntry> entries;

    /**
     * Constructs a new {@code Condition} with predefined template entries.
     * @param entries template entries
     */
    public Condition(Collection<TemplateEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    /**
     * Constructs a new {@code Condition}.
     */
    public Condition() {
        this.entries = new ArrayList<>();
    }

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        for (TemplateEntry entry: entries) {
            if (entry.isValid(dataProvider)) {
                entry.appendText(result, dataProvider);
                return;
            }
        }

        // Fallback to last entry
        TemplateEntry entry = entries.get(entries.size() - 1);
        entry.appendText(result, dataProvider);
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        return entries.stream().anyMatch(entry -> entry.isValid(dataProvider));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("?{ ");
        for (TemplateEntry entry: entries) {
            if (entry instanceof SearchExpressionCondition) {
                sb.append(entry);
            } else {
                sb.append('\'').append(entry).append('\'');
            }
            sb.append(" | ");
        }
        sb.setLength(sb.length() - 3);
        sb.append(" }");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return 31 + ((entries == null) ? 0 : entries.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Condition other = (Condition) obj;
        if (entries == null) {
            if (other.entries != null)
                return false;
        } else if (!entries.equals(other.entries))
            return false;
        return true;
    }
}
