// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;

public class SearchExpressionCondition implements TemplateEntry {

    private final Match condition;
    private final TemplateEntry text;

    public SearchExpressionCondition(Match condition, TemplateEntry text) {
        this.condition = condition;
        this.text = text;
    }

    @Override
    public void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider) {
        text.appendText(result, dataProvider);
    }

    @Override
    public boolean isValid(TemplateEngineDataProvider dataProvider) {
        return dataProvider.evaluateCondition(condition);
    }

    @Override
    public String toString() {
        return condition + " '" + text + '\'';
    }
}
