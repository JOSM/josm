// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Collection;


public class Variable implements TemplateEntry {

    private static final String SPECIAL_VARIABLE_PREFIX = "special:";
    private static final String SPECIAL_VALUE_EVERYTHING = "everything";


    private final String variableName;
    private final boolean special;

    public Variable(String variableName) {
        if (variableName.toLowerCase().startsWith(SPECIAL_VARIABLE_PREFIX)) {
            this.variableName = variableName.substring(SPECIAL_VARIABLE_PREFIX.length());
            // special:special:key means that real key named special:key is needed, not special variable
            this.special = !this.variableName.toLowerCase().startsWith(SPECIAL_VARIABLE_PREFIX);
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
                result.append(key).append("=").append(dataProvider.getTemplateValue(key, false));
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
        if (special && SPECIAL_VALUE_EVERYTHING.equals(variableName))
            return true;
        else
            return dataProvider.getTemplateValue(variableName, special) != null;
    }

    @Override
    public String toString() {
        return "{" + variableName + "}";
    }

    public boolean isSpecial() {
        return special;
    }

}