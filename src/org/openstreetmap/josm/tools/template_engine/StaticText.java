// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;


public class StaticText implements TemplateEntry {

    private final String staticText;

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