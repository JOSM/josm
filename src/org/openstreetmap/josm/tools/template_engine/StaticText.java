// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

/**
 * {@link TemplateEntry} representing a static string.
 * <p>
 * When compiling the template result, the given string will simply be inserted at the current position.
 */
public class StaticText implements TemplateEntry {

    private final String staticText;

    /**
     * Create a new {@code StaticText}.
     * @param staticText the text to insert verbatim
     */
    public StaticText(String staticText) {
        this.staticText = staticText;
    }

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        result.append(staticText);
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        return true;
    }

    @Override
    public String toString() {
        return staticText;
    }
}
