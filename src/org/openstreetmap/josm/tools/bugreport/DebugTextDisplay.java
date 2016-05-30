// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.bugreport;

import java.awt.Dimension;

import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a text area that displays the debug text with scroll bars.
 * @author Michael Zangl
 * @since 10055
 */
public class DebugTextDisplay extends JScrollPane {
    private final String text;

    /**
     * Creates a new text are with the fixed text
     * @param textToDisplay The text to display.
     */
    public DebugTextDisplay(String textToDisplay) {
        text = "{{{\n" + Utils.strip(textToDisplay) + "\n}}}";
        JosmTextArea textArea = new JosmTextArea(text);
        textArea.setCaretPosition(0);
        textArea.setEditable(false);
        setViewportView(textArea);
        setPreferredSize(new Dimension(600, 300));
    }

    /**
     * Copies the debug text to the clippboard.
     * @return <code>true</code> if copy was successful
     */
    public boolean copyToClippboard() {
        return Utils.copyToClipboard(text);
    }
}
