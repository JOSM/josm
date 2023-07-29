// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Some objects like layers (when they are removed) or the whole map frame (when the last layer has
 * been removed) have a definite set of actions to execute. This is the "destructor" interface called
 * on those objects.
 *
 * @author immanuel.scholz
 * @since   208 (creation)
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface Destroyable {

    /**
     * Called when the object has been destroyed.
     */
    void destroy();
}
