// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

abstract public class AbstractProjectionChoice implements ProjectionChoice {

    protected String name;
    protected String id;
    protected String cacheDir;

    /**
     * Constructs a new {@code AbstractProjectionChoice}.
     *
     * @param name short name of the projection choice as shown in the GUI
     * @param id unique identifier for the projection choice
     * @param cacheDir a cache directory name
     */
    public AbstractProjectionChoice(String name, String id, String cacheDir) {
        this.name = name;
        this.id = id;
        this.cacheDir = cacheDir;
    }

    /**
     * Constructs a new {@code AbstractProjectionChoice}.
     *
     * Only for core projection choices, where chacheDir is the same as
     * the second part of the id.
     * @param name short name of the projection choice as shown in the GUI
     * @param id unique identifier for the projection choice
     */
    public AbstractProjectionChoice(String name, String id) {
        this(name, id, null);
        if (!id.startsWith("core:")) throw new IllegalArgumentException();
        this.cacheDir = id.substring(5);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    @Override
    public String toString() {
        return name;
    }

    abstract public String getCurrentCode();

    abstract public String getProjectionName();

    @Override
    public Projection getProjection() {
        String code = getCurrentCode();
        String pref = Projections.getInit(code);
        if (pref == null)
            throw new AssertionError("Error: Unkown projection code");
        return new CustomProjection(getProjectionName(), code, pref, getCacheDir());
    }
}
