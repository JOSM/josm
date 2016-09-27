// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.ArrayList;
import java.util.List;

public class Condition implements TemplateEntry {

    private final List<TemplateEntry> entries = new ArrayList<>();

    public List<TemplateEntry> getEntries() {
        return entries;
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

        for (TemplateEntry entry: entries) {
            if (entry.isValid(dataProvider))
                return true;
        }

        return false;
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
}
