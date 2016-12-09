// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Proj Factory that creates instances from a given class.
 */
public class ClassProjFactory implements ProjFactory {

    private final Class<? extends Proj> projClass;

    /**
     * Constructs a new {@code ClassProjFactory}.
     * @param projClass projection class
     */
    public ClassProjFactory(Class<? extends Proj> projClass) {
        this.projClass = projClass;
    }

    @Override
    public Proj createInstance() {
        Proj proj = null;
        try {
            proj = projClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
        return proj;
    }
}
