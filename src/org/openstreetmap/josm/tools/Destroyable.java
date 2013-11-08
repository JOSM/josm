// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Some objects like layers (when they are removed) or the whole map frame (when the last layer has
 * been removed) have an definite set of actions to execute. This is the "destructor" interface called
 * on those objects.
 *
 * @author immanuel.scholz
 */
public interface Destroyable {

    /**
     * Called when the object has been destroyed.
     */
    public void destroy();
}
