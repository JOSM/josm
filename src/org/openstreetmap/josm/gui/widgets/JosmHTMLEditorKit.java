// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * A subclass of {@link HTMLEditorKit} that fixes an uncommon design choice that shares the set stylesheet between all instances.
 * This class stores a single stylesheet per instance, as it should have be done by Sun in the first place.
 * @since 6040
 */
public class JosmHTMLEditorKit extends HTMLEditorKit {

    /**
     * Constructs a new {@code JosmHTMLEditorKit}
     */
    public JosmHTMLEditorKit() {
    }
    
    protected StyleSheet ss = super.getStyleSheet();

    /**
     * Set the set of styles to be used to render the various HTML elements.
     * These styles are specified in terms of CSS specifications. 
     * Each document produced by the kit will have a copy of the sheet which
     * it can add the document specific styles to. 
     * 
     * Unlike the base implementation, the StyleSheet specified is NOT shared 
     * by all HTMLEditorKit instances, to provide a finer granularity.

     * @see #getStyleSheet
     */
    @Override
    public void setStyleSheet(StyleSheet s) {
        ss = s;
    }

    /**
     * Get the set of styles currently being used to render the HTML elements.
     *  
     * Unlike the base implementation, the StyleSheet specified is NOT shared 
     * by all HTMLEditorKit instances, to provide a finer granularity.
     * 
     * @see #setStyleSheet
     */
    @Override
    public StyleSheet getStyleSheet() {
        return ss;
    }
}
