// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

abstract public class AbstractProjectionChoice implements ProjectionChoice {
    private String id;
    private String name;

    public AbstractProjectionChoice(String id, String name) {
        this.id = id;
        this.name = name;
    }
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return name;
    }

}
