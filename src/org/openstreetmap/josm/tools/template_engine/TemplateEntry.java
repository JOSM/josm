// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.template_engine;


public interface TemplateEntry {
    void appendText(StringBuilder result, TemplateEngineDataProvider dataProvider);
    boolean isValid(TemplateEngineDataProvider dataProvider);
}