// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import java.util.Optional;

import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Super class for ProjectionChoice implementations.
 * <p>
 * Handles common parameters <code>name</code> and <code>id</code>.
 */
public abstract class AbstractProjectionChoice implements ProjectionChoice {

    protected String name;
    protected String id;

    /**
     * Constructs a new {@code AbstractProjectionChoice}.
     *
     * @param name short name of the projection choice as shown in the GUI
     * @param id unique identifier for the projection choice
     * @param cacheDir unused
     * @deprecated use {@link #AbstractProjectionChoice(String, String)} instead
     */
    @Deprecated
    public AbstractProjectionChoice(String name, String id, String cacheDir) {
        this(name, id);
    }

    /**
     * Constructs a new {@code AbstractProjectionChoice}.
     *
     * @param name short name of the projection choice as shown in the GUI
     * @param id unique identifier for the projection choice
     */
    public AbstractProjectionChoice(String name, String id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns current projection code.
     * @return current projection code
     */
    public abstract String getCurrentCode();

    /**
     * Returns projection name.
     * @return projection name
     */
    public abstract String getProjectionName();

    @Override
    public Projection getProjection() {
        String code = getCurrentCode();
        return new CustomProjection(getProjectionName(), code, Optional.ofNullable(Projections.getInit(code))
                .orElseThrow(() -> new AssertionError("Error: Unknown projection code: " + code)));
    }
}
