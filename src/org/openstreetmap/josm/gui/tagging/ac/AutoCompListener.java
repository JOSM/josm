// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.EventListener;

/**
 * The listener interface for receiving autoComp events.
 * The class that is interested in processing an autoComp event
 * implements this interface, and the object created with that
 * class is registered with a component, using the component's
 * <code>addAutoCompListener</code> method. When the autoComp event
 * occurs, that object's <code>autoCompPerformed</code> method is
 * invoked.
 *
 * @see AutoCompEvent
 * @since 18221
 */
public interface AutoCompListener extends EventListener {

    /**
     * Invoked before an autocomplete.  You can use this to change the model.
     *
     * @param e an {@link AutoCompEvent}
     */
    void autoCompBefore(AutoCompEvent e);

    /**
     * Invoked after an autocomplete happened.
     *
     * @param e an {@link AutoCompEvent}
     */
    void autoCompPerformed(AutoCompEvent e);
}
