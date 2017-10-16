// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;

/**
 * Interface for one node in the abstract syntax tree that is the result of parsing a template
 * string with {@link TemplateParser}.
 *
 * The node can either be branching (condition, context switch) or a leaf node (variable, static text).
 * The root node, representing the entire template is also a {@code TemplateEntry}.
 */
public interface TemplateEntry {
    /**
     * Execute this template by generating text for a given data provider.
     * @param result the {@link StringBuilder} to append the text to
     * @param dataProvider the data provider from which information should be compiled to a string
     */
    void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider);

    /**
     * Check if this template is applicable to the given data provider.
     *
     * @param dataProvider the data provider to check
     * @return true if all conditions are fulfilled to apply the template (for instance all
     * required key=value mappings are present), false otherwise
     */
    boolean isValid(TemplateEngineDataProvider dataProvider);
}
