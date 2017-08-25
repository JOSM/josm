// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;

public interface TemplateEngineDataProvider {
    Collection<String> getTemplateKeys();

    Object getTemplateValue(String name, boolean special);

    boolean evaluateCondition(Match condition);
}
