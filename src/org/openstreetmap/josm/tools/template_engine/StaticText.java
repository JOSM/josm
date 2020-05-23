// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(staticText);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        StaticText other = (StaticText) obj;
        if (staticText == null) {
            if (other.staticText != null)
                return false;
        } else if (!staticText.equals(other.staticText))
            return false;
        return true;
    }
}
