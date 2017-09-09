// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

public final class CompoundTemplateEntry implements TemplateEntry {

    public static TemplateEntry fromArray(TemplateEntry... entry) {
        if (entry.length == 0)
            return new StaticText("");
        else if (entry.length == 1)
            return entry[0];
        else
            return new CompoundTemplateEntry(entry);
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
}
