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
    private static final String CODE_PATTERN = "{{{%n%s%n}}}";
    private String text;
    private JosmTextArea textArea;

    /**
     * Creates a new text area.
     * @since 10585
     */
    private DebugTextDisplay() {
        textArea = new JosmTextArea();
        textArea.setCaretPosition(0);
        textArea.setEditable(false);
        setViewportView(textArea);
        setPreferredSize(new Dimension(600, 300));
    }

    /**
     * Creates a new text area with an inital text to display
     * @param textToDisplay The text to display.
     */
    public DebugTextDisplay(String textToDisplay) {
        this();
        setCodeText(textToDisplay);
    }

    /**
     * Creates a new text area that displays the bug report data
     * @param report The bug report data to display.
     * @since 10585
     */
    public DebugTextDisplay(BugReport report) {
        this();
        setCodeText(report.getReportText());
        report.addChangeListener(e -> setCodeText(report.getReportText()));
    }

    /**
     * Sets the text that should be displayed in this view.
     * @param textToDisplay The text
     */
    private void setCodeText(String textToDisplay) {
        text = Utils.strip(textToDisplay).replaceAll("\r", "");
        textArea.setText(String.format(CODE_PATTERN, text));
    }

    /**
     * Copies the debug text to the clippboard. This includes the code tags for trac.
     * @return <code>true</code> if copy was successful
     */
    public boolean copyToClippboard() {
        return Utils.copyToClipboard(String.format(CODE_PATTERN, text));
    }

    /**
     * Gets the text this are displays, without the code tag.
     * @return The stripped text set by {@link #setCodeText(String)}
     * @since 10585
     */
    public String getCodeText() {
        return text;
    }
}
