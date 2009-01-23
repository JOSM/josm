//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

/**
* Only the plugin name, its jar location and the description.
* In other words, this is the minimal requirement the plugin preference page
* needs to show the plugin as available
*
* @author imi
*/
public class PluginDescription implements Comparable<Object> {
    // Note: All the following need to be public instance variables of
    // type String.  (Plugin description XMLs from the server are parsed
    // with tools.XmlObjectParser, which uses reflection to access them.)
    public String name;
    public String description;
    public String resource;
    public String version;
    public PluginDescription(String name, String description, String resource, String version) {
        this.name = name;
        this.description = description;
        this.resource = resource;
        this.version = version;
    }
    public PluginDescription() {
    }
    public int compareTo(Object n) {
        if(n instanceof PluginDescription)
            return name.compareToIgnoreCase(((PluginDescription)n).name);
        return -1;
    }
}
