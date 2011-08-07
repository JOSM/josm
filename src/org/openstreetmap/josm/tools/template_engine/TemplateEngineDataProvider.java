// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.List;

import org.openstreetmap.josm.actions.search.SearchCompiler.Match;

public interface TemplateEngineDataProvider {
    List<String> getTemplateKeys();
    Object getTemplateValue(String name);
    boolean evaluateCondition(Match condition);
}