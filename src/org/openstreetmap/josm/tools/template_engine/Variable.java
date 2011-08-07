// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.List;


public class Variable implements TemplateEntry {

    private final String variableName;

    public Variable(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        if ("*".equals(variableName)) {
            List<String> keys = dataProvider.getTemplateKeys();
            boolean first = true;
            for (String key: keys) {
                if (!first) {
                    result.append(", ");
                } else {
                    first = false;
                }
                result.append(key).append("=").append(dataProvider.getTemplateValue(key));
            }
        } else {
            Object value = dataProvider.getTemplateValue(variableName);
            if (value != null) {
                result.append(value);
            }
        }
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        if ("*".equals(variableName))
            return true;
        else
            return dataProvider.getTemplateValue(variableName) != null;
    }

    @Override
    public String toString() {
        return "{" + variableName + "}";
    }

}