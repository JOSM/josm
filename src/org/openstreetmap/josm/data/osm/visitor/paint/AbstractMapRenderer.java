// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Graphics2D;

import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * <p>Abstract common superclass for {@link Rendering} implementations.</p>
 *
 */
public abstract class AbstractMapRenderer implements Rendering {

    /** the graphics context to which the visitor renders OSM objects */
    protected Graphics2D g;
    /** the map viewport - provides projection and hit detection functionality */
    protected NavigatableComponent nc;
    /** if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     */
    protected boolean isInactiveMode;

    /**
     * <p>Creates an abstract paint visitor</p>
     * 
     * @param g the graphics context. Must not be null.
     * @param nc the map viewport. Must not be null.
     * @param isInactiveMode if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     * @throws IllegalArgumentException thrown if {@code g} is null
     * @throws IllegalArgumentException thrown if {@code nc} is null
     */
    public AbstractMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(g);
        CheckParameterUtil.ensureParameterNotNull(nc);
        this.g = g;
        this.nc = nc;
        this.isInactiveMode = isInactiveMode;
    }
}
