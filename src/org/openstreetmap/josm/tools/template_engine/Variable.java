// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * {@link TemplateEntry} that inserts the value of a variable.
 * <p>
 * Variables starting with "special:" form a separate namespace and
 * provide actions other than simple key-value lookup.
 * <p>
 * A variable with no mapping for a given data provider will be considered not "valid"
 * (see {@link TemplateEntry#isValid(TemplateEngineDataProvider)}).
 */
public class Variable implements TemplateEntry {

    private static final String SPECIAL_VARIABLE_PREFIX = "special:";
    private static final String SPECIAL_VALUE_EVERYTHING = "everything";

    private final String variableName;
    private final boolean special;

    /**
     * Constructs a new {@code Variable}.
     * @param variableName the variable name (i.e. the key in the data provider key-value mapping);
     * will be considered "special" if the variable name starts with {@link #SPECIAL_VARIABLE_PREFIX}
     */
    public Variable(String variableName) {
        if (variableName.toLowerCase(Locale.ENGLISH).startsWith(SPECIAL_VARIABLE_PREFIX)) {
            this.variableName = variableName.substring(SPECIAL_VARIABLE_PREFIX.length());
            // special:special:key means that real key named special:key is needed, not special variable
            this.special = !this.variableName.toLowerCase(Locale.ENGLISH).startsWith(SPECIAL_VARIABLE_PREFIX);
        } else {
            this.variableName = variableName;
            this.special = false;
        }
    }

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        if (special && SPECIAL_VALUE_EVERYTHING.equals(variableName)) {
            Collection<String> keys = dataProvider.getTemplateKeys();
            boolean first = true;
            for (String key: keys) {
                if (!first) {
                    result.append(", ");
                } else {
                    first = false;
                }
                result.append(key).append('=').append(dataProvider.getTemplateValue(key, false));
            }
        } else {
            Object value = dataProvider.getTemplateValue(variableName, special);
            if (value != null) {
                result.append(value);
            }
        }
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        return (special && SPECIAL_VALUE_EVERYTHING.equals(variableName))
            || dataProvider.getTemplateValue(variableName, special) != null;
    }

    @Override
    public String toString() {
        return '{' + (special ? SPECIAL_VARIABLE_PREFIX : "") + variableName + '}';
    }

    /**
     * Check if this variable is special.
     *
     * @return true if this variable is special
     */
    public boolean isSpecial() {
        return special;
    }

    @Override
    public int hashCode() {
        return Objects.hash(special, variableName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Variable other = (Variable) obj;
        return this.special == other.special && Objects.equals(this.variableName, other.variableName);
    }
}
