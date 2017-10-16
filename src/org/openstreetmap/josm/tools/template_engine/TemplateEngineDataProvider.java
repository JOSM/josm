// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;

/**
 * Interface for objects that can be used with a template to generate a string.
 * <p>
 * Provides the necessary information for the template to be applied.
 */
public interface TemplateEngineDataProvider {
    /**
     * Get the collection of all keys that can be mapped to values.
     * @return all keys that can be mapped to values
     */
    Collection<String> getTemplateKeys();

    /**
     * Map a key to a value given the properties of the object.
     * @param key the key to map
     * @param special if the key is a "special:*" keyword that is used
     * to get certain information or automated behavior
     * @return a value that the key is mapped to or "special" information in case {@code special} is true
     */
    Object getTemplateValue(String key, boolean special);

    /**
     * Check if a condition holds for the object represented by this {@link TemplateEngineDataProvider}.
     * @param condition the condition to check (which is a search expression)
     * @return true if the condition holds
     */
    boolean evaluateCondition(Match condition);
}
