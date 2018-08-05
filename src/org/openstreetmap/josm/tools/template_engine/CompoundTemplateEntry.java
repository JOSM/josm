// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Arrays;

/**
 * {@link TemplateEntry} that concatenates several templates.
 */
public final class CompoundTemplateEntry implements TemplateEntry {

    /**
     * Factory method to concatenate several {@code TemplateEntry}s.
     *
     * If the number of entries is 0 or 1, the result may not be a {@code CompoundTemplateEntry},
     * but optimized to a static text or the single entry itself.
     * @param entries the {@code TemplateEntry}s to concatenate
     * @return a {@link TemplateEntry} that concatenates all the entries
     */
    public static TemplateEntry fromArray(TemplateEntry... entries) {
        if (entries.length == 0)
            return new StaticText("");
        else if (entries.length == 1)
            return entries[0];
        else
            return new CompoundTemplateEntry(entries);
    }

    private CompoundTemplateEntry(TemplateEntry... entries) {
        this.entries = entries;
    }

    private final TemplateEntry[] entries;

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        for (TemplateEntry te: entries) {
            te.appendText(result, dataProvider);
        }
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        for (TemplateEntry te: entries) {
            if (!te.isValid(dataProvider))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (TemplateEntry te: entries) {
            result.append(te);
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return 31 + Arrays.hashCode(entries);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CompoundTemplateEntry other = (CompoundTemplateEntry) obj;
        if (!Arrays.equals(entries, other.entries))
            return false;
        return true;
    }
}
