// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;

/**
 * Conditional {@link TemplateEntry} that executes another template in case a search expression applies
 * to the given data provider.
 */
public class SearchExpressionCondition implements TemplateEntry {

    private final Match condition;
    private final TemplateEntry text;

    /**
     * Creates a new {@link SearchExpressionCondition}.
     * @param condition the match condition that is checked before applying the child template
     * @param text the child template to execute in case the condition is fulfilled
     */
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((condition == null) ? 0 : condition.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SearchExpressionCondition other = (SearchExpressionCondition) obj;
        if (condition == null) {
            if (other.condition != null)
                return false;
        } else if (!condition.equals(other.condition))
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return true;
    }
}
