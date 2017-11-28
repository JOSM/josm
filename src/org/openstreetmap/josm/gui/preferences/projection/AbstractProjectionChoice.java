// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import java.util.Optional;

import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Super class for ProjectionChoice implementations.
 * <p>
 * Handles common parameters <code>name</code>, <code>id</code> and <code>cacheDir</code>.
 */
public abstract class AbstractProjectionChoice implements ProjectionChoice {

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
        if (!id.startsWith("core:")) throw new IllegalArgumentException(id+" does not start with core:");
        this.cacheDir = id.substring(5);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract String getCurrentCode();

    public abstract String getProjectionName();

    @Override
    public Projection getProjection() {
        String code = getCurrentCode();
        return new CustomProjection(getProjectionName(), code, Optional.ofNullable(Projections.getInit(code))
                .orElseThrow(() -> new AssertionError("Error: Unknown projection code: " + code)));
    }
}
